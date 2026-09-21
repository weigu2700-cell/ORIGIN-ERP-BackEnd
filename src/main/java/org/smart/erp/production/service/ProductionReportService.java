package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.smart.erp.production.dto.ProductionReportAddDto;
import org.smart.erp.production.dto.ProductionReportPageDto;
import org.smart.erp.production.entity.ProductionReport;
import org.smart.erp.production.vo.ProductionReportVo;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProductionReportService extends IService<ProductionReport> {
    @PreAuthorize("hasAnyAuthority('prd:report:add')")
    void addProductionReport(ProductionReportAddDto dto);

    @PreAuthorize("hasAnyAuthority('prd:report:page')")
    Page<ProductionReportVo> pageProductionReport(ProductionReportPageDto dto);

    @PreAuthorize("hasAnyAuthority('prd:report:get')")
    ProductionReportVo detailProductionReport(Long id);

    @PreAuthorize("hasAnyAuthority('prd:report:approve')")
    void approveProductionReport(Long id);

    @PreAuthorize("hasAnyAuthority('prd:report:cancel')")
    void cancelProductionReport(Long id);

    @PreAuthorize("hasAnyAuthority('prd:report:reject')")
    void rejectProductionReport(Long id);

    @PreAuthorize("hasAnyAuthority('prd:report:finish')")
    void finishProductionReport(Long id);
}
