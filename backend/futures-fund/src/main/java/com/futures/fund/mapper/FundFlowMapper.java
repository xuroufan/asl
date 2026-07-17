package com.futures.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.fund.entity.FundFlowEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金流水 Mapper
 */
@Mapper
public interface FundFlowMapper extends BaseMapper<FundFlowEntity> {
}
