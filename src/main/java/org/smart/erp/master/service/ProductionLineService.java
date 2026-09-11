package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.master.dto.ProductionLineDto.ProductionLineAddDto;
import org.smart.erp.master.dto.ProductionLineDto.ProductionLinePageDto;
import org.smart.erp.master.dto.ProductionLineDto.ProductionLineUpdateDto;
import org.smart.erp.master.entity.ProductionLine;
import org.smart.erp.master.enums.ProductionLineStatus;
import org.smart.erp.master.vo.ProductionLineVo;

import java.util.List;

public interface ProductionLineService extends IService<ProductionLine> {
    void addProductionLine(ProductionLineAddDto dto);

    Page<ProductionLineVo> pageProductionLine(ProductionLinePageDto dto);

    ProductionLineVo getProductionLine(Long id);

    void updateProductionLine(Long id, ProductionLineUpdateDto dto);

    void updateProductionLineStatus(Long id, ProductionLineStatus status);

    void exportProductionLine(List<Long> ids, HttpServletResponse response);
}
