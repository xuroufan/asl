package com.futures.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.risk.entity.PositionLimitConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 持仓限额配置 Mapper。
 */
@Mapper
public interface PositionLimitConfigMapper extends BaseMapper<PositionLimitConfigEntity> {
}
