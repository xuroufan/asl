package com.futures.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 下单请求 DTO
 */
@Data
public class OrderPlaceRequest {

    private Long userId;

    @NotBlank(message = "合约代码不能为空")
    @Size(max = 20)
    private String symbol;

    @NotBlank(message = "方向不能为空")
    private String direction;          // BUY / SELL

    @NotBlank(message = "订单类型不能为空")
    private String orderType;          // LIMIT / MARKET / STOP

    /** 价格（限价单必填，市价单忽略） */
    private BigDecimal price;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须为正整数")
    @Max(value = 9999, message = "单笔数量不能超过9999手")
    private Integer volume;

    /** 止损触发价（STOP单必填） */
    private BigDecimal stopPrice;

    /** 客户端订单号（幂等） */
    private String clientOrderId;

    /** 止盈价（BRACKET单使用） */
    private BigDecimal takeProfitPrice;
}
