create table `smart-erp`.md_workshop
(
    id          bigint            not null comment '车间id'
        primary key,
    code        varchar(100)      not null comment '车间编码',
    name        varchar(100)      null comment '车间名称',
    short_name  varchar(50)       null comment '车间简称',
    factory_id  bigint            not null comment '所属工厂id',
    status      tinyint default 1 null comment '车间状态',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    constraint uk_workshop_code
        unique (code)
)
    comment '车间信息表';

create index idx_workshop_factory
    on `smart-erp`.md_workshop (factory_id);

