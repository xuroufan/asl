package com.futures.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.futures.order.enums.OrderDirection;
import com.futures.order.enums.OrderStatus;
import com.futures.order.enums.OrderType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体 — 完全对齐 t_order 表 DDL
 */
@Data
@TableName("t_order")
public class OrderEntity {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单号（全局唯一） */
    private String orderId;

    /** 用户ID */
    private Long userId;

    /** 合约代码，如 ES、GC */
    private String symbol;

    /** 方向 0=BUY 1=SELL */
    private OrderDirection direction;

    /** 订单类型 0=LIMIT 1=MARKET 2=STOP */
    private OrderType orderType;

    /** 价格 */
    private BigDecimal price;

    /** 委托数量 */
    private Integer volume;

    /** 已成交数量 */
    private Integer filledVolume;

    /** 成交均价 */
    private BigDecimal avgPrice;

    /** 止损触发价（STOP单使用） */
    private BigDecimal stopPrice;

    /** 止盈价（BRACKET单使用） */
    private BigDecimal takeProfitPrice;

    /** 父订单ID（括号单关联） */
    private Long parentId;

    /** 状态 0=PENDING 1=PARTIAL 2=FILLED 3=CANCELLED 4=REJECTED */
    private OrderStatus status;

    /** 客户端订单号（幂等用） */
    private String clientOrderId;

    /** 有效期 DAY/IOC/GTC */
    private String timeInForce;

    /** 拒绝原因 */
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
