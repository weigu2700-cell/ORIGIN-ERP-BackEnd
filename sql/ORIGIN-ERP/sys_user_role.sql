create table `smart-erp`.sys_user_role
(
    id      bigint not null comment 'id'
        primary key,
    user_id bigint not null comment '用户id',
    role_id bigint not null comment '角色id'
);

