package com.futures.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金账户实体 — t_fund_account 表
 * <p>
 * 约束：available + frozen = balance 恒成立
 * 乐观锁：version 字段保证并发安全
 */
@Data
@TableName("t_fund_account")
public class FundAccountEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private String userId;

    /** 总权益 = 可用资金 + 冻结保证金 */
    private BigDecimal balance;

    /** 可用资金 */
    private BigDecimal available;

    /** 冻结保证金 */
    private BigDecimal frozen;

    /** 占用保证金 */
    private BigDecimal margin;

    /** 浮动盈亏 */
    private BigDecimal floatProfit;

    /** 币种 */
    private String currency;

    /** 状态 0=正常, 1=冻结 */
    private Integer status;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    /** 快速创建新账户 */
    public static FundAccountEntity create(String userId) {
        FundAccountEntity acc = new FundAccountEntity();
        acc.setUserId(userId);
        acc.setBalance(BigDecimal.ZERO);
        acc.setAvailable(BigDecimal.ZERO);
        acc.setFrozen(BigDecimal.ZERO);
        acc.setMargin(BigDecimal.ZERO);
        acc.setFloatProfit(BigDecimal.ZERO);
        acc.setCurrency("HKD");
        acc.setStatus(0);
        acc.setVersion(0);
        return acc;
    }
}
