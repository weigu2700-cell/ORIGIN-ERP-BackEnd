package org.smart.erp.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/list")
    public Result<Page<MenuListVO>> get(MenuGetDTO dto) {
        return Result.success(menuService.getMenuPage(dto));
    }

    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> getTree(MenuGetTreeDTO dto) {
        return Result.success(menuService.getMenuTree(dto));
    }

    @GetMapping("/{id}")
    public Result<MenuListVO> getById(@PathVariable Long id) {
        return Result.success(menuService.getMenuDetail(id));
    }

    @PostMapping("/create")
    public Result<Void> create(@RequestBody MenuCreateDTO dto) {
        menuService.createMenu(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update( Long id, @RequestBody MenuCreateDTO dto) {
        menuService.updateMenu(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }

}
