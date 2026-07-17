package com.futures.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 *
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 数据列表 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页码 */
    private int page;
    /** 每页大小 */
    private int size;
    /** 总页数 */
    private long pages;

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = total;
        r.page = page;
        r.size = size;
        r.pages = (total + size - 1) / size;
        return r;
    }

    /**
     * 从 MyBatis-Plus IPage 转换。
     *
     * @param page MyBatis-Plus 分页对象
     * @param <T>  数据类型
     * @return 分页响应结果
     */
    public static <T> PageResult<T> from(IPage<T> page) {
        return of(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }
}
