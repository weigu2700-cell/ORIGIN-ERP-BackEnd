package org.smart.erp.production.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.production.dto.creatBOMDto;
import org.smart.erp.production.entity.BOM;

public interface BOMService extends IService<BOM> {

    void createBOM(creatBOMDto dto);

}
