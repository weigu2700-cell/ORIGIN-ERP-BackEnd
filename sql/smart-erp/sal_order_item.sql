create table `smart-erp`.sal_order_item
(
    id             bigint         not null comment 'id'
        primary key,
    sales_order_id bigint         not null comment '销售订单id',
    line_no        int            not null comment '行号',
    material_id    bigint         not null comment '物料id',
    quantity       decimal(18, 4) not null comment '销售数量',
    unit_price     decimal(18, 4) not null comment '单价',
    amount         decimal(18, 4) not null comment '总价',
    warehouse_id   bigint         not null comment '仓库id',
    delivery_date  datetime       null comment '要求交货日期'
)
    comment '销售订单明细表';

create index idx_material_id
    on `smart-erp`.sal_order_item (material_id);

create index idx_sales_order_id
    on `smart-erp`.sal_order_item (sales_order_id);

create index idx_sales_order_line
    on `smart-erp`.sal_order_item (sales_order_id, line_no);

create index idx_warehouse_id
    on `smart-erp`.sal_order_item (warehouse_id);

