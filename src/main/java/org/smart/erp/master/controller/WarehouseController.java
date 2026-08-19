package org.smart.erp.master.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("master/warehouse")
@Tag(name = "仓库管理", description = "仓库的增删改查")
public class WarehouseController {
}
