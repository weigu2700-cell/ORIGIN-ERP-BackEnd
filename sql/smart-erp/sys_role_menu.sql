create table `smart-erp`.sys_role_menu
(
    id      bigint not null comment 'id'
        primary key,
    role_id bigint not null comment '角色id',
    menu_id bigint not null comment '菜单id'
);

