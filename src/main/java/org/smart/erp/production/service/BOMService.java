package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.creatBOMDto;
import org.smart.erp.production.dto.pageBOMDto;
import org.smart.erp.production.entity.BOM;
import org.smart.erp.production.vo.BOMVo;

public interface BOMService extends IService<BOM> {

    void activeBOM(Long id);

    void createBOM(creatBOMDto dto);

    BOMVo getBOMDetailById(Long id);

    Page<BOMVo> getPageBOMVo(pageBOMDto dto);

    void disableBOM(Long id);
}
