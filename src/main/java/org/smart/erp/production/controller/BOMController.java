package org.smart.erp.production.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smart.erp.common.result.Result;
import org.smart.erp.production.dto.pageBOMDto;
import org.smart.erp.production.service.BOMService;
import org.smart.erp.production.vo.BOMVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("prd/bom")
public class BOMController {

    private final BOMService bomService;

    public BOMController(BOMService bomService) {
        this.bomService = bomService;
    }


    @GetMapping("/{id}")
    public Result<BOMVo> getBOMById(Long id) {
        return Result.success(bomService.getBOMDetailById(id));
    }

    @GetMapping
    public Result<Page<BOMVo>> getPage(pageBOMDto dto) {
        return Result.success(bomService.getPageBOMVo(dto));
    }
}
