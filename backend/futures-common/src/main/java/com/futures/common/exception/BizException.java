package com.futures.common.exception;

import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // ============ 预定义业务异常工厂 ============

    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }

    public static BizException insufficientFunds(String msg) {
        return new BizException(4001, msg);
    }

    public static BizException marginError(String msg) {
        return new BizException(4002, msg);
    }

    public static BizException positionError(String msg) {
        return new BizException(4003, msg);
    }

    public static BizException contractNotFound(String symbol) {
        return new BizException(4004, "合约不存在: " + symbol);
    }

    public static BizException unauthorized(String msg) {
        return new BizException(401, msg);
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }

   public static BizException dailyLossLimitBreached() {
        return new BizException(4005, "日内亏损限额已触发，禁止开仓");
    }
}
