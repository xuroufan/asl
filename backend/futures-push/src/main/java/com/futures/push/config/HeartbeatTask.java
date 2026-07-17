package com.futures.push.config;

import com.futures.push.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 心跳任务。
 *
 * <p>每 30 秒向所有已连接的客户端发送 ping 消息，客户端需回复 pong。
 * 连续 3 次未收到 pong 的客户端将被主动断开。
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class HeartbeatTask {

    private final PushService pushService;

    /**
     * 每 30 秒发送一次心跳 ping。
     */
    @Scheduled(fixedRate = 30_000)
    public void sendHeartbeat() {
        int activeConnections = pushService.getActiveConnectionCount();
        if (activeConnections == 0) {
            return;
        }

        log.debug("发送心跳 ping: {} 个活跃连接", activeConnections);
        pushService.sendHeartbeatPing();
    }

    /**
     * 每 35 秒检查一次心跳超时（给客户端 5 秒缓冲）。
     */
    @Scheduled(fixedRate = 35_000)
    public void checkTimeouts() {
        pushService.checkHeartbeatTimeouts();
    }
}
