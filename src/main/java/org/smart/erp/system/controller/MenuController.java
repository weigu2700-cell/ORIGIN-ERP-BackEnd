package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.MenuAddDto;
import org.smart.erp.system.dto.MenuDetailDto;
import org.smart.erp.system.dto.MenuTreeDto;
import org.smart.erp.system.service.MenuService;
import org.smart.erp.system.vo.MenuListVo;
import org.smart.erp.system.vo.MenuSearchVo;
import org.smart.erp.system.vo.MenuTreeVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
@Tag(name = "菜单管理", description = "菜单的增删改查、列表与树形结构")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "菜单分页列表")
    @GetMapping("/list")
    public Result<Page<MenuListVo>> pageMenu(MenuDetailDto dto) {
        return Result.success(menuService.pageMenu(dto));
    }

    @Operation(summary = "菜单树形结构")
    @GetMapping("/tree")
    public Result<List<MenuTreeVo>> getTree(MenuTreeDto dto) {
        return Result.success(menuService.getMenuTree(dto));
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/{id:\\d+}")
    public Result<MenuListVo> detailMenu(@PathVariable Long id) {
        return Result.success(menuService.detailMenu(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public Result<Void> add(@RequestBody MenuAddDto dto) {
        menuService.addMenu(dto);
        return Result.success();
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update( Long id, @RequestBody MenuAddDto dto) {
        menuService.updateMenu(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> remove(@PathVariable Long id) {
        menuService.removeMenu(id);
        return Result.success();
    }

    @Operation(summary = "获取当前用户的菜单树")
    @GetMapping("/current")
    public Result<List<MenuTreeVo>> getCurrentUserMenu() {
        return Result.success(menuService.getCurrentUserMenu());
    }

    @Operation(summary = "模糊搜索当前用户可访问的菜单")
    @GetMapping("/search")
    public Result<List<MenuSearchVo>> searchCurrentUserMenu(@RequestParam(required = false) String keyword) {
        return Result.success(menuService.searchCurrentUserMenu(keyword));
    }

}
