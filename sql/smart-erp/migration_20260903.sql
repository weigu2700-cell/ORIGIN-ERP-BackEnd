-- 已有数据库的兼容补丁：SalesOrderItem 实体会写入 create_time。
-- 可重复执行；全新数据库请直接使用更新后的 sal_order_item.sql。
USE `smart-erp`;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sal_order_item'
      AND column_name = 'create_time'
);

SET @add_column_sql := IF(
    @column_exists = 0,
    'ALTER TABLE sal_order_item ADD COLUMN create_time datetime NULL COMMENT ''创建时间''',
    'SELECT 1'
);
PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

-- 旧表可能把无默认值的 datetime 初始化成零日期，YEAR() 可识别它而不再解析零日期字面量。
UPDATE sal_order_item
SET create_time = NOW()
WHERE create_time IS NULL OR YEAR(create_time) = 0;

ALTER TABLE sal_order_item
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间';
