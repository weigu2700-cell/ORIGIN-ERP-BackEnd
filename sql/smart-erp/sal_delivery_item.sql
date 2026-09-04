create table `smart-erp`.sal_delivery_item
(
    id                  bigint         not null comment '出库明细id'
        primary key,
    delivery_id         bigint         not null comment '出库单id',
    line_no             int            not null comment '行号',
    sales_order_item_id bigint         not null comment '销售订单明细id',
    material_id         bigint         not null comment '物料id',
    warehouse_id        bigint         not null comment '仓库id',
    quantity            decimal(18, 4) not null comment '出库数量'
)
    comment '销售出库明细';

create index idx_delivery_item_delivery
    on `smart-erp`.sal_delivery_item (delivery_id);

create index idx_delivery_item_material
    on `smart-erp`.sal_delivery_item (material_id);
