package com.futures.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.account.entity.KycRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * KYC 审核记录 Mapper。
 */
@Mapper
public interface KycRecordMapper extends BaseMapper<KycRecordEntity> {
}
