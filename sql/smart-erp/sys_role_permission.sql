create table `smart-erp`.sys_role_permission
(
    id            bigint not null comment 'id'
        primary key,
    role_id       bigint not null comment '角色id',
    permission_id bigint not null comment '权限id'
)
    comment '角色权限对照表';

