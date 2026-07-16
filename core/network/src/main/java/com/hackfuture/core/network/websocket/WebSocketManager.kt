package com.hackfuture.core.network.websocket

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.hackfuture.core.network.security.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    @ApplicationContext private val app: Application,
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    config: WebSocketConfig = WebSocketConfig(),
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var lifecycleJob: Job? = null

    private val activeSubscriptions = ConcurrentHashMap.newKeySet<String>()
    private val messageBuffer = ArrayDeque<String>(MAX_BUFFER_SIZE)

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.IDLE)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1, extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    var config: WebSocketConfig = config
        private set

    private var isAppInForeground = true

    init {
        observeLifecycle()
    }

    // ==================== 连接管理 ====================

    fun connect() {
        if (_connectionState.value == WebSocketConnectionState.CONNECTING ||
            _connectionState.value == WebSocketConnectionState.CONNECTED
        ) return

        _connectionState.value = WebSocketConnectionState.CONNECTING
        reconnectAttempt = 0
        doConnect()
    }

    fun disconnect(reason: WebSocketCloseReason = WebSocketCloseReason.MANUAL_CLOSE) {
        Timber.d("WebSocket disconnect: ${reason.description}")
        stopHeartbeat()
        cancelReconnect()
        webSocket?.close(reason.code, reason.description)
        webSocket = null
        _connectionState.value = WebSocketConnectionState.CLOSED
        _events.tryEmit(WebSocketEvent(WebSocketEvent.EventType.DISCONNECTED, reason.description))
    }

    fun close() {
        disconnect()
        activeSubscriptions.clear()
        messageBuffer.clear()
        lifecycleJob?.cancel()
    }

    // ==================== 订阅管理 ====================

    fun subscribe(channel: String) {
        activeSubscriptions.add(channel)
        sendMessage("{\"type\":\"subscribe\",\"channel\":\"$channel\"}")
    }

    fun unsubscribe(channel: String) {
        activeSubscriptions.remove(channel)
        sendMessage("{\"type\":\"unsubscribe\",\"channel\":\"$channel\"}")
    }

    fun sendMessage(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    // ==================== 内部连接 ====================

    private fun doConnect() {
        val token = tokenManager.getAccessToken()
        if (token == null) {
            Timber.e("Cannot connect WebSocket: no token")
            _connectionState.value = WebSocketConnectionState.CLOSED
            return
        }
        val request = Request.Builder()
            .url(config.url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Device-Id", tokenManager.getDeviceId())
            .build()

        val wsClient = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS) // WebSocket 需要无限超时
            .build()

        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Timber.i("WebSocket connected to ${config.url}")
                reconnectAttempt = 0
                _connectionState.value = WebSocketConnectionState.CONNECTED
                _events.tryEmit(WebSocketEvent(WebSocketEvent.EventType.CONNECTED))
                resubscribeAll()
                flushMessageBuffer()
                startHeartbeat()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                _events.tryEmit(WebSocketEvent(WebSocketEvent.EventType.MESSAGE, text))
                if (text.startsWith("{\"type\":\"pong\"}")) {
                    onPongReceived()
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code $reason")
                ws.close(code, reason)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closed: $code $reason")
                stopHeartbeat()
                webSocket = null
                _connectionState.value = WebSocketConnectionState.CLOSED
                _events.tryEmit(WebSocketEvent(WebSocketEvent.EventType.DISCONNECTED, "$code: $reason"))
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket failure")
                webSocket = null
                _events.tryEmit(WebSocketEvent(WebSocketEvent.EventType.ERROR, error = t))
                scheduleReconnect()
            }
        })
    }

    // ==================== 心跳保活 ====================

    private var lastPongTime = 0L

    private fun startHeartbeat() {
        stopHeartbeat()
        lastPongTime = System.currentTimeMillis()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(config.heartbeatIntervalMs)
                if (!isAppInForeground) {
                    Timber.d("App in background, skipping heartbeat")
                    continue
                }
                if (_connectionState.value != WebSocketConnectionState.CONNECTED) break
                sendPing()
                if (System.currentTimeMillis() - lastPongTime > config.heartbeatTimeoutMs) {
                    Timber.w("Heartbeat timeout, closing connection")
                    webSocket?.close(
                        WebSocketCloseReason.HEARTBEAT_TIMEOUT.code,
                        WebSocketCloseReason.HEARTBEAT_TIMEOUT.description,
                    )
                    onPongReceived() // reset
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun sendPing() {
        webSocket?.send(config.pingMessage)
    }

    private fun onPongReceived() {
        lastPongTime = System.currentTimeMillis()
    }

    // ==================== 指数退避重连 ====================

    private fun scheduleReconnect() {
        if (reconnectAttempt >= config.maxReconnectAttempts) {
            Timber.w("Max reconnect attempts reached ($reconnectAttempt)")
            _connectionState.value = WebSocketConnectionState.CLOSED
            return
        }
        if (!isAppInForeground) {
            Timber.d("App in background, deferring reconnect")
            return
        }
        reconnectAttempt++
        val delayMs = calculateBackoff(reconnectAttempt)
        _connectionState.value = WebSocketConnectionState.RECONNECTING
        _events.tryEmit(WebSocketEvent(WebSocketEvent.EventType.RECONNECTING))
        Timber.i("Reconnecting in ${delayMs}ms (attempt $reconnectAttempt)")
        reconnectJob = scope.launch {
            delay(delayMs)
            if (isActive) {
                _connectionState.value = WebSocketConnectionState.CONNECTING
                doConnect()
            }
        }
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun calculateBackoff(attempt: Int): Long {
        val exponential = config.reconnectBaseDelayMs * (1L shl (attempt - 1).coerceAtMost(30))
        val jitter = (Math.random() * 0.3 * exponential).toLong()
        return (exponential + jitter).coerceAtMost(config.reconnectMaxDelayMs)
    }

    // ==================== 订阅恢复与消息缓存 ====================

    private fun resubscribeAll() {
        Timber.d("Resubscribing to ${activeSubscriptions.size} channels")
        for (channel in activeSubscriptions) {
            sendMessage("{\"type\":\"subscribe\",\"channel\":\"$channel\"}")
        }
    }

    private fun flushMessageBuffer() {
        while (messageBuffer.isNotEmpty()) {
            val msg = messageBuffer.removeFirst()
            if (!sendMessage(msg)) {
                messageBuffer.addFirst(msg)
                break
            }
        }
    }

    fun sendWithBuffer(message: String) {
        if (webSocket?.send(message) != true) {
            if (messageBuffer.size < MAX_BUFFER_SIZE) {
                messageBuffer.addLast(message)
            } else {
                Timber.w("Message buffer full, dropping message")
            }
        }
    }

    // ==================== 生命周期感知 ====================

    private fun observeLifecycle() {
        lifecycleJob = scope.launch(Dispatchers.Main) {
            try {
                val lifecycle = ProcessLifecycleOwner.get().lifecycle
                lifecycle.addObserver(LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            isAppInForeground = true
                            Timber.d("App resumed, reconnecting WebSocket")
                            if (_connectionState.value == WebSocketConnectionState.CLOSED ||
                                _connectionState.value == WebSocketConnectionState.IDLE
                            ) {
                                connect()
                            }
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            isAppInForeground = false
                            Timber.d("App paused, suspending non-critical operations")
                            stopHeartbeat()
                        }
                        else -> {}
                    }
                })
            } catch (e: Exception) {
                Timber.w(e, "ProcessLifecycleOwner not available")
            }
        }
    }

    // ==================== 配置更新 ====================

    fun updateConfig(newConfig: WebSocketConfig) {
        config = newConfig
    }

    companion object {
        private const val MAX_BUFFER_SIZE = 256
    }
}
