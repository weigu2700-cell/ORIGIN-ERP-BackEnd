ALTER TABLE sal_order_item
    ADD COLUMN remark varchar(255) NULL COMMENT '备注' AFTER delivery_date;
