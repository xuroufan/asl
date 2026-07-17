package com.futures.market.controller;

import com.futures.common.exception.BizException;
import com.futures.common.result.Result;
import com.futures.market.entity.KlineEntity;
import com.futures.market.entity.MarketDepth;
import com.futures.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    /** 合约列表 */
    @GetMapping("/symbols")
    public Result<List<Map<String, Object>>> getSymbols() {
        return Result.success(marketService.getSymbols());
    }

    /** 实时报价 */
    @GetMapping("/quote")
    public Result<Map<String, Object>> getQuote(@RequestParam String symbol) {
        Map<String, Object> quote = marketService.getQuote(symbol);
        if (quote == null) throw BizException.contractNotFound(symbol);
        return Result.success(quote);
    }

    /** 所有合约报价（用于前端行情看板） */
    @GetMapping("/all-quotes")
    public Result<Map<String, Map<String, Object>>> getAllQuotes() {
        return Result.success(marketService.getAllQuotes());
    }

    /** 五档盘口 */
    @GetMapping("/depth")
    public Result<MarketDepth> getDepth(@RequestParam String symbol) {
        MarketDepth depth = marketService.getDepth(symbol);
        if (depth == null) throw BizException.contractNotFound(symbol);
        return Result.success(depth);
    }

    /** K线数据 */
    @GetMapping("/kline")
    public Result<List<KlineEntity>> getKlines(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(marketService.getKlines(symbol, interval, limit));
    }

    /** 最近成交记录 */
    @GetMapping("/trades")
    public Result<List<Map<String, Object>>> getTrades(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(marketService.getTrades(symbol, limit));
    }

    /** OMD 协议格式快照（港交所 OMD 兼容） */
    @GetMapping("/omd/snapshot")
    public Result<Map<String, Object>> getOmdSnapshot(@RequestParam String symbol) {
        Map<String, Object> snap = marketService.getOmdSnapshot(symbol);
        if (snap.isEmpty() || "ERROR".equals(snap.get("type")))
            throw BizException.contractNotFound(symbol);
        return Result.success(snap);
    }

    /** 所有合约的 OMD 快照 */
    @GetMapping("/omd/all")
    public Result<Map<String, Map<String, Object>>> getAllOmdSnapshots() {
        return Result.success(marketService.getAllOmdSnapshots());
    }
}
