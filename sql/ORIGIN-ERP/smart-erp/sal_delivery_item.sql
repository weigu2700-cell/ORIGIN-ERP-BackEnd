create table `smart-erp`.sal_delivery_item
(
    id                  bigint         not null comment 'id'
        primary key,
    delivery_id         bigint         not null comment '表头id',
    line_no             varchar(100)   not null comment '行号',
    sales_order_item_id bigint         not null comment '销售明细id',
    material_id         bigint         not null comment '物料id',
    warehouse_id        bigint         not null comment '仓库id',
    quantity            decimal(18, 4) not null comment '出库数量',
    create_time         datetime       not null comment '创建时间',
    update_time         datetime       null comment '更新时间'
)
    comment '销售出库明细表';

create index idx_delivery_id
    on `smart-erp`.sal_delivery_item (delivery_id);

create index idx_material_warehouse
    on `smart-erp`.sal_delivery_item (material_id, warehouse_id);

create index idx_sales_order_item_id
    on `smart-erp`.sal_delivery_item (sales_order_item_id);

