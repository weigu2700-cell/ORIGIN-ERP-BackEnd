package org.smart.erp.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.inventory.dto.transactionDto.ListDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.entity.Transaction;
import org.smart.erp.inventory.enums.TransactionType;
import org.smart.erp.inventory.vo.TransactionVO;
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

    Page<TransactionVO> listTransaction(ListDto listDto);

    void export(HttpServletResponse response);

    void importExcel(MultipartFile file);
}
