package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.MenuAddDto;
import org.smart.erp.system.dto.MenuDetailDto;
import org.smart.erp.system.dto.MenuTreeDto;
import org.smart.erp.system.entity.Menu;
import org.smart.erp.system.vo.MenuListVo;
import org.smart.erp.system.vo.MenuSearchVo;
import org.smart.erp.system.vo.MenuTreeVo;

import java.util.List;

public interface MenuService extends IService<Menu> {
    Page<MenuListVo> pageMenu(MenuDetailDto dto);

    List<MenuTreeVo> getMenuTree(MenuTreeDto dto);

    MenuListVo detailMenu(Long id);

    void addMenu(MenuAddDto dto);

    void updateMenu(Long id ,MenuAddDto dto);

    void removeMenu(Long id);

    List<MenuTreeVo> getCurrentUserMenu();

    List<MenuSearchVo> searchCurrentUserMenu(String keyword);
}
