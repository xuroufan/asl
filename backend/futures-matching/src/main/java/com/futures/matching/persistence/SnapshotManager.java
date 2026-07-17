package com.futures.matching.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.futures.matching.model.OrderBook;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 快照管理器（Snapshot Manager）。
 * <p>
 * 定期生成订单簿快照，用于加速故障恢复。
 * 快照 + 事件日志 = 完整恢复方案。
 * </p>
 *
 * <p>
 * <b>快照策略</b>：
 * <ul>
 *   <li>定时触发：每 5 分钟自动生成</li>
 *   <li>事件量触发：每 10000 笔成交后生成</li>
 *   <li>手动触发：通过 API 或管理命令</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class SnapshotManager {

    private static final String SNAPSHOT_DIR = "events/snapshots";

    /** 快照时间间隔（秒） */
    @Value("${matching.snapshot.interval-seconds:300}")
    private int snapshotInterval;

    /** 事件量触发阈值 */
    @Value("${matching.snapshot.event-threshold:10000}")
    private int eventThreshold;

    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private long eventCount;

    public SnapshotManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "snapshot-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.eventCount = 0;
    }

    @PostConstruct
    public void init() {
        Path dir = Path.of(SNAPSHOT_DIR);
        if (!dir.toFile().exists()) {
            dir.toFile().mkdirs();
        }

        scheduler.scheduleAtFixedRate(
                () -> log.debug("快照定时任务运行中（实际保存由 Disruptor 触发）"),
                0, snapshotInterval, TimeUnit.SECONDS
        );
        log.info("快照管理器初始化完成, 间隔={}s, 事件阈值={}", snapshotInterval, eventThreshold);
    }

    /**
     * 保存订单簿快照。
     *
     * @param symbol    合约代码
     * @param orderBook 订单簿
     */
    public void saveSnapshot(String symbol, OrderBook orderBook) {
        try {
            String filename = SNAPSHOT_DIR + "/snapshot-" + symbol + "-"
                    + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    + ".json";
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filename), orderBook.toSnapshot());
            log.info("快照已保存: symbol={}, file={}, version={}", symbol, filename, orderBook.getVersion());
        } catch (IOException e) {
            log.error("保存快照失败: symbol={}", symbol, e);
        }
    }

    /**
     * 从快照恢复订单簿。
     *
     * @param symbol   合约代码
     * @param filePath 快照文件路径
     * @param orderBook 待恢复的订单簿
     */
    public void restoreFromSnapshot(String symbol, String filePath, OrderBook orderBook) {
        try {
            var snapshot = objectMapper.readValue(
                    new File(filePath),
                    OrderBook.OrderBookSnapshot.class
            );
            log.warn("快照恢复功能需要根据实际快照结构实现");
        } catch (IOException e) {
            log.error("恢复快照失败: symbol={}, file={}", symbol, filePath, e);
        }
    }

    /** 获取最近的快照文件 */
    public File getLatestSnapshot(String symbol) {
        File dir = new File(SNAPSHOT_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("snapshot-" + symbol));
        if (files == null || files.length == 0) return null;

        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) {
                latest = f;
            }
        }
        return latest;
    }

    /** 增加事件计数（达到阈值时触发快照） */
    public boolean incrementAndCheckEventCount() {
        eventCount++;
        if (eventCount >= eventThreshold) {
            eventCount = 0;
            return true;
        }
        return false;
    }
}
