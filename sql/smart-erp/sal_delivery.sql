create table `smart-erp`.sal_delivery
(
    id             bigint         not null comment '出库单id'
        primary key,
    delivery_no    varchar(100)   not null comment '出库单号',
    sales_order_id bigint         not null comment '销售订单id',
    sales_order_no varchar(100)   not null comment '销售订单号',
    customer_id    bigint         not null comment '客户id',
    delivery_date  datetime       null comment '出库日期',
    status         tinyint        not null default 0 comment '出库状态：0草稿，1已确认，2已取消，3已完成',
    remark         varchar(255)   null comment '备注',
    create_time    datetime       not null comment '创建时间',
    update_time    datetime       null comment '更新时间',
    constraint uk_delivery_no
        unique (delivery_no)
)
    comment '销售出库单';

create index idx_delivery_order
    on `smart-erp`.sal_delivery (sales_order_id);

create index idx_delivery_customer
    on `smart-erp`.sal_delivery (customer_id);
