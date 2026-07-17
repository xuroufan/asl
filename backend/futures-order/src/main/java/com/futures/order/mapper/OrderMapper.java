package com.futures.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    // ==================== XML 查询方法（见 OrderMapper.xml） ====================

    /** 根据 orderId 查询订单 */
    OrderEntity selectByOrderId(@Param("orderId") String orderId);

    /** 查询用户活跃订单（PENDING / PARTIAL），可选按合约筛选 */
    List<OrderEntity> selectActiveOrders(@Param("userId") Long userId,
                                         @Param("symbol") String symbol);

    /** 分页查询历史委托 */
    List<OrderEntity> selectHistoryOrders(@Param("userId") Long userId,
                                          @Param("symbol") String symbol,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /** 查询父单下的所有子单 */
    List<OrderEntity> selectChildrenByParentId(@Param("parentId") Long parentId);

    // ==================== 注解 SQL 方法 ====================

    /**

    /**
     * 更新订单的 parentId（括号单关联）
     *
     * @param id       子单ID
     * @param parentId 父单ID
     */
    @Update("UPDATE t_order SET parent_id = #{parentId} WHERE id = #{id}")
    void updateParentIdById(@Param("id") Long id, @Param("parentId") Long parentId);
/**
     * 游标分页查询 (Seek Method) — 替代深分页 OFFSET
     *
     * @param queryWrapper 查询条件
     * @param limit        每页条数
     * @param lastId       上一页最后一条ID (首次传 null)
     * @return 订单列表
     */
    List<OrderEntity> selectPageCursor(
            @Param("ew") com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderEntity> queryWrapper,
            @Param("limit") int limit,
            @Param("lastId") Long lastId);
}
