package com.futures.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.settlement.entity.ReconciliationDiffEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账差异明细 Mapper。
 */
@Mapper
public interface ReconciliationDiffMapper extends BaseMapper<ReconciliationDiffEntity> {
}
