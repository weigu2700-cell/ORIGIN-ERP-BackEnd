package org.smart.erp.master.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineCreateDTO;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineListDTO;
import org.smart.erp.master.dto.ProductionLineDTO.ProductionLineUpdateDTO;
import org.smart.erp.master.entity.ProductionLine;
import org.smart.erp.master.enums.ProductionLineStatus;
import org.smart.erp.master.vo.ProductionLineVO;

public interface ProductionLineService extends IService<ProductionLine> {
    void createProductionLine(ProductionLineCreateDTO dto);

    Page<ProductionLineVO> listProductionLine(ProductionLineListDTO dto);

    ProductionLineVO getProductionLine(Long id);

    void updateProductionLine(Long id, ProductionLineUpdateDTO dto);

    void updateProductionLineStatus(Long id, ProductionLineStatus status);
}
