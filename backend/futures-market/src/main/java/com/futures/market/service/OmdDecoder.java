package com.futures.market.service;

import com.futures.market.entity.KlineEntity;
import com.futures.market.entity.MarketDepth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * HKEX OMD（Orion Market Data）协议解码器。
 *
 * OMD 是港交所的行情数据二进制协议。
 * 此解码器支持 OMD 1.0/2.0 消息格式解析。
 *
 * 主要消息类型：
 * - 0x01: MarketSnapshot (快照行情)
 * - 0x02: OrderBookSnapshot (盘口快照，十档)
 * - 0x03: Trade (逐笔成交)
 * - 0x04: Statistics (统计信息)
 * - 0x05: SecurityStatus (品种状态)
 *
 * 解码器设计为无状态，线程安全。
 */
@Slf4j
@Component
public class OmdDecoder {

    // ─── 消息类型常量 ───
    private static final byte MSG_MARKET_SNAPSHOT = 0x01;
    private static final byte MSG_ORDER_BOOK = 0x02;
    private static final byte MSG_TRADE = 0x03;
    private static final byte MSG_STATISTICS = 0x04;
    private static final byte MSG_SECURITY_STATUS = 0x05;

    // ─── 消息头长度 ───
    private static final int HEADER_LENGTH = 12;

    // ─── 合约映射（OMD 品种代码 → 内部代码）───
    private static final Map<String, String> OMD_SYMBOL_MAP = Map.ofEntries(
            Map.entry("HSI", "HSI"),
            Map.entry("HHI", "HHI"),
            Map.entry("MHI", "MHI"),
            Map.entry("MCH", "MCH"),
            Map.entry("HTI", "HTI")
    );

    /**
     * OMD 消息头结构。
     */
    public record OmdHeader(
            short messageSize,
            byte messageType,
            byte version,
            int sequenceNumber,
            long timestamp
    ) {}

    /**
     * 快照行情数据。
     */
    public record MarketSnapshotData(
            String symbol,
            BigDecimal lastPrice,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            long bidSize,
            long askSize,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal openPrice,
            BigDecimal closePrice,
            long totalVolume,
            BigDecimal turnover
    ) {}

    /**
     * 盘口数据。
     */
    public record OrderBookEntry(
            int position,
            BigDecimal price,
            long quantity,
            int orderCount
    ) {}

    /**
     * 逐笔成交数据。
     */
    public record TradeData(
            String symbol,
            BigDecimal price,
            long volume,
            long tradeId,
            byte tradeType,
            long timestamp
    ) {}

    // ─── 解码方法 ───

    /**
     * 解码 OMD 二进制消息。
     *
     * @param rawBytes OMD 原始二进制数据。
     * @return 解码结果 Map，包含 "type" 和具体数据。
     */
    public Map<String, Object> decode(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length < HEADER_LENGTH) {
            log.warn("OMD 数据长度不足: {}", rawBytes != null ? rawBytes.length : 0);
            return Map.of("type", "ERROR", "error", "数据长度不足");
        }

        try {
            OmdHeader header = parseHeader(rawBytes);
            byte[] payload = Arrays.copyOfRange(rawBytes, HEADER_LENGTH, rawBytes.length);

            return switch (header.messageType) {
                case MSG_MARKET_SNAPSHOT -> decodeMarketSnapshot(payload, header);
                case MSG_ORDER_BOOK -> decodeOrderBook(payload, header);
                case MSG_TRADE -> decodeTrade(payload, header);
                case MSG_STATISTICS -> decodeStatistics(payload, header);
                case MSG_SECURITY_STATUS -> decodeSecurityStatus(payload, header);
                default -> {
                    log.warn("未知 OMD 消息类型: 0x%02x", header.messageType);
                    yield Map.of("type", "UNKNOWN", "messageType", header.messageType);
                }
            };
        } catch (Exception e) {
            log.error("OMD 解码失败: {}", e.getMessage());
            return Map.of("type", "ERROR", "error", e.getMessage());
        }
    }

    /**
     * 批量解码多条 OMD 消息。
     *
     * @param rawBytes 包含多条消息的原始数据。
     * @return 解码结果列表。
     */
    public List<Map<String, Object>> decodeBatch(byte[] rawBytes) {
        List<Map<String, Object>> results = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.BIG_ENDIAN);

        while (buffer.remaining() >= HEADER_LENGTH) {
            int pos = buffer.position();
            try {
                short msgSize = buffer.getShort(pos);
                if (msgSize <= 0 || buffer.remaining() < msgSize) {
                    break;
                }
                byte[] msgBytes = new byte[msgSize];
                buffer.get(msgBytes);
                results.add(decode(msgBytes));
            } catch (Exception e) {
                log.warn("批量解码中断于位置 {}: {}", pos, e.getMessage());
                break;
            }
        }

        return results;
    }

    /**
     * 将 OMD 快照数据转换为 MarketTick 格式。
     *
     * @param decoded 已解码的 OMD 消息。
     * @return MarketTick 兼容的 Map。
     */
    public Map<String, Object> toMarketTick(Map<String, Object> decoded) {
        if (!"SNAPSHOT".equals(decoded.get("type"))) {
            return Map.of();
        }

        @SuppressWarnings("unchecked")
        MarketSnapshotData snap = (MarketSnapshotData) decoded.get("data");
        if (snap == null) return Map.of();

        Map<String, Object> tick = new LinkedHashMap<>();
        tick.put("symbol", snap.symbol());
        tick.put("bid", snap.bidPrice());
        tick.put("ask", snap.askPrice());
        tick.put("last", snap.lastPrice());
        tick.put("high", snap.highPrice());
        tick.put("low", snap.lowPrice());
        tick.put("volume", snap.totalVolume());
        tick.put("open", snap.openPrice());
        tick.put("close", snap.closePrice());
        tick.put("timestamp", Instant.now().toEpochMilli());
        return tick;
    }

    /**
     * 将 OMD 盘口数据转换为 MarketDepth 对象。
     *
     * @param decoded 已解码的 OMD 消息。
     * @return MarketDepth 对象。
     */
    public MarketDepth toMarketDepth(Map<String, Object> decoded) {
        if (!"ORDER_BOOK".equals(decoded.get("type"))) {
            return null;
        }

        @SuppressWarnings("unchecked")
        var entries = (List<Map<String, Object>>) decoded.get("entries");
        if (entries == null) return null;

        MarketDepth depth = new MarketDepth();
        depth.setSymbol((String) decoded.get("symbol"));
        depth.setBids(new ArrayList<>());
        depth.setAsks(new ArrayList<>());

        for (Map<String, Object> entry : entries) {
            int pos = (int) entry.get("position");
            BigDecimal price = (BigDecimal) entry.get("price");
            BigDecimal qty = new BigDecimal((long) entry.get("quantity"));

            MarketDepth.Level level = new MarketDepth.Level();
            level.setPosition(pos);
            level.setPrice(price);
            level.setQuantity(qty);

            if (pos > 0) {
                depth.getAsks().add(level);
            } else {
                depth.getBids().add(0, level);
            }
        }

        depth.setBids(depth.getBids().stream()
                .sorted(Comparator.comparingInt(MarketDepth.Level::getPosition)).toList());
        depth.setAsks(depth.getAsks().stream()
                .sorted(Comparator.comparingInt(MarketDepth.Level::getPosition)).toList());

        return depth;
    }

    // ─── 内部解析方法 ───

    private OmdHeader parseHeader(byte[] rawBytes) {
        ByteBuffer buf = ByteBuffer.wrap(rawBytes).order(ByteOrder.BIG_ENDIAN);
        short msgSize = buf.getShort();
        byte msgType = buf.get();
        byte version = buf.get();
        int seqNo = buf.getInt();
        long ts = buf.getInt() & 0xFFFFFFFFL; // 4字节时间戳

        return new OmdHeader(msgSize, msgType, version, seqNo, ts);
    }

    private Map<String, Object> decodeMarketSnapshot(byte[] payload, OmdHeader header) {
        ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);

        // OMD 快照结构（简化版，实际协议有更多字段）
        String symbol = readSymbol(buf);
        BigDecimal last = readPrice(buf);
        BigDecimal bid = readPrice(buf);
        BigDecimal ask = readPrice(buf);
        long bidSz = buf.getInt() & 0xFFFFFFFFL;
        long askSz = buf.getInt() & 0xFFFFFFFFL;
        BigDecimal high = readPrice(buf);
        BigDecimal low = readPrice(buf);
        BigDecimal open = readPrice(buf);
        BigDecimal close = readPrice(buf);
        long vol = buf.getLong();
        BigDecimal turnover = readPrice(buf);

        MarketSnapshotData snap = new MarketSnapshotData(
                symbol, last, bid, ask, bidSz, askSz,
                high, low, open, close, vol, turnover
        );

        log.debug("OMD 快照: {} last={} bid={} ask={}", symbol, last, bid, ask);

        return Map.of(
                "type", "SNAPSHOT",
                "header", header,
                "data", snap
        );
    }

    private Map<String, Object> decodeOrderBook(byte[] payload, OmdHeader header) {
        ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);

        String symbol = readSymbol(buf);
        int entryCount = buf.get() & 0xFF;
        List<Map<String, Object>> entries = new ArrayList<>();

        for (int i = 0; i < entryCount && buf.remaining() >= 16; i++) {
            int position = buf.get() & 0xFF; // 1-10 为卖盘, 11-20 为买盘
            BigDecimal price = readPrice(buf);
            long quantity = buf.getLong();
            int orderCount = buf.getInt();
            buf.position(buf.position() + 4); // 跳过保留字段

            int side = position <= 10 ? 1 : -1; // 正=卖, 负=买
            int displayPos = position <= 10 ? position : position - 10;

            entries.add(Map.of(
                    "position", side * displayPos,
                    "price", price,
                    "quantity", quantity,
                    "orderCount", orderCount
            ));
        }

        log.debug("OMD 盘口: {} {} 档", symbol, entryCount);

        return Map.of(
                "type", "ORDER_BOOK",
                "header", header,
                "symbol", symbol,
                "entries", entries
        );
    }

    private Map<String, Object> decodeTrade(byte[] payload, OmdHeader header) {
        ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);

        String symbol = readSymbol(buf);
        BigDecimal price = readPrice(buf);
        long volume = buf.getLong();
        long tradeId = buf.getLong();
        byte tradeType = buf.get();

        TradeData trade = new TradeData(symbol, price, volume, tradeId, tradeType, header.timestamp);

        log.debug("OMD 成交: {} {} @ {} id={}", symbol, volume, price, tradeId);

        return Map.of(
                "type", "TRADE",
                "header", header,
                "data", trade
        );
    }

    private Map<String, Object> decodeStatistics(byte[] payload, OmdHeader header) {
        ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);

        String symbol = readSymbol(buf);
        long totalVolume = buf.getLong();
        BigDecimal turnover = readPrice(buf);
        int numTrades = buf.getInt();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("symbol", symbol);
        stats.put("totalVolume", totalVolume);
        stats.put("turnover", turnover);
        stats.put("numTrades", numTrades);

        log.debug("OMD 统计: {} vol={} turnover={}", symbol, totalVolume, turnover);

        return Map.of(
                "type", "STATISTICS",
                "header", header,
                "data", stats
        );
    }

    private Map<String, Object> decodeSecurityStatus(byte[] payload, OmdHeader header) {
        ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);

        String symbol = readSymbol(buf);
        byte status = buf.get();
        String statusStr = switch (status) {
            case 0 -> "CONTINUOUS_TRADING";
            case 1 -> "AUCTION";
            case 2 -> "CLOSED";
            case 3 -> "HALTED";
            case 4 -> "SUSPENDED";
            default -> "UNKNOWN";
        };

        log.info("OMD 状态: {} = {}", symbol, statusStr);

        return Map.of(
                "type", "SECURITY_STATUS",
                "header", header,
                "symbol", symbol,
                "status", statusStr
        );
    }

    // ─── 工具方法 ───

    private String readSymbol(ByteBuffer buf) {
        // OMD 品种代码：8字节 ASCII
        byte[] symBytes = new byte[8];
        buf.get(symBytes);
        String raw = new String(symBytes).trim();
        return OMD_SYMBOL_MAP.getOrDefault(raw, raw);
    }

    private BigDecimal readPrice(ByteBuffer buf) {
        // OMD 价格：8字节，定点数，单位 0.0001
        long raw = buf.getLong();
        return BigDecimal.valueOf(raw, 4).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 模拟 OMD 消息生成（用于无真实数据时测试）。
     *
     * @param symbol 品种代码。
     * @param last   最新价。
     * @return 模拟的 OMD 快照消息。
     */
    public byte[] generateMockSnapshot(String symbol, BigDecimal last) {
        ByteBuffer buf = ByteBuffer.allocate(512).order(ByteOrder.BIG_ENDIAN);

        // 占位：消息头
        buf.putShort((short) (HEADER_LENGTH + 64));  // 消息总长度
        buf.put(MSG_MARKET_SNAPSHOT);                  // 消息类型
        buf.put((byte) 1);                              // 版本号
        buf.putInt(new Random().nextInt(Integer.MAX_VALUE));  // 序号
        buf.putInt((int) (System.currentTimeMillis() / 1000));  // 时间戳

        // 品种代码
        byte[] symBytes = String.format("%-8s", symbol).getBytes();
        buf.put(symBytes);

        // 价格数据
        BigDecimal spread = last.multiply(new BigDecimal("0.0002"));
        BigDecimal bid = last.subtract(spread);
        BigDecimal ask = last.add(spread);

        writePrice(buf, last);  // last
        writePrice(buf, bid);   // bid
        writePrice(buf, ask);   // ask
        buf.putInt(new Random().nextInt(500));    // bidSize
        buf.putInt(new Random().nextInt(500));    // askSize
        writePrice(buf, last.multiply(new BigDecimal("1.005")));  // high
        writePrice(buf, last.multiply(new BigDecimal("0.995")));  // low
        writePrice(buf, last);   // open
        writePrice(buf, last);   // close
        buf.putLong(new Random().nextLong(100000)); // volume
        writePrice(buf, last.multiply(new BigDecimal("1000")));   // turnover

        // 回到开头写入真实消息长度
        int totalLen = buf.position();
        buf.putShort(0, (short) totalLen);  // 更新消息头中的长度字段

        byte[] result = new byte[totalLen];
        System.arraycopy(buf.array(), 0, result, 0, totalLen);
        return result;
    }

    private void writePrice(ByteBuffer buf, BigDecimal price) {
        long raw = price.multiply(new BigDecimal("10000")).longValue();
        buf.putLong(raw);
    }
}
