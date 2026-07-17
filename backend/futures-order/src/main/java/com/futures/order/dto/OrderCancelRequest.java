package com.futures.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 撤单请求 DTO
 */
@Data
public class OrderCancelRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
