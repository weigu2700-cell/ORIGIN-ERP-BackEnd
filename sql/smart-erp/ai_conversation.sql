create table `smart-erp`.ai_conversation
(
    id          bigint                        not null comment '会话id'
        primary key,
    user_id     bigint                        not null comment '用户id',
    title       varchar(128) default '新对话' not null comment '会话标题',
    status      tinyint      default 1        not null comment '会话状态：0未开始 1进行中 2已结束',
    create_time datetime                      not null comment '创建时间',
    update_time datetime                      not null comment '更新时间',
    deleted     tinyint      default 0        not null comment '逻辑删除：0否 1是'
)
    comment 'AI会话表';

create index idx_user_update_time
    on `smart-erp`.ai_conversation (user_id, deleted, update_time);

