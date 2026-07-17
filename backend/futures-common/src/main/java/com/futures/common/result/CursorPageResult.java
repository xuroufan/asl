package com.futures.common.result;

import lombok.Data;

import java.util.List;

/**
 * 游标分页响应 (替代传统 offset 分页，解决深分页性能问题)
 *
 * @param <T> 数据类型
 */
@Data
public class CursorPageResult<T> {
    private List<T> records;
    private Long lastId;       // 最后一条记录的ID(用于下一页查询)
    private Integer size;      // 当前页大小
    private Boolean hasMore;   // 是否还有更多数据

    public static <T> CursorPageResult<T> of(List<T> records, Long lastId, Integer size, Boolean hasMore) {
        CursorPageResult<T> r = new CursorPageResult<>();
        r.records = records;
        r.lastId = lastId;
        r.size = size;
        r.hasMore = hasMore;
        return r;
    }
}
