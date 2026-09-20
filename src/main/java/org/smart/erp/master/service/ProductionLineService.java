package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.ProductionLineDto.ProductionLineAddDto;
import org.smart.erp.master.dto.ProductionLineDto.ProductionLinePageDto;
import org.smart.erp.master.dto.ProductionLineDto.ProductionLineUpdateDto;
import org.smart.erp.master.entity.ProductionLine;
import org.smart.erp.master.enums.ProductionLineStatus;
import org.smart.erp.master.vo.ProductionLineVo;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ProductionLineService extends IService<ProductionLine> {
    @PreAuthorize("hasAnyAuthority('master:production_line:create')")
    void addProductionLine(ProductionLineAddDto dto);

    @PreAuthorize("hasAnyAuthority('master:production_line:list')")
    Page<ProductionLineVo> pageProductionLine(ProductionLinePageDto dto);

    @PreAuthorize("hasAnyAuthority('master:production_line:get')")
    ProductionLineVo getProductionLine(Long id);

    @PreAuthorize("hasAnyAuthority('master:production_line:update')")
    void updateProductionLine(Long id, ProductionLineUpdateDto dto);

    @PreAuthorize("hasAnyAuthority('master:production_line:status')")
    void updateProductionLineStatus(Long id, ProductionLineStatus status);

    @PreAuthorize("hasAnyAuthority('master:production_line:export')")
    void exportProductionLine(List<Long> ids, HttpServletResponse response);
}
