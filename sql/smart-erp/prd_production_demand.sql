create table `smart-erp`.prd_production_demand
(
    id              bigint            not null comment 'id'
        primary key,
    demand_no       varchar(100)      not null comment '需求单号',
    material_id     bigint            not null comment '所需物料id',
    demand_quantity decimal(18, 4)    not null comment '所需数量',
    source_type     tinyint           not null comment '来源类型',
    source_no       varchar(100)      not null comment '来源单号',
    status          tinyint default 0 not null comment '状态',
    create_time     datetime          not null comment '创建时间',
    update_time     datetime          null comment '更新时间',
    quantity        decimal(18, 4)    null comment '需求数量',
    constraint uk_demand_no
        unique (demand_no)
)
    comment '生产订单信息表';

create index idx_material_id
    on `smart-erp`.prd_production_demand (material_id);

create index idx_source
    on `smart-erp`.prd_production_demand (source_type, source_no);

