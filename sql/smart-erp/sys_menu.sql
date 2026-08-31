create table `smart-erp`.sys_menu
(
    id              bigint            not null comment '菜单id'
        primary key,
    name            varchar(100)      not null comment '菜单名称',
    path            varchar(100)      not null comment '菜单路径',
    component       varchar(200)      not null comment '组件名称',
    icon            varchar(100)      null comment '菜单图标',
    visible         tinyint default 0 null comment '是否隐藏',
    parent_id       bigint            null comment '上级菜单id',
    status          tinyint default 1 null comment '菜单状态',
    create_time     datetime          not null comment '创建时间',
    update_time     datetime          null comment '修改时间',
    deleted         tinyint default 0 null comment '删除',
    title           varchar(100)      not null comment '菜单标题',
    permission_code varchar(100)      null comment '权限编码',
    constraint name
        unique (name),
    constraint path
        unique (path)
)
    comment '菜单表';

