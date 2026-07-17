package com.futures.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.admin.entity.SysDept;
import java.util.List;
public interface SysDeptMapper extends BaseMapper<SysDept> {
    List<SysDept> selectDeptTree();
}
