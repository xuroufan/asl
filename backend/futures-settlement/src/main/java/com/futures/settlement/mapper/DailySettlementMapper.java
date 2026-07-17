package com.futures.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.settlement.entity.DailySettlementEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日终结算 Mapper。
 */
@Mapper
public interface DailySettlementMapper extends BaseMapper<DailySettlementEntity> {
}
