create table `smart-erp`.sys_permission
(
    id          bigint            not null comment '权限id'
        primary key,
    name        varchar(20)       not null comment '权限名称',
    code        varchar(50)       not null comment '权限编码',
    type        tinyint default 1 not null comment '权限类型',
    parent_id   bigint            null comment '上级权限id',
    status      tinyint default 1 not null comment '权限状态',
    sort        tinyint default 0 null comment '排序',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 not null comment '删除',
    constraint code
        unique (code)
)
    comment '权限信息表';

