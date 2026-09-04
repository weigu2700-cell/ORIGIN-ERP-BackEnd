package org.smart.erp.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.result.Result;
import org.smart.erp.inventory.dto.transactionDto.ListDto;
import org.smart.erp.inventory.service.TransactionService;
import org.smart.erp.inventory.vo.TransactionVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "库存流水", description = "库存流水的分页查询、Excel 导出与导入")
@RestController
@RequestMapping("inventory/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "分页查询库存流水",
            description = "支持按仓库、物料、业务类型、业务单号过滤")
    @GetMapping
    public Result<Page<TransactionVO>> list(ListDto listDto) {
        return Result.success(transactionService.listTransaction(listDto));
    }

    @Operation(summary = "导出库存流水",
            description = "以 Excel 文件流形式下载全部库存流水")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        transactionService.export(response);
    }

    @Operation(summary = "导入库存流水",
            description = "上传 .xlsx 文件批量导入流水；会按物料编码、仓库名称反查并同步更新库存，"
                    + "整表校验通过后才按批提交，失败会提示具体行号")
    @PostMapping("/import")
    public Result<Void> importExcel(
            @Parameter(description = "Excel 文件（.xlsx）", required = true)
            @RequestParam("file") MultipartFile file) {
        transactionService.importExcel(file);
        return Result.success();
    }
}
