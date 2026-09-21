-- 缺料自动生成的生产领料草稿暂未分配仓库，采购上架后再回填。
-- 人工新增及审批/确认领料的业务校验仍要求仓库，不删除或改写已有业务数据。
-- 该脚本幂等：仅当 warehouse_id 当前为 NOT NULL 时才改为可空；表或列不存在时自动跳过。
SET @prd_picking_warehouse_nullable = IF(
    (SELECT COUNT(*)
       FROM information_schema.columns
      WHERE table_schema = 'smart-erp'
        AND table_name = 'prd_production_picking'
        AND column_name = 'warehouse_id'
        AND is_nullable = 'NO') > 0,
    'ALTER TABLE `smart-erp`.`prd_production_picking` MODIFY COLUMN `warehouse_id` BIGINT NULL COMMENT ''仓库id；缺料草稿待采购上架后回填，人工新增和确认领料前仍需指定''',
    'SELECT 1'
);
PREPARE prd_picking_warehouse_nullable_stmt FROM @prd_picking_warehouse_nullable;
EXECUTE prd_picking_warehouse_nullable_stmt;
DEALLOCATE PREPARE prd_picking_warehouse_nullable_stmt;
