package org.smart.erp.inventory.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.smart.erp.inventory.service.FinishWarehousingService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inv/finish-warehousing")
@Tag(name = "成品入库", description = "成品入库相关接口")
public class FinishWarehousingController {

    private final FinishWarehousingService finishWarehousingService;

    public FinishWarehousingController(FinishWarehousingService finishWarehousingService) {
        this.finishWarehousingService = finishWarehousingService;
    }
}
