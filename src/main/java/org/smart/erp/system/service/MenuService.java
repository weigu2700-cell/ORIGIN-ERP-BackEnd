package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.dto.MenuCreateDTO;
import org.smart.erp.system.dto.MenuGetDTO;
import org.smart.erp.system.dto.MenuGetTreeDTO;
import org.smart.erp.system.entity.Menu;
import org.smart.erp.system.vo.MenuListVO;
import org.smart.erp.system.vo.MenuTreeVO;

import java.util.List;

public interface MenuService extends IService<Menu> {
    Page<MenuListVO> listMenu(MenuGetDTO dto);

    List<MenuTreeVO> getMenuTree(MenuGetTreeDTO dto);

    MenuListVO getMenuDetail(Long id);

    void createMenu(MenuCreateDTO dto);

    void updateMenu(Long id ,MenuCreateDTO dto);

    void deleteMenu(Long id);

    List<MenuTreeVO> getCurrentUserMenu();
}
