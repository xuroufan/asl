package com.futures.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_account")
public class AccountEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal cashBalance;       // 现金余额
    private BigDecimal equityWithLoan;    // 总权益
    private BigDecimal initialMargin;     // 占用保证金
    private BigDecimal maintenanceMargin; // 维持保证金
    private BigDecimal availableFunds;    // 可用资金
    private BigDecimal dailyPnl;          // 日内盈亏
    private BigDecimal dailyLossLimit;    // 日内亏损限额
    private BigDecimal totalPnl;          // 累计盈亏
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
}
