package com.futures.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.settlement.entity.ReconciliationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账记录 Mapper。
 */
@Mapper
public interface ReconciliationMapper extends BaseMapper<ReconciliationEntity> {
}
