package com.futures.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.risk.entity.RiskConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控配置 Mapper
 */
@Mapper
public interface RiskConfigMapper extends BaseMapper<RiskConfigEntity> {
}
