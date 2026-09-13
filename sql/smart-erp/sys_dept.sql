create table `smart-erp`.sys_dept
(
    id          bigint            not null comment '部门id'
        primary key,
    name        varchar(50)       not null comment '部门名称',
    code        varchar(50)       not null comment '部门编码',
    parent_id   bigint  default 0 null comment '上级部门编码',
    status      tinyint default 1 not null comment '状态',
    sort        int     default 0 null comment '排序',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 not null comment '删除',
    constraint code
        unique (code),
    constraint name
        unique (name)
)
    comment '部门信息表';

