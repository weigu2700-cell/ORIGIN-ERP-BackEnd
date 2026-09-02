package org.smart.erp.inventory.service.impl;

import cn.idev.excel.FastExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.inventory.dto.transactionDto.ListDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.entity.Transaction;
import org.smart.erp.inventory.enums.TransactionType;
import org.smart.erp.inventory.mapper.TransactionMapper;
import org.smart.erp.inventory.vo.ExcelPrintVo.TransactionPrintVo;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.smart.erp.inventory.service.TransactionService;
import org.smart.erp.inventory.vo.TransactionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl
        extends ServiceImpl<TransactionMapper, Transaction>
        implements TransactionService
{

    private final TransactionMapper transactionMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    public TransactionServiceImpl(
            TransactionMapper transactionMapper,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper)
    {
        this.transactionMapper = transactionMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
    }

    /**
     * 查询物料map
     * @param transactions 交易列表
     * @return 物料map
     */
    private Map<Long, Material> queryMaterialMap(List<Transaction> transactions) {
        List<Long> materialIds = transactions.stream()
                .map(Transaction::getMaterialId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return materialMapper.selectByIds(materialIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Material::getId, m -> m));
    }

    /**
     * 查询仓库名称map
     * @param transactions 交易列表
     * @return 仓库名称map
     */
    private Map<Long, String> queryWarehouseNameMap(List<Transaction> transactions) {
        List<Long> warehouseIds = transactions.stream()
                .map(Transaction::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (warehouseIds.isEmpty()) {
            return Map.of();
        }
        return warehouseMapper.selectByIds(warehouseIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
    }

    @Override
    public void recordTransaction(
            MaterialStock stock,
            TransactionType type,
            BigDecimal quantity,
            BigDecimal beforeOnHand,
            BigDecimal beforeReserved,
            String businessType,
            String businessNo,
            String remark
    )
    {
        Transaction transaction = new Transaction();
        transaction.setWarehouseId(stock.getWarehouseId());
        transaction.setMaterialId(stock.getMaterialId());

        transaction.setTransactionType(type);

        transaction.setBusinessType(businessType);
        transaction.setBusinessNo(businessNo);

        transaction.setQuantity(quantity);

        transaction.setBeforeOnHand(beforeOnHand);
        transaction.setAfterOnHand(stock.getOnHand());

        transaction.setBeforeReserved(beforeReserved);
        transaction.setAfterReserved(stock.getReserved());

        transaction.setRemark(remark);

        transactionMapper.insert(transaction);
    }

    @Override
    public Page<TransactionVO> listTransaction(ListDto listDto) {
        LambdaQueryWrapper<Transaction> queryWrapper =
                new LambdaQueryWrapper<Transaction>()
                        .eq(listDto.getWarehouseId() != null, Transaction::getWarehouseId, listDto.getWarehouseId())
                        .eq(listDto.getMaterialId() != null, Transaction::getMaterialId, listDto.getMaterialId())
                        .eq(StringUtils.hasText(listDto.getBusinessType()), Transaction::getBusinessType, listDto.getBusinessType())
                        .eq(StringUtils.hasText(listDto.getBusinessNo()), Transaction::getBusinessNo, listDto.getBusinessNo());

        Page<Transaction> page = this.page(new Page<>(listDto.getPageNum(), listDto.getPageSize()), queryWrapper);

        Map<Long, Material> materialMap = queryMaterialMap(page.getRecords());
        Map<Long, String> warehouseNameMap = queryWarehouseNameMap(page.getRecords());

        return PageConvertUtils.convert(page, transaction -> {
            TransactionVO vo = new TransactionVO();
            BeanUtils.copyProperties(transaction, vo);

            TransactionType type = transaction.getTransactionType();
            if (type != null) {
                vo.setTransactionType(type.getCode());
                vo.setTransactionTypeName(type.getDesc());
            }

            Material material = materialMap.get(transaction.getMaterialId());
            if (material != null) {
                vo.setMaterialName(material.getName());
                vo.setMaterialCode(material.getCode());
            }
            vo.setWarehouseName(warehouseNameMap.get(transaction.getWarehouseId()));
            return vo;
        });
    }

    @Override
    public void export(HttpServletResponse response) {
        List<Transaction> transactionList = this.list();

        Map<Long, Material> materialMap = queryMaterialMap(transactionList);
        Map<Long, String> warehouseNameMap = queryWarehouseNameMap(transactionList);

        List<TransactionPrintVo> transactionPrintVoList = transactionList.stream().map(transaction -> {
            TransactionPrintVo vo = new TransactionPrintVo();
            BeanUtils.copyProperties(transaction, vo);

            TransactionType type = transaction.getTransactionType();
            if (type != null) {
                vo.setTransactionTypeName(type.getDesc());
            }
            Material material = materialMap.get(transaction.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getCode());
                vo.setMaterialName(material.getName());
            }
            vo.setWarehouseName(warehouseNameMap.get(transaction.getWarehouseId()));
            return vo;
        }).toList();

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=inventory-transactions.xlsx");

        try {
            FastExcel.write(
                    response.getOutputStream(),
                    TransactionPrintVo.class
            ).sheet("库存流水").doWrite(transactionPrintVoList);
        } catch (IOException e) {
            throw new BusinessException(500, "导出库存流水失败: " + e.getMessage());
        }
    }


}
