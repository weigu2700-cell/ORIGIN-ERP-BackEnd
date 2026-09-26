create table `smart-erp`.eip_notification_template
(
    id                bigint            not null
        primary key,
    code              varchar(100)      not null,
    name              varchar(200)      not null,
    title_template    varchar(200)      not null,
    content_template  longtext          not null,
    notification_type tinyint default 0 not null,
    status            tinyint default 1 not null,
    remark            varchar(500)      null,
    create_time       datetime          null,
    update_time       datetime          null,
    constraint uk_notification_template_code
        unique (code)
);

create index idx_notification_template_status
    on `smart-erp`.eip_notification_template (status);

