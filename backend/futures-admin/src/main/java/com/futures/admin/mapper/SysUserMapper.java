package com.futures.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.admin.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface SysUserMapper extends BaseMapper<SysUser> {
    SysUser selectUserByUsername(@Param("username") String username);
    IPage<SysUser> selectUserList(Page<?> page, @Param("user") SysUser user);
}
