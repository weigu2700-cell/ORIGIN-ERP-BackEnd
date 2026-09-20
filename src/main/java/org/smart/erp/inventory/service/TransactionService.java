package org.smart.erp.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.inventory.dto.TransactionPageDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.entity.Transaction;
import org.smart.erp.inventory.enums.TransactionType;
import org.smart.erp.inventory.vo.TransactionVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface TransactionService extends IService<Transaction> {

    void recordTransaction(
            MaterialStock stock,
            TransactionType type,
            BigDecimal quantity,
            BigDecimal beforeOnHand,
            BigDecimal beforeReserved,
            String businessType,
            String businessNo,
            String remark
    );

    @PreAuthorize("hasAnyAuthority('inventory:transaction:list')")
    Page<TransactionVo> pageTransaction(TransactionPageDto listDto);

    @PreAuthorize("hasAnyAuthority('inventory:transaction:export')")
    void export(HttpServletResponse response);

    @PreAuthorize("hasAnyAuthority('inventory:transaction:import')")
    void importExcel(MultipartFile file);
}
