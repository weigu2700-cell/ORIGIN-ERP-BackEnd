create table `smart-erp`.md_warehouse
(
    id          bigint            not null comment '仓库Id'
        primary key,
    name        varchar(100)      not null comment '仓库名称',
    code        varchar(100)      not null comment '仓库编码',
    type        tinyint           not null comment '仓库类型',
    factory_id  bigint            not null comment '所属工厂id',
    address     varchar(100)      null comment '仓库地址',
    status      tinyint default 1 null comment '仓库状态',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    constraint type
        unique (type),
    constraint uk_warehouse_code
        unique (code)
)
    comment '仓库信息表';

create index idx_warehouse_factory
    on `smart-erp`.md_warehouse (factory_id);

