package com.futures.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.settlement.entity.SettlementOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 结算订单明细 Mapper。
 */
@Mapper
public interface SettlementOrderMapper extends BaseMapper<SettlementOrderEntity> {
}
