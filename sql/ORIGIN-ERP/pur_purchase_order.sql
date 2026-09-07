create table `smart-erp`.pur_purchase_order
(
    id                     bigint                        not null comment '订单ID'
        primary key,
    purchase_order_no      varchar(100)                  not null comment '采购订单号',
    purchase_demand_id     bigint                        not null comment '关联的采购需求ID',
    material_id            bigint                        not null comment '采购物料ID',
    supplier_id            bigint                        null comment '供应商ID（若需要）',
    planned_quantity       decimal(18, 4)                not null comment '计划采购数量',
    complete_quantity      decimal(18, 4) default 0.0000 null comment '已到货/已采购数量',
    unit_price             decimal(18, 4)                null comment '采购单价（含税或不含税，可约定）',
    total_amount           decimal(18, 4)                null comment '订单总金额（= planned_quantity * unit_price，可冗余）',
    order_date             datetime                      not null comment '下单日期',
    expected_delivery_date datetime                      null comment '期望交货日期',
    actual_delivery_date   datetime                      null comment '实际交货日期',
    status                 tinyint                       not null comment '订单状态（0-草稿，1-已审批，2-已发货，3-已收货，4-已关闭等）',
    create_time            datetime                      not null comment '创建时间',
    update_time            datetime                      null comment '更新时间',
    constraint uk_order_no
        unique (purchase_order_no)
)
    comment '采购订单表';

create index idx_demand_id
    on `smart-erp`.pur_purchase_order (purchase_demand_id);

create index idx_material
    on `smart-erp`.pur_purchase_order (material_id);

create index idx_order_date
    on `smart-erp`.pur_purchase_order (order_date);

create index idx_status
    on `smart-erp`.pur_purchase_order (status);

create index idx_supplier
    on `smart-erp`.pur_purchase_order (supplier_id);

