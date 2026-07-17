package com.futures.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果包装类
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 状态码：200=成功，其他=失败 */
    private int code;
    /** 响应消息 */
    private String msg;
    /** 响应数据 */
    private T data;
    /** 时间戳 */
    private long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }

    public static <T> Result<T> unauthorized(String msg) {
        return error(401, msg);
    }

    public static <T> Result<T> forbidden(String msg) {
        return error(403, msg);
    }

    public static <T> Result<T> badRequest(String msg) {
        return error(400, msg);
    }
}
