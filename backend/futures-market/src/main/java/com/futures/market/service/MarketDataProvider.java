package com.futures.market.service;

import com.futures.market.entity.KlineEntity;
import com.futures.market.entity.MarketDepth;
import java.util.List;
import java.util.Map;

/**
 * 行情数据提供者接口。
 * 
 * 实现类可以从真实交易所 API (如 iTick) 获取行情，
 * 也可以使用模拟数据生成器。
 */
public interface MarketDataProvider {

    /** 获取所有合约列表 */
    List<Map<String, Object>> getSymbols();

    /** 获取实时报价 */
    Map<String, Object> getQuote(String symbol);

    /** 获取五档盘口 */
    MarketDepth getDepth(String symbol);

    /** 获取K线数据 */
    List<KlineEntity> getKlines(String symbol, String interval, int limit);

    /** 获取最近成交 */
    List<Map<String, Object>> getTrades(String symbol, int limit);

    /** 该提供者是否可用（API连接正常） */
    boolean isAvailable();
}
