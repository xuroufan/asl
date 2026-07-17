package com.futures.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.futures.admin.aspect.Log;
import com.futures.admin.entity.SysDictData;
import com.futures.admin.entity.SysDictType;
import com.futures.admin.service.SysUserService;
import com.futures.common.result.PageResult;
import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统管理 — 字典管理
 */
@RestController
@RequestMapping("/api/v1/admin/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysUserService userService;

    // ====== 字典类型 ======
    @GetMapping("/type/list")
    @PreAuthorize("hasPermission('system:dict:list')")
    public Result<PageResult<SysDictType>> typeList(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        IPage<SysDictType> pr = userService.getDictTypePage(page, size);
        return Result.success(PageResult.from(pr));
    }

    @GetMapping("/type/{dictId}")
    @PreAuthorize("hasPermission('system:dict:list')")
    public Result<SysDictType> typeById(@PathVariable Long dictId) {
        return Result.success(userService.getDictTypeById(dictId));
    }

    @PostMapping("/type")
    @PreAuthorize("hasPermission('system:dict:add')")
    @Log(title = "字典管理", operType = 1)
    public Result<Void> typeAdd(@RequestBody SysDictType dict) {
        userService.saveDictType(dict);
        return Result.success();
    }

    @PutMapping("/type")
    @PreAuthorize("hasPermission('system:dict:edit')")
    @Log(title = "字典管理", operType = 2)
    public Result<Void> typeEdit(@RequestBody SysDictType dict) {
        userService.saveDictType(dict);
        return Result.success();
    }

    @DeleteMapping("/type/{dictId}")
    @PreAuthorize("hasPermission('system:dict:remove')")
    @Log(title = "字典管理", operType = 3)
    public Result<Void> typeRemove(@PathVariable Long dictId) {
        userService.removeDictTypeById(dictId);
        return Result.success();
    }

    // ====== 字典数据 ======
    @GetMapping("/data/list")
    @PreAuthorize("hasPermission('system:dict:list')")
    public Result<PageResult<SysDictData>> dataList(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam String dictType) {
        IPage<SysDictData> pr = userService.getDictDataPage(page, size, dictType);
        return Result.success(PageResult.from(pr));
    }

    @GetMapping("/data/{dictCode}")
    @PreAuthorize("hasPermission('system:dict:list')")
    public Result<SysDictData> dataById(@PathVariable Long dictCode) {
        return Result.success(userService.getDictDataById(dictCode));
    }

    @PostMapping("/data")
    @PreAuthorize("hasPermission('system:dict:add')")
    @Log(title = "字典管理", operType = 1)
    public Result<Void> dataAdd(@RequestBody SysDictData data) {
        userService.saveDictData(data);
        return Result.success();
    }

    @PutMapping("/data")
    @PreAuthorize("hasPermission('system:dict:edit')")
    @Log(title = "字典管理", operType = 2)
    public Result<Void> dataEdit(@RequestBody SysDictData data) {
        userService.saveDictData(data);
        return Result.success();
    }

    @DeleteMapping("/data/{dictCode}")
    @PreAuthorize("hasPermission('system:dict:remove')")
    @Log(title = "字典管理", operType = 3)
    public Result<Void> dataRemove(@PathVariable Long dictCode) {
        userService.removeDictDataById(dictCode);
        return Result.success();
    }

    /** 根据字典类型获取所有数据（通用，供其他模块调用） */
    @GetMapping("/data/type/{dictType}")
    public Result<java.util.List<SysDictData>> dataByType(@PathVariable String dictType) {
        return Result.success(userService.getDictDataByType(dictType));
    }
}
