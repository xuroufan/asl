package com.futures.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓限额配置实体。
 * <p>按合约配置单个用户的持仓上限、单笔限额等风控参数。</p>
 */
@Data
@TableName("position_limit_config")
public class PositionLimitConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（null 表示全局默认配置） */
    private Long userId;

    /** 合约代码（null 表示全局默认配置） */
    private String symbol;

    /** 单合约最大持仓手数 */
    private Integer maxPositionVolume;

    /** 单笔最大下单手数 */
    private Integer maxOrderVolume;

    /** 是否启用（1启用/0禁用） */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
