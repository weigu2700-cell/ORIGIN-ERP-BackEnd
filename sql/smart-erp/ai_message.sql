create table `smart-erp`.ai_message
(
    id              bigint            not null comment '消息id'
        primary key,
    conversation_id bigint            not null comment '会话id',
    role            varchar(32)       not null comment '消息角色：USER/ASSISTANT',
    content         mediumtext        not null comment '消息内容',
    create_time     datetime          not null comment '创建时间',
    update_time     datetime          not null comment '更新时间',
    deleted         tinyint default 0 not null comment '逻辑删除：0否 1是'
)
    comment 'AI会话消息表';

create index idx_conversation_time
    on `smart-erp`.ai_message (conversation_id, deleted, create_time, id);

