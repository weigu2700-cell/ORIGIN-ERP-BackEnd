create table `smart-erp`.prd_production_picking
(
    id                  bigint                        not null comment 'id'
        primary key,
    picking_no          varchar(100)                  not null comment '领料单号',
    production_order_id bigint                        not null comment '生产订单id',
    material_id         bigint                        not null comment '物料id',
    warehouse_id        bigint                        not null comment '仓库id',
    planned_quantity    decimal(18, 4) default 0.0000 not null comment '计划领料数量',
    actual_quantity     decimal(18, 4) default 0.0000 not null comment '实际领料数量',
    status              tinyint        default 0      not null comment '领料单状态',
    picking_time        datetime                      null comment '领料时间',
    create_time         datetime                      not null comment '创建时间',
    update_time         datetime                      null comment '修改时间',
    purchaseDemandId    bigint                        null comment '采购需求id',
    constraint uk_picking_no
        unique (picking_no)
)
    comment '生产领料表';

create index idx_material_id
    on `smart-erp`.prd_production_picking (material_id);

create index idx_production_order_id
    on `smart-erp`.prd_production_picking (production_order_id);

create index idx_warehouse_id
    on `smart-erp`.prd_production_picking (warehouse_id);

