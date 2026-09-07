create table `smart-erp`.pur_purchase_demand
(
    id                 bigint         not null comment 'id'
        primary key,
    purchase_demand_no varchar(100)   not null comment '采购需求单号',
    material_id        bigint         not null comment '采购物料',
    purchase_quantity  decimal(18, 4) not null comment '采购数量',
    source_type        tinyint        not null comment '来源类型',
    source_no          varchar(100)   not null comment '来源单号',
    status             tinyint        not null comment '状态',
    create_time        datetime       not null comment '创建时间',
    update_time        datetime       null comment '更新时间',
    constraint uk_purchase_no
        unique (purchase_demand_no)
)
    comment '采购需求表';

create index idx_material_id
    on `smart-erp`.pur_purchase_demand (material_id);

create index idx_source
    on `smart-erp`.pur_purchase_demand (source_type, source_no);

