package org.smart.erp.inventory.service.impl;

import cn.idev.excel.FastExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.excel.ExcelRowCollectListener;
import org.smart.erp.common.util.PageConvertUtils;
import org.smart.erp.inventory.dto.ExcelPrintDto.TransactionImportDto;
import org.smart.erp.inventory.dto.transactionDto.ListDto;
import org.smart.erp.inventory.entity.MaterialStock;
import org.smart.erp.inventory.entity.Transaction;
import org.smart.erp.inventory.enums.TransactionType;
import org.smart.erp.inventory.mapper.TransactionMapper;
import org.smart.erp.inventory.service.MaterialStockService;
import org.smart.erp.inventory.service.TransactionService;
import org.smart.erp.inventory.vo.ExcelPrintVo.TransactionExportVO;
import org.smart.erp.inventory.vo.TransactionVO;
import org.smart.erp.master.entity.Material;
import org.smart.erp.master.entity.Warehouse;
import org.smart.erp.master.mapper.MaterialMapper;
import org.smart.erp.master.mapper.WarehouseMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionServiceImpl
        extends ServiceImpl<TransactionMapper, Transaction>
        implements TransactionService
{

    private static final int BATCH_SIZE = 200;

    private final TransactionMapper transactionMapper;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;
    private final MaterialStockService materialStockService;
    private final TransactionTemplate transactionTemplate;

    public TransactionServiceImpl(
            TransactionMapper transactionMapper,
            MaterialMapper materialMapper,
            WarehouseMapper warehouseMapper,
            @Lazy MaterialStockService materialStockService,
            PlatformTransactionManager transactionManager)
    {
        this.transactionMapper = transactionMapper;
        this.materialMapper = materialMapper;
        this.warehouseMapper = warehouseMapper;
        this.materialStockService = materialStockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
        List<Transaction> transactions = this.list();

        Map<Long, Material> materialMap = queryMaterialMap(transactions);
        Map<Long, String> warehouseNameMap = queryWarehouseNameMap(transactions);

        List<TransactionExportVO> data = transactions.stream()
                .map(transaction -> {
                    TransactionExportVO vo = new TransactionExportVO();
                    BeanUtils.copyProperties(transaction, vo);

                    if (transaction.getTransactionType() != null) {
                        vo.setTransactionTypeName(
                                transaction.getTransactionType().getDesc()
                        );
                    }

                    Material material =
                            materialMap.get(transaction.getMaterialId());

                    if (material != null) {
                        vo.setMaterialCode(material.getCode());
                        vo.setMaterialName(material.getName());
                    }

                    vo.setWarehouseName(
                            warehouseNameMap.get(transaction.getWarehouseId())
                    );

                    return vo;
                })
                .toList();

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        response.setCharacterEncoding("utf-8");

        try {
            String fileName = URLEncoder.encode(
                    "库存流水",
                    StandardCharsets.UTF_8
            ).replace("+", "%20");

            response.setHeader(
                    "Content-Disposition",
                    "attachment;filename*=utf-8''"
                            + fileName
                            + ".xlsx"
            );

            FastExcel.write(
                            response.getOutputStream(),
                            TransactionExportVO.class
                    )
                    .sheet("库存流水")
                    .doWrite(data);

        } catch (IOException e) {
            log.error("导出库存流水失败", e);
            throw new BusinessException(500, "导出库存流水失败");
        }
    }

    @Override
    public void importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "导入文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.endsWith(".xlsx")) {
            throw new BusinessException(400, "导入文件格式不正确，仅支持 .xlsx 文件");
        }

        try {
            FastExcel.read(
                            file.getInputStream(),
                            TransactionImportDto.class,
                            new ExcelRowCollectListener<>(this::handleImportedRows)
                    )
                    .sheet()
                    .doRead();
        } catch (IOException e) {
            log.error("读取库存流水导入文件失败", e);
            throw new BusinessException(500, "读取导入文件失败");
        }
    }

    /**
     * Excel 读取完成后的统一处理：
     * 1) 全表解析 + 业务校验（含库存可用性预检），无副作用，错误带精确行号；
     * 2) 预检全部通过后按批提交，每批一个事务，避免超大单事务长时间持锁。
     */
    private void handleImportedRows(List<TransactionImportDto> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(400, "导入文件没有数据");
        }

        // 1) 一次性反查：物料编码 -> 物料、仓库名称 -> 仓库
        List<String> materialCodes = rows.stream()
                .map(TransactionImportDto::getMaterialCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, Material> materialByCode = materialCodes.isEmpty() ? Map.of()
                : materialMapper.selectList(
                                new LambdaQueryWrapper<Material>().in(Material::getCode, materialCodes)
                        ).stream()
                        .collect(Collectors.toMap(Material::getCode, m -> m));

        List<String> warehouseNames = rows.stream()
                .map(TransactionImportDto::getWarehouseName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, Warehouse> warehouseByName = warehouseNames.isEmpty() ? Map.of()
                : warehouseMapper.selectList(
                                new LambdaQueryWrapper<Warehouse>().in(Warehouse::getName, warehouseNames)
                        ).stream()
                        .collect(Collectors.toMap(Warehouse::getName, w -> w));

        // 2) 逐行解析与校验（仅校验，不改动数据），行号便于定位
        List<ImportAction> actions = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNo = ExcelRowCollectListener.excelRowNo(i);
            TransactionImportDto dto = rows.get(i);

            Material material = materialByCode.get(dto.getMaterialCode());
            if (material == null) {
                throw new BusinessException(400, "第 " + rowNo + " 行：物料编码「"
                        + dto.getMaterialCode() + "」不存在");
            }
            Warehouse warehouse = warehouseByName.get(dto.getWarehouseName());
            if (warehouse == null) {
                throw new BusinessException(400, "第 " + rowNo + " 行：仓库名称「"
                        + dto.getWarehouseName() + "」不存在");
            }
            if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "第 " + rowNo + " 行：数量必须大于 0");
            }
            TransactionType type = parseTransactionType(dto.getTransactionTypeName());
            if (type == null) {
                throw new BusinessException(400, "第 " + rowNo + " 行：流水类型「"
                        + dto.getTransactionTypeName() + "」不合法，可选值：入库/出库/预占/释放预占");
            }

            actions.add(new ImportAction(rowNo, material, warehouse, type, dto.getQuantity(), dto.getRemark()));
        }

        // 3) 库存可用性预检：一次批量查询所有涉及库存行，整表校验完再动手，
        //    避免"执行到第 500 行才发现第 300 行库存不足"的尴尬回滚
        preCheckStocks(actions);

        // 4) 按批执行库存动作：每批一个事务，批内任一行失败则回滚该批并终止。
        //    校验已前置，此阶段失败仅剩并发竞态等罕见情况；报错会提示已提交行数，避免重复导入。
        int committed = 0;
        for (int from = 0; from < actions.size(); from += BATCH_SIZE) {
            List<ImportAction> batch = actions.subList(from, Math.min(from + BATCH_SIZE, actions.size()));
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    for (ImportAction action : batch) {
                        try {
                            applyAction(action);
                        } catch (BusinessException e) {
                            throw new BusinessException(400, "第 " + action.rowNo + " 行：" + e.getMessage());
                        }
                    }
                });
                committed += batch.size();
            } catch (BusinessException e) {
                throw new BusinessException(400, e.getMessage()
                        + "（前 " + committed + " 行已成功导入并提交，请勿重复导入；修正文件后从剩余行开始重导）");
            }
        }
    }

    /** 按流水类型分发到库存服务动作（更新库存 + 写流水在同一事务内完成） */
    private void applyAction(ImportAction action) {
        switch (action.type) {
            case INBOUND -> materialStockService.inboundStock(
                    action.material.getId(), action.warehouse.getId(), action.quantity);
            case OUTBOUND -> materialStockService.outboundStock(
                    action.material.getId(), action.warehouse.getId(), action.quantity);
            case RESERVE -> materialStockService.reserveStock(
                    action.material.getId(), action.warehouse.getId(), action.quantity);
            case RELEASE -> materialStockService.releaseStock(
                    action.material.getId(), action.warehouse.getId(), action.quantity);
        }
    }

    /**
     * 库存可用性预检：批量查出涉及的所有库存行做整表校验。
     * 规则与 {@code MaterialStockServiceImpl} 各动作内部校验保持一致，
     * 执行阶段的乐观锁 + 余额校验仍作为并发兜底。
     */
    private void preCheckStocks(List<ImportAction> actions) {
        List<Long> materialIds = actions.stream()
                .map(action -> action.material.getId()).distinct().toList();
        List<Long> warehouseIds = actions.stream()
                .map(action -> action.warehouse.getId()).distinct().toList();
        if (materialIds.isEmpty() || warehouseIds.isEmpty()) {
            return;
        }

        List<MaterialStock> stocks = materialStockService.list(
                new LambdaQueryWrapper<MaterialStock>()
                        .in(MaterialStock::getMaterialId, materialIds)
                        .in(MaterialStock::getWarehouseId, warehouseIds));
        Map<StockKey, MaterialStock> stockMap = stocks.stream()
                .collect(Collectors.toMap(
                        s -> new StockKey(s.getMaterialId(), s.getWarehouseId()),
                        s -> s));

        for (ImportAction action : actions) {
            MaterialStock stock = stockMap.get(
                    new StockKey(action.material.getId(), action.warehouse.getId()));
            String prefix = "第 " + action.rowNo + " 行：";
            if (stock == null) {
                throw new BusinessException(400, prefix + "物料「" + action.material.getCode()
                        + "」在仓库「" + action.warehouse.getName() + "」无库存档案，请先建档");
            }
            switch (action.type) {
                case RESERVE -> {
                    if (stock.getOnHand().subtract(stock.getReserved()).compareTo(action.quantity) < 0) {
                        throw new BusinessException(400, prefix + "可用库存不足（在库 "
                                + stock.getOnHand() + "，已预占 " + stock.getReserved() + "）");
                    }
                }
                case RELEASE -> {
                    if (stock.getReserved().compareTo(action.quantity) < 0) {
                        throw new BusinessException(400, prefix + "预留库存不足（当前预留 "
                                + stock.getReserved() + "）");
                    }
                }
                case OUTBOUND -> {
                    if (action.quantity.compareTo(stock.getOnHand()) > 0) {
                        throw new BusinessException(400, prefix + "出库数量不能大于在库数量（在库 "
                                + stock.getOnHand() + "）");
                    }
                    if (action.quantity.compareTo(stock.getReserved()) > 0) {
                        throw new BusinessException(400, prefix + "出库数量不能大于预留数量（当前预留 "
                                + stock.getReserved() + "）");
                    }
                }
                case INBOUND -> { /* 入库无余额限制 */ }
            }
        }
    }

    private TransactionType parseTransactionType(String desc) {
        for (TransactionType type : TransactionType.values()) {
            if (type.getDesc().equals(desc)) {
                return type;
            }
        }
        return null;
    }

    /** 导入动作载体：携带 Excel 行号与解析结果，便于批量执行与错误定位 */
    private record ImportAction(
            int rowNo,
            Material material,
            Warehouse warehouse,
            TransactionType type,
            BigDecimal quantity,
            String remark
    ) {
    }

    /** 库存行定位键：(物料, 仓库) */
    private record StockKey(long materialId, long warehouseId) {
    }
}
