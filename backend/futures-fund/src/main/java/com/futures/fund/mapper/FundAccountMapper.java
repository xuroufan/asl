package com.futures.fund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.fund.entity.FundAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 资金账户 Mapper
 */
@Mapper
public interface FundAccountMapper extends BaseMapper<FundAccountEntity> {

    /**
     * 乐观锁冻结：可用资金减少，冻结增加
     */
    @Update("UPDATE t_fund_account SET available = available - #{amount}, " +
            "frozen = frozen + #{amount}, version = version + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND version = #{version} AND available >= #{amount}")
    int freezeByVersion(@Param("userId") String userId,
                        @Param("amount") java.math.BigDecimal amount,
                        @Param("version") Integer version);

    /**
     * 乐观锁解冻：冻结减少，可用增加
     */
    @Update("UPDATE t_fund_account SET available = available + #{amount}, " +
            "frozen = frozen - #{amount}, version = version + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND version = #{version} AND frozen >= #{amount}")
    int unfreezeByVersion(@Param("userId") String userId,
                          @Param("amount") java.math.BigDecimal amount,
                          @Param("version") Integer version);

    /**
     * 乐观锁扣减冻结资金（成交时使用）
     */
    @Update("UPDATE t_fund_account SET frozen = frozen - #{amount}, " +
            "balance = balance - #{amount}, margin = margin + #{amount}, " +
            "version = version + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND version = #{version} AND frozen >= #{amount}")
    int deductByVersion(@Param("userId") String userId,
                        @Param("amount") java.math.BigDecimal amount,
                        @Param("version") Integer version);

    /**
     * 乐观锁增加资金（入金/平仓盈利）
     */
    @Update("UPDATE t_fund_account SET balance = balance + #{amount}, " +
            "available = available + #{amount}, version = version + 1, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND version = #{version}")
    int depositByVersion(@Param("userId") String userId,
                         @Param("amount") java.math.BigDecimal amount,
                         @Param("version") Integer version);
}
