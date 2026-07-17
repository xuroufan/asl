package com.futures.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.risk.entity.ForcedLiquidationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 强平记录 Mapper。
 */
@Mapper
public interface ForcedLiquidationMapper extends BaseMapper<ForcedLiquidationEntity> {
}
