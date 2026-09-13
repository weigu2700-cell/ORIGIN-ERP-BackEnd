create table `smart-erp`.prd_bom
(
    id          bigint            not null comment 'id'
        primary key,
    bom_no      varchar(100)      not null comment 'BOM编码',
    material_id bigint            not null comment '物料id',
    status      tinyint default 0 not null comment 'BOM状态',
    version     tinyint default 0 not null comment '版本',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    constraint uk_bom_no
        unique (bom_no),
    constraint uk_material_version
        unique (material_id, version)
)
    comment 'BOM信息表';

create index idx_material_id
    on `smart-erp`.prd_bom (material_id);

