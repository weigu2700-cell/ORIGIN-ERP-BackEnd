create table `smart-erp`.md_material_supplier
(
    id                     bigint            not null comment '物料供应关系id'
        primary key,
    material_id            bigint            not null comment '物料id',
    supplier_id            bigint            not null comment '供应商id',
    material_supplier_code varchar(100)      not null comment '物料供应商关系编码',
    purchase_price         decimal(18, 4)    not null default 0.0000 comment '采购价',
    lead_time_days         int               not null default 0 comment '交期天数',
    preferred              tinyint           not null default 0 comment '是否首选供应商',
    min_order_qty          decimal(18, 4)    not null default 0.0000 comment '最小采购量',
    status                 tinyint           not null default 1 comment '状态，1有效，0无效',
    remark                 varchar(255)      null comment '备注',
    create_time            datetime          not null comment '创建时间',
    update_time            datetime          null comment '更新时间',
    deleted                tinyint           not null default 0 comment '删除标记',
    constraint uk_material_supplier_code
        unique (material_supplier_code),
    constraint uk_material_supplier_pair
        unique (material_id, supplier_id)
)
    comment '物料供应商关系';

create index idx_material_supplier_material
    on `smart-erp`.md_material_supplier (material_id);

create index idx_material_supplier_supplier
    on `smart-erp`.md_material_supplier (supplier_id);
