package com.futures.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test @DisplayName("success() 返回code=200")
    void testSuccess() {
        Result<String> r = Result.success("data");
        assertEquals(200, r.getCode());
        assertEquals("data", r.getData());
    }

    @Test @DisplayName("success() 无参返回code=200")
    void testSuccessNoData() {
        Result<Void> r = Result.success();
        assertEquals(200, r.getCode());
    }

    @Test @DisplayName("error() 设置code和msg")
    void testError() {
        Result<Void> r = Result.error(400, "bad request");
        assertEquals(400, r.getCode());
        assertEquals("bad request", r.getMsg());
    }

    @Test @DisplayName("timestamp 自动设置")
    void testTimestamp() {
        Result<String> r = Result.success("x");
        assertTrue(r.getTimestamp() > 0);
    }
}
