create table `smart-erp`.eip_notification
(
    id            bigint               not null
        primary key,
    user_id       bigint               not null,
    type          tinyint              not null,
    title         varchar(200)         not null,
    content       longtext             not null,
    business_id   bigint               null,
    business_no   varchar(100)         null,
    business_type varchar(100)         null,
    publish_id    bigint               null,
    is_read       tinyint(1) default 0 not null,
    read_time     datetime             null,
    create_time   datetime             null,
    update_time   datetime             null,
    constraint uk_notification_publish_user
        unique (publish_id, user_id)
);

create index idx_notification_business
    on `smart-erp`.eip_notification (business_type, business_id);

create index idx_notification_inbox
    on `smart-erp`.eip_notification (user_id, is_read, create_time, id);

create index idx_notification_publish_id
    on `smart-erp`.eip_notification (publish_id);

