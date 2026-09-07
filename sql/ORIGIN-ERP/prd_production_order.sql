create table `smart-erp`.prd_production_order
(
    id                   bigint                        not null comment 'id'
        primary key,
    production_order_no  varchar(100)                  not null comment '生产订单编号',
    production_demand_id bigint                        not null comment '生产需求id',
    material_id          bigint                        not null comment '生产物料id',
    planned_quantity     decimal(18, 4)                not null comment '计划生产数量',
    completed_quantity   decimal(18, 4) default 0.0000 not null comment '已完成数量',
    status               tinyint        default 0      not null comment '订单状态',
    planned_start_time   datetime                      not null comment '计划开始时间',
    planned_end_time     datetime                      not null comment '计划完成时间',
    actual_start_time    datetime                      not null comment '实际开始时间',
    actual_end_time      datetime                      not null comment '实际完成时间',
    remark               varchar(255)                  null comment '备注',
    create_time          datetime                      not null comment '创建时间',
    update_time          datetime                      null comment '更新时间',
    constraint uk_production_order_no
        unique (production_order_no)
)
    comment '生产订单表';

create index idx_material_id
    on `smart-erp`.prd_production_order (material_id);

create index idx_production_demand_id
    on `smart-erp`.prd_production_order (production_demand_id);

create index idx_production_status
    on `smart-erp`.prd_production_order (status);

