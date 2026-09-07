create table `smart-erp`.md_factory
(
    id          bigint            not null comment 'id'
        primary key,
    name        varchar(100)      not null comment '工厂名称',
    short_name  varchar(50)       null comment '工厂简称',
    code        varchar(100)      not null comment '工厂编码',
    address     varchar(100)      null comment '工厂地址',
    status      tinyint default 1 null comment '工厂状态',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    constraint uk_factory_code
        unique (code)
)
    comment '工厂信息表';

