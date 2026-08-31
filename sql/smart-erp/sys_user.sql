create table `smart-erp`.sys_user
(
    id          bigint            not null comment '用户id'
        primary key,
    username    varchar(50)       not null comment '用户昵称',
    password    varchar(255)      not null comment '用户密码',
    phone       varchar(20)       null comment '用户手机号',
    status      tinyint default 1 null comment '用户状态',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    dept_id     bigint            null comment '部门id',
    constraint username
        unique (username)
)
    comment '用户信息表';

