-- 草稿单尚未实际入库；上架成功时才写入实际入库时间。
ALTER TABLE pur_in_stock MODIFY COLUMN in_date DATETIME NULL COMMENT '实际入库日期（上架后填写）';
