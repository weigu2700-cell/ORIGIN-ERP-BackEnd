package org.smart.erp.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.result.Result;
import org.smart.erp.inventory.dto.transactionDto.ListDto;
import org.smart.erp.inventory.service.TransactionService;
import org.smart.erp.inventory.vo.TransactionVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "库存流水")
@RestController
@RequestMapping("inventory/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "分页查询库存流水")
    @GetMapping
    public Result<Page<TransactionVO>> list(ListDto listDto) {
        return Result.success(transactionService.listTransaction(listDto));
    }

    @GetMapping("/export")
    public Result<Void> export(HttpServletResponse response) throws IOException {
        transactionService.export(response);
        return Result.success();
    }

    @PostMapping("/import")
    public Result<Void> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        transactionService.importExcel(file);
        return Result.success();
    }
}
