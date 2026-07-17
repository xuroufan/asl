package com.futures.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.futures.admin.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface SysRoleMapper extends BaseMapper<SysRole> {
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
   void deleteUserRoleByUserId(@Param("userId") Long userId);

    void insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
    void deleteRoleMenuByRoleId(@Param("roleId") Long roleId);
}
