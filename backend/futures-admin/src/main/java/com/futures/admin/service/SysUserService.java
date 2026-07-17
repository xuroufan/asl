package com.futures.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.futures.admin.entity.*;
import com.futures.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 管理后台统一 Service
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysConfigMapper configMapper;

    // ==================== 用户 ====================

    public SysUser getUserByUsername(String username) {
        return userMapper.selectUserByUsername(username);
    }

    public SysUser getUserByEmail(String email) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getEmail, email);
        return userMapper.selectOne(w);
    }

    @Transactional
    public void assignDefaultRole(Long userId) {
        // 分配默认角色：普通用户（roleId=2）
        roleMapper.insertUserRole(userId, 2L);
    }

    public IPage<SysUser> getUserPage(int page, int size, SysUser query) {
        return userMapper.selectUserList(new Page<>(page, size), query);
    }

    @Transactional
    public void saveUser(SysUser user) {
        if (user.getUserId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        if (user.getRoleIds() != null) {
            roleMapper.deleteUserRoleByUserId(user.getUserId());
            for (Long roleId : user.getRoleIds()) {
                roleMapper.insertUserRole(user.getUserId(), roleId);
            }
        }
    }

    public void resetPassword(Long userId) {
        SysUser u = new SysUser();
        u.setUserId(userId);
        u.setPassword("$2a$10$IZ7otMBh67E.1SzSGWjPa.xt8.UXrhOQFabmLQtcLTKu3u6Jpcxxm"); // admin123
        userMapper.updateById(u);
    }

    public List<SysRole> getUserRoles(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    // ==================== 角色 ====================

    public IPage<SysRole> getRolePage(int page, int size) {
        return roleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getRoleSort));
    }

    public List<SysRole> getAllRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 0).orderByAsc(SysRole::getRoleSort));
    }

    public SysRole getRoleById(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    @Transactional
    public void saveRole(SysRole role) {
        if (role.getRoleId() == null) roleMapper.insert(role);
        else roleMapper.updateById(role);
        // 更新角色-菜单关联
        if (role.getMenuIds() != null && role.getRoleId() != null) {
            roleMapper.deleteRoleMenuByRoleId(role.getRoleId());
            for (Long menuId : role.getMenuIds()) {
                roleMapper.insertRoleMenu(role.getRoleId(), menuId);
            }
        }
    }

    @Transactional
    public void removeRoleById(Long roleId) {
        roleMapper.deleteUserRoleByUserId(roleId);  // 清理用户-角色关联
        roleMapper.deleteRoleMenuByRoleId(roleId); // 清理角色-菜单关联
        roleMapper.deleteById(roleId);             // 删除角色本身
    }

    // ==================== 菜单 ====================

    public List<SysMenu> getMenuTree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 0).orderByAsc(SysMenu::getOrderNum));
        return buildMenuTree(all, 0L);
    }

    public List<SysMenu> getMenusByUserId(Long userId) {
        if (userId == 1L) return getMenuTree();
        return buildMenuTree(menuMapper.selectMenusByUserId(userId), 0L);
    }

    private List<SysMenu> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> m.getParentId().equals(parentId))
                .peek(m -> m.setChildren(buildMenuTree(menus, m.getMenuId())))
                .toList();
    }

    public void saveMenu(SysMenu menu) {
        if (menu.getMenuId() == null) menuMapper.insert(menu);
        else menuMapper.updateById(menu);
    }

    public SysMenu getMenuById(Long menuId) {
        return menuMapper.selectById(menuId);
    }

    public void removeMenuById(Long menuId) {
        menuMapper.deleteById(menuId);
    }

    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    // ==================== 部门 ====================

    public List<SysDept> getDeptTree() {
        return buildDeptTree(deptMapper.selectDeptTree(), 0L);
    }

    private List<SysDept> buildDeptTree(List<SysDept> depts, Long parentId) {
        return depts.stream()
                .filter(d -> d.getParentId().equals(parentId))
                .peek(d -> d.setChildren(buildDeptTree(depts, d.getDeptId())))
                .toList();
    }

    public SysDept getDeptById(Long deptId) {
        return deptMapper.selectById(deptId);
    }

    public void saveDept(SysDept dept) {
        if (dept.getDeptId() == null) deptMapper.insert(dept);
        else deptMapper.updateById(dept);
    }

    public void removeDeptById(Long deptId) {
        deptMapper.deleteById(deptId);
    }

    // ==================== 字典类型 ====================

    public IPage<SysDictType> getDictTypePage(int page, int size) {
        return dictTypeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysDictType>().orderByAsc(SysDictType::getDictId));
    }

    public SysDictType getDictTypeById(Long dictId) {
        return dictTypeMapper.selectById(dictId);
    }

    public void saveDictType(SysDictType dict) {
        if (dict.getDictId() == null) dictTypeMapper.insert(dict);
        else dictTypeMapper.updateById(dict);
    }

    public void removeDictTypeById(Long dictId) {
        dictTypeMapper.deleteById(dictId);
    }

    // ==================== 字典数据 ====================

    public IPage<SysDictData> getDictDataPage(int page, int size, String dictType) {
        return dictDataMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictType, dictType)
                        .orderByAsc(SysDictData::getDictSort));
    }

    public SysDictData getDictDataById(Long dictCode) {
        return dictDataMapper.selectById(dictCode);
    }

    public void saveDictData(SysDictData dict) {
        if (dict.getDictCode() == null) dictDataMapper.insert(dict);
        else dictDataMapper.updateById(dict);
    }

    public void removeDictDataById(Long dictCode) {
        dictDataMapper.deleteById(dictCode);
    }

    public List<SysDictData> getDictDataByType(String dictType) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 0)
                .orderByAsc(SysDictData::getDictSort));
    }

    // ==================== 操作日志 ====================

    public IPage<SysOperLog> getOperLogPage(int page, int size) {
        return operLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysOperLog>().orderByDesc(SysOperLog::getOperTime));
    }

    public void saveOperLog(SysOperLog log) {
        operLogMapper.insert(log);
    }

    public void removeOperLogById(Long operId) {
        operLogMapper.deleteById(operId);
    }

    public void cleanOperLog() {
        operLogMapper.delete(new QueryWrapper<>());
    }

    // ==================== 登录日志 ====================

    public IPage<SysLoginLog> getLoginLogPage(int page, int size, String username) {
        LambdaQueryWrapper<SysLoginLog> w = new LambdaQueryWrapper<SysLoginLog>()
                .orderByDesc(SysLoginLog::getLoginTime);
        if (username != null && !username.isEmpty()) {
            w.eq(SysLoginLog::getUsername, username);
        }
        return loginLogMapper.selectPage(new Page<>(page, size), w);
    }

    public void saveLoginLog(SysLoginLog log) {
        loginLogMapper.insert(log);
    }

    public void removeLoginLogById(Long infoId) {
        loginLogMapper.deleteById(infoId);
    }

    public void cleanLoginLog() {
        loginLogMapper.delete(new QueryWrapper<>());
    }

    // ==================== 系统配置 ====================

    public IPage<SysConfig> getConfigPage(int page, int size) {
        return configMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getConfigId));
    }

    public SysConfig getConfigById(Long configId) {
        return configMapper.selectById(configId);
    }

    public String getConfigValueByKey(String configKey) {
        SysConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configKey));
        return config != null ? config.getConfigValue() : null;
    }

    public void saveConfig(SysConfig config) {
        if (config.getConfigId() == null) configMapper.insert(config);
        else configMapper.updateById(config);
    }

    public void removeConfigById(Long configId) {
        configMapper.deleteById(configId);
    }

    // ==================== 仪表盘 ====================

    public Map<String, Object> getDashboardStats() {
        long userCount = userMapper.selectCount(null);
        long todayOperCount = operLogMapper.selectCount(
                new LambdaQueryWrapper<SysOperLog>()
                        .ge(SysOperLog::getOperTime, java.time.LocalDateTime.now().minusDays(1)));
        long todayLoginCount = loginLogMapper.selectCount(
                new LambdaQueryWrapper<SysLoginLog>()
                        .ge(SysLoginLog::getLoginTime, java.time.LocalDateTime.now().minusDays(1)));
        return Map.of(
                "userCount", userCount,
                "todayOperCount", todayOperCount,
                "todayLoginCount", todayLoginCount,
                "serverTime", java.time.LocalDateTime.now().toString()
        );
    }
}
