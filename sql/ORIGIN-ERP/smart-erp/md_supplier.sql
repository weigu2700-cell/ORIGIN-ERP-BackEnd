create table `smart-erp`.md_supplier
(
    id           bigint            not null comment '供应商id'
        primary key,
    code         varchar(50)       not null comment '供应商编码',
    name         varchar(100)      not null comment '供应商名称',
    short_name   varchar(50)       null comment '供应商简称',
    contact_name varchar(50)       null comment '联系人',
    phone        varchar(15)       not null comment '联系电话',
    email        varchar(100)      null comment '供应商邮箱',
    address      varchar(255)      null comment '供应商地址',
    status       tinyint default 1 null comment '供应商状态',
    remark       varchar(255)      null comment '备注',
    create_time  datetime          not null comment '创建时间',
    update_time  datetime          null comment '更新时间',
    deleted      tinyint default 0 null comment '删除',
    constraint uk_supplier_unit
        unique (code)
)
    comment '供应商信息表';

