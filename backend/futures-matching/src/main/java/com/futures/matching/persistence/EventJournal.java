package com.futures.matching.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.futures.matching.model.MatchResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 事件日志（Event Journal）。
 * <p>
 * 每次撮合操作以追加方式写入磁盘文件。
 * 使用 {@link FileChannel} 实现高性能顺序写入。
 * </p>
 */
@Slf4j
@Component
public class EventJournal {

    private static final String JOURNAL_DIR = "events";

    private final ObjectMapper objectMapper;
    private FileChannel writeChannel;
    private final ReentrantLock writeLock = new ReentrantLock();

    public EventJournal() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() throws IOException {
        Path dir = Path.of(JOURNAL_DIR);
        if (!dir.toFile().exists()) {
            dir.toFile().mkdirs();
        }

        String filename = JOURNAL_DIR + "/journal-" + java.time.LocalDate.now() + ".dat";
        this.writeChannel = FileChannel.open(
                Path.of(filename),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );
        log.info("事件日志初始化: {}", filename);
    }

    /**
     * 追加成交记录列表到事件日志。
     *
     * @param results 撮合结果列表
     */
    public void append(List<MatchResult> results) {
        if (results == null || results.isEmpty()) return;
        writeLock.lock();
        try {
            for (MatchResult result : results) {
                byte[] data = objectMapper.writeValueAsBytes(result);
                ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
                buffer.putInt(data.length);
                buffer.put(data);
                buffer.flip();
                writeChannel.write(buffer);
            }
            writeChannel.force(false);
        } catch (IOException e) {
            log.error("写入事件日志失败", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 从指定文件恢复事件日志。
     *
     * @param filePath 日志文件路径
     * @return 撮合结果列表
     */
    public List<MatchResult> recover(String filePath) throws IOException {
        List<MatchResult> results = new ArrayList<>();
        try (FileChannel channel = FileChannel.open(
                Path.of(filePath), StandardOpenOption.READ)) {

            ByteBuffer lengthBuf = ByteBuffer.allocate(4);
            while (channel.read(lengthBuf) > 0) {
                lengthBuf.flip();
                int dataLength = lengthBuf.getInt();
                lengthBuf.clear();

                ByteBuffer dataBuf = ByteBuffer.allocate(dataLength);
                channel.read(dataBuf);
                dataBuf.flip();

                byte[] data = new byte[dataLength];
                dataBuf.get(data);
                MatchResult result = objectMapper.readValue(data, MatchResult.class);
                results.add(result);
            }
        }
        return results;
    }

    /** 获取当前日志文件大小 */
    public long journalSize() throws IOException {
        return writeChannel.size();
    }

    @PreDestroy
    public void close() throws IOException {
        if (writeChannel != null) {
            writeChannel.close();
        }
        log.info("事件日志已关闭");
    }
}
