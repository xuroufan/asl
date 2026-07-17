package com.futures.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.admin.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
