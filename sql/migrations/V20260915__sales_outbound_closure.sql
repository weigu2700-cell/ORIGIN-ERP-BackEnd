-- 销售到出库闭环增量迁移；在已有 smart-erp 数据库执行一次。
-- MySQL DDL 会隐式提交，因此请先备份并在维护窗口执行。

ALTER TABLE `smart-erp`.sal_order
    ADD COLUMN version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER remark;

ALTER TABLE `smart-erp`.sal_delivery
    ADD COLUMN version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER remark;

ALTER TABLE `smart-erp`.sal_order_item
    ADD COLUMN delivered_quantity decimal(18, 4) NOT NULL DEFAULT 0.0000 COMMENT '已发货数量' AFTER amount;

ALTER TABLE `smart-erp`.sal_delivery_item
    ADD COLUMN reserved_quantity decimal(18, 4) NOT NULL DEFAULT 0.0000 COMMENT '本单已预占数量' AFTER quantity;

ALTER TABLE `smart-erp`.inv_transaction
    MODIFY COLUMN business_type varchar(64) NOT NULL COMMENT '来源业务类型',
    MODIFY COLUMN business_no varchar(100) NOT NULL COMMENT '来源业务单号';

ALTER TABLE `smart-erp`.prd_production_demand
    ADD COLUMN warehouse_id bigint NULL COMMENT '目标入库仓库id' AFTER material_id,
    ADD INDEX idx_source_material_warehouse (source_type, source_no, material_id, warehouse_id);

ALTER TABLE `smart-erp`.prd_production_order
    ADD COLUMN warehouse_id bigint NULL COMMENT '目标入库仓库id' AFTER material_id;
