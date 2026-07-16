package com.hackfuture.core.network.websocket

enum class WebSocketConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    CLOSED,
}

enum class WebSocketCloseReason(val code: Int, val description: String) {
    NORMAL(1000, "Normal closure"),
    GOING_AWAY(1001, "Going away"),
    AUTH_EXPIRED(4001, "Auth token expired"),
    SERVER_ERROR(4002, "Server error"),
    HEARTBEAT_TIMEOUT(4003, "Heartbeat timeout"),
    MANUAL_CLOSE(4004, "Manually closed"),
}

data class WebSocketConfig(
    val url: String = "wss://ws.hackfuture.com/ws",
    val heartbeatIntervalMs: Long = 15_000L,
    val heartbeatTimeoutMs: Long = 30_000L,
    val reconnectBaseDelayMs: Long = 1_000L,
    val reconnectMaxDelayMs: Long = 30_000L,
    val maxReconnectAttempts: Int = 10,
    val pingMessage: String = "{\"type\":\"ping\"}",
)

data class WebSocketEvent(
    val type: EventType,
    val data: String? = null,
    val error: Throwable? = null,
) {
    enum class EventType {
        CONNECTED,
        DISCONNECTED,
        MESSAGE,
        ERROR,
        RECONNECTING,
        RECONNECTED,
    }
}
