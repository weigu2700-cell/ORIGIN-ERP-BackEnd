package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.ProductionReportAddDto;
import org.smart.erp.production.dto.ProductionReportPageDto;
import org.smart.erp.production.entity.ProductionReport;
import org.smart.erp.production.vo.ProductionReportVo;

public interface ProductionReportService extends IService<ProductionReport> {
    void addProductionReport(ProductionReportAddDto dto);

    Page<ProductionReportVo> pageProductionReport(ProductionReportPageDto dto);

    ProductionReportVo detailProductionReport(Long id);

    void approveProductionReport(Long id);

    void cancelProductionReport(Long id);

    void rejectProductionReport(Long id);

    void finishProductionReport(Long id);
}
