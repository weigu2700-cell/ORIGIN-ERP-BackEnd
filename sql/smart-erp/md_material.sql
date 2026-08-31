create table `smart-erp`.md_material
(
    id           bigint                        not null comment 'id'
        primary key,
    code         varchar(100)                  not null comment '物料编码',
    name         varchar(100)                  not null comment '物料名称',
    type         tinyint                       not null comment '物料类型',
    spec         varchar(100)                  null comment '规格型号',
    unit         varchar(20)                   not null comment '物料单位',
    safety_stock decimal(18, 4) default 0.0000 not null comment '安全库存',
    status       tinyint        default 1      not null comment '物料状态',
    remark       varchar(255)                  null comment '备注',
    create_time  datetime                      not null comment '创建时间',
    update_time  datetime                      null comment '更新时间',
    deleted      tinyint        default 0      null comment '删除',
    constraint uk_material_code
        unique (code)
)
    comment '物料信息表';

