create table `smart-erp`.sys_role
(
    id          bigint            not null comment '角色id'
        primary key,
    name        varchar(20)       not null comment '角色名称',
    code        varchar(50)       not null comment '角色编码',
    sort        tinyint default 0 null comment '排序',
    status      tinyint default 1 null comment '状态',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 not null comment '删除',
    constraint code
        unique (code),
    constraint name
        unique (name)
)
    comment '角色形象表';

