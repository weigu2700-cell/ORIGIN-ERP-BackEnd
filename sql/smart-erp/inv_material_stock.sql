create table `smart-erp`.inv_material_stock
(
    id           bigint         not null comment 'id'
        primary key,
    warehouse_id bigint         not null comment '仓库id',
    material_id  bigint         not null comment '物料id',
    on_hand      decimal(18, 4) not null comment '实际库存',
    reserved     decimal(18, 4) not null comment '预占库存',
    version      int default 0  not null comment '乐观锁版本号',
    create_time  datetime       not null comment '创建时间',
    update_time  datetime       null comment '修改时间',
    constraint uk_material_warehouse_id
        unique (material_id, warehouse_id)
)
    comment '库存信息表';

create index idx_material_id
    on `smart-erp`.inv_material_stock (material_id);

