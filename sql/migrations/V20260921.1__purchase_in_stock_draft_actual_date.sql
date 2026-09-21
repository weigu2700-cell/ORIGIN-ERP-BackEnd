-- 采购订单审批后自动生成的入库单处于草稿状态，实际入库日期要到上架时才确定。
-- 仅允许草稿阶段的 in_date 为空，其他业务必填字段保持原约束。
SET @purchase_in_stock_in_date_nullable = IF(
    (SELECT COUNT(*)
       FROM information_schema.columns
      WHERE table_schema = 'smart-erp'
        AND table_name = 'pur_in_stock'
        AND column_name = 'in_date'
        AND is_nullable = 'NO') > 0,
    'ALTER TABLE `smart-erp`.`pur_in_stock` MODIFY COLUMN `in_date` DATETIME NULL COMMENT ''实际入库日期（上架时填写）''',
    'SELECT 1'
);
PREPARE purchase_in_stock_in_date_nullable_stmt FROM @purchase_in_stock_in_date_nullable;
EXECUTE purchase_in_stock_in_date_nullable_stmt;
DEALLOCATE PREPARE purchase_in_stock_in_date_nullable_stmt;
