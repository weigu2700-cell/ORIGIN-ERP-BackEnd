create table `smart-erp`.md_material_supplier
(
    id                     bigint                        not null
        primary key,
    material_id            bigint                        not null,
    supplier_id            bigint                        not null,
    material_supplier_code varchar(100)                  not null,
    purchase_price         decimal(18, 4) default 0.0000 not null,
    lead_time_days         int            default 0      not null,
    preferred              tinyint        default 0      not null,
    min_order_qty          decimal(18, 4) default 0.0000 not null,
    status                 tinyint        default 1      not null,
    remark                 varchar(255)                  null,
    create_time            datetime                      not null,
    update_time            datetime                      null,
    deleted                tinyint        default 0      not null,
    constraint uk_material_supplier_code
        unique (material_supplier_code),
    constraint uk_material_supplier_pair
        unique (material_id, supplier_id)
)
    comment '物料供应商关系';

