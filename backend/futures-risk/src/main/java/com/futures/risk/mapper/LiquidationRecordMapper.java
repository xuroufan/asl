package com.futures.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.risk.entity.LiquidationRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 强平记录 Mapper
 */
@Mapper
public interface LiquidationRecordMapper extends BaseMapper<LiquidationRecordEntity> {
}
