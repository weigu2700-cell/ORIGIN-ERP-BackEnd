create table `smart-erp`.sal_delivery
(
    id             bigint            not null comment 'id'
        primary key,
    delivery_no    varchar(100)      not null comment '出库单号',
    sales_order_id bigint            not null comment '来源订单id',
    sales_order_no varchar(100)      not null comment '来源订单单号',
    customer_id    bigint            not null comment '客户id',
    status         tinyint default 0 not null comment '出库单状态',
    delivery_date  datetime          not null comment '出库日期',
    remark         varchar(255)      null comment '备注',
    create_time    datetime          not null comment '创建时间',
    update_time    datetime          null comment '更新时间',
    constraint uk_delivery_no
        unique (delivery_no)
)
    comment '销售出库信息表';

create index idx_customer_id
    on `smart-erp`.sal_delivery (customer_id);

create index idx_sales_order_id
    on `smart-erp`.sal_delivery (sales_order_id);

