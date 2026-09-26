create table `smart-erp`.eip_notification_template_recipient
(
    id               bigint               not null
        primary key,
    template_id      bigint               not null,
    selector_type    varchar(32)          not null,
    selector_value   varchar(200)         not null,
    include_children tinyint(1) default 0 not null,
    create_time      datetime             null,
    constraint uk_notification_template_recipient
        unique (template_id, selector_type, selector_value)
);

create index idx_notification_template_recipient_template
    on `smart-erp`.eip_notification_template_recipient (template_id);

