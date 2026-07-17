package com.futures.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.fund.entity.WithdrawRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出金记录 Mapper。
 */
@Mapper
public interface WithdrawRecordMapper extends BaseMapper<WithdrawRecordEntity> {
}
