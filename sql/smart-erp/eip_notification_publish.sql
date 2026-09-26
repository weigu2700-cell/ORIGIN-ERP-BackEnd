create table `smart-erp`.eip_notification_publish
(
    id              bigint        not null
        primary key,
    request_id      varchar(100)  not null,
    source_type     varchar(20)   not null,
    type            tinyint       not null,
    title           varchar(200)  not null,
    content         longtext      not null,
    business_type   varchar(100)  null,
    business_id     bigint        null,
    business_no     varchar(100)  null,
    recipient_count int default 0 not null,
    create_time     datetime      null,
    update_time     datetime      null,
    constraint uk_notification_publish_request
        unique (request_id)
);

create index idx_notification_publish_business
    on `smart-erp`.eip_notification_publish (business_type, business_id);

