create table `smart-erp`.prd_bom_item
(
    id                    bigint                        not null comment 'id'
        primary key,
    bom_id                bigint                        not null comment 'BOMId',
    line_no               int            default 10     not null comment '行号',
    component_material_id bigint                        not null comment '组成物料id',
    quantity              decimal(18, 4) default 0.0000 not null comment '物料数量',
    loss_rate             decimal(8, 4)  default 0.0000 not null comment '损坏率',
    remark                varchar(255)                  null comment '备注',
    constraint uk_bom_line_no
        unique (bom_id, line_no)
)
    comment 'BOM明细表';

create index idx_bom_id
    on `smart-erp`.prd_bom_item (bom_id);

create index idx_component_material_id
    on `smart-erp`.prd_bom_item (component_material_id);

