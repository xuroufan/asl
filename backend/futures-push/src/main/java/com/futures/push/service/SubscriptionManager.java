package com.futures.push.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 订阅管理器。
 *
 * <p>管理用户对行情合约和订单推送的订阅关系。每个用户可以：
 * <ul>
 *   <li>订阅多个行情合约（symbols）</li>
 *   <li>订阅自己的订单状态推送</li>
 * </ul>
 *
 * <p>线程安全：使用 {@link ConcurrentHashMap} 和 {@link CopyOnWriteArraySet}。
 */
@Slf4j
@Component
public class SubscriptionManager {

    /** 用户 → 订阅的行情合约列表 */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> marketSubscriptions = new ConcurrentHashMap<>();

    /** 用户 → 是否订阅了订单推送 */
    private final ConcurrentHashMap<String, Boolean> orderSubscriptions = new ConcurrentHashMap<>();

    /** 合约 → 订阅了该合约的用户列表（用于行情消息分发） */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<String>> symbolSubscribers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("SubscriptionManager initialized");
    }

    /**
     * 订阅行情合约。
     */
    public void subscribeMarket(String userId, String symbol) {
        marketSubscriptions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(symbol);
        symbolSubscribers.computeIfAbsent(symbol, k -> new CopyOnWriteArraySet<>()).add(userId);
        log.debug("用户 {} 订阅行情合约 {}", userId, symbol);
    }

    /**
     * 批量订阅行情合约。
     */
    public void subscribeMarkets(String userId, Set<String> symbols) {
        for (String symbol : symbols) {
            subscribeMarket(userId, symbol);
        }
    }

    /**
     * 取消订阅行情合约。
     */
    public void unsubscribeMarket(String userId, String symbol) {
        Set<String> userSymbols = marketSubscriptions.get(userId);
        if (userSymbols != null) {
            userSymbols.remove(symbol);
        }
        Set<String> subscribers = symbolSubscribers.get(symbol);
        if (subscribers != null) {
            subscribers.remove(userId);
        }
        log.debug("用户 {} 取消订阅行情合约 {}", userId, symbol);
    }

    /**
     * 订阅订单状态推送。
     */
    public void subscribeOrder(String userId) {
        orderSubscriptions.put(userId, Boolean.TRUE);
        log.debug("用户 {} 订阅订单推送", userId);
    }

    /**
     * 取消订阅订单推送。
     */
    public void unsubscribeOrder(String userId) {
        orderSubscriptions.put(userId, Boolean.FALSE);
        log.debug("用户 {} 取消订阅订单推送", userId);
    }

    /**
     * 获取用户订阅的行情合约列表。
     */
    public Set<String> getUserMarketSubscriptions(String userId) {
        return marketSubscriptions.getOrDefault(userId, new CopyOnWriteArraySet<>());
    }

    /**
     * 用户是否订阅了订单推送。
     */
    public boolean isOrderSubscribed(String userId) {
        return orderSubscriptions.getOrDefault(userId, Boolean.FALSE);
    }

    /**
     * 获取订阅了指定合约的所有用户。
     */
    public Set<String> getSymbolSubscribers(String symbol) {
        return symbolSubscribers.getOrDefault(symbol, new java.util.concurrent.CopyOnWriteArraySet<>());
    }

    /**
     * 用户断线时清除其所有订阅。
     */
    public void removeUserSubscriptions(String userId) {
        Set<String> symbols = marketSubscriptions.remove(userId);
        if (symbols != null) {
            for (String symbol : symbols) {
                Set<String> subscribers = symbolSubscribers.get(symbol);
                if (subscribers != null) {
                    subscribers.remove(userId);
                }
            }
        }
        orderSubscriptions.remove(userId);
        log.info("已清除用户 {} 的所有订阅", userId);
    }

    /** 当前活跃订阅行情合约的用户数 */
    public int getMarketSubscriberCount() {
        return marketSubscriptions.size();
    }

    /** 当前活跃订阅订单推送的用户数 */
    public int getOrderSubscriberCount() {
        return orderSubscriptions.size();
    }
}
