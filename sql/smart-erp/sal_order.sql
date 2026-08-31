create table `smart-erp`.sal_order
(
    id            bigint                      not null comment 'id'
        primary key,
    order_no      varchar(100)                not null comment '订单号',
    customer_id   bigint                      not null comment '客户id',
    status        tinyint        default 0    not null comment '订单状态',
    order_date    datetime                    not null comment '下单日期',
    delivery_date datetime                    null comment '要求交货日期',
    total_amount  decimal(18, 2) default 0.00 not null comment '总金额',
    remark        varchar(255)                null comment '备注',
    create_time   datetime                    not null comment '创建时间',
    update_time   datetime                    null comment '更新时间',
    constraint uk_order_no
        unique (order_no)
)
    comment '销售订单表';

create index idx_customer_id
    on `smart-erp`.sal_order (customer_id);

create index idx_order_date
    on `smart-erp`.sal_order (order_date);

