package com.futures.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.risk.entity.RiskAlertEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控预警 Mapper
 */
@Mapper
public interface RiskAlertMapper extends BaseMapper<RiskAlertEntity> {
}
