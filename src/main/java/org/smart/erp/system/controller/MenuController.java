package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.common.result.Result;
import org.smart.erp.system.dto.MenuCreateDTO;
import org.smart.erp.system.dto.MenuGetDTO;
import org.smart.erp.system.dto.MenuGetTreeDTO;
import org.smart.erp.system.service.MenuService;
import org.smart.erp.system.vo.MenuListVO;
import org.smart.erp.system.vo.MenuTreeVO;
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
    public Result<Page<MenuListVO>> listMenu(MenuGetDTO dto) {
        return Result.success(menuService.listMenu(dto));
    }

    @Operation(summary = "菜单树形结构")
    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> getTree(MenuGetTreeDTO dto) {
        return Result.success(menuService.getMenuTree(dto));
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/{id:\\d+}")
    public Result<MenuListVO> getMenuDetail(@PathVariable Long id) {
        return Result.success(menuService.getMenuDetail(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public Result<Void> create(@RequestBody MenuCreateDTO dto) {
        menuService.createMenu(dto);
        return Result.success();
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update( Long id, @RequestBody MenuCreateDTO dto) {
        menuService.updateMenu(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }

    @Operation(summary = "获取当前用户的菜单树")
    @GetMapping("/current")
    public Result<List<MenuTreeVO>> getCurrentUserMenu() {
        return Result.success(menuService.getCurrentUserMenu());
    }

}
