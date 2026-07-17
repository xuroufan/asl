package com.futures.order.dto;

import com.futures.order.enums.OrderDirection;
import com.futures.order.enums.OrderStatus;
import com.futures.order.enums.OrderType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应 VO
 */
@Data
public class OrderVO {

    private Long id;
    private String orderId;
    private Long userId;
    private String symbol;
    private String direction;         // BUY / SELL
    private String orderType;         // LIMIT / MARKET / STOP
    private BigDecimal price;
    private Integer volume;
    private Integer filledVolume;
    /** 剩余未成交数量 */
    private Integer remainingVolume;
    private BigDecimal avgPrice;
    private BigDecimal stopPrice;
    private BigDecimal takeProfitPrice;
    private Long parentId;
    private String status;            // PENDING / PARTIAL / FILLED / CANCELLED / REJECTED
    private String statusDesc;        // 中文描述
    private String clientOrderId;
    private String timeInForce;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 成交比例（百分比） */
    private String fillRate;

    /** 从 Entity 构建 VO */
    public static OrderVO from(com.futures.order.entity.OrderEntity e) {
        OrderVO vo = new OrderVO();
        vo.id = e.getId();
        vo.orderId = e.getOrderId();
        vo.userId = e.getUserId();
        vo.symbol = e.getSymbol();
        vo.direction = e.getDirection().name();
        vo.orderType = e.getOrderType().name();
        vo.price = e.getPrice();
        vo.volume = e.getVolume();
        vo.filledVolume = e.getFilledVolume();
        vo.remainingVolume = e.getVolume() - e.getFilledVolume();
        vo.avgPrice = e.getAvgPrice();
        vo.stopPrice = e.getStopPrice();
        vo.takeProfitPrice = e.getTakeProfitPrice();
        vo.parentId = e.getParentId();
        String s = e.getStatus().name();
        vo.status = s;
        switch (s) {
            case "PENDING":  vo.statusDesc = "已报"; break;
            case "PARTIAL":  vo.statusDesc = "部成"; break;
            case "FILLED":   vo.statusDesc = "全成"; break;
            case "CANCELLED":vo.statusDesc = "已撤"; break;
            case "REJECTED": vo.statusDesc = "废单"; break;
            default:         vo.statusDesc = s; break;
        }
        vo.clientOrderId = e.getClientOrderId();
        vo.timeInForce = e.getTimeInForce();
        vo.rejectReason = e.getRejectReason();
        vo.createdAt = e.getCreatedAt();
        vo.updatedAt = e.getUpdatedAt();
        if (e.getVolume() != null && e.getVolume() > 0) {
            vo.fillRate = String.format("%.1f%%",
                    e.getFilledVolume() * 100.0 / e.getVolume());
        } else {
            vo.fillRate = "0%";
        }
        return vo;
    }
}
