create table `smart-erp`.pur_in_stock
(
    id                bigint            not null comment '入库单ID'
        primary key,
    in_stock_no       varchar(100)      not null comment '入库单号（唯一）',
    purchase_order_id bigint            not null comment '关联的采购订单ID',
    purchase_order_no varchar(100)      not null comment '关联的采购订单号（冗余字段，便于查询）',
    material_id       bigint            not null comment '物料ID',
    in_quantity       decimal(18, 4)    not null comment '本次入库数量',
    unit_price        decimal(18, 4)    null comment '入库单价（取自订单或实时）',
    total_amount      decimal(18, 4)    null comment '本次入库总金额（= in_quantity * unit_price）',
    warehouse_id      bigint            null comment '仓库ID（关联仓库主表）',
    storage_location  varchar(100)      null comment '库位/货架位置',
    batch_no          varchar(100)      null comment '批次号（若启用批次管理）',
    production_date   datetime          null comment '生产日期',
    expiry_date       datetime          null comment '有效期/失效日期',
    in_type           tinyint default 1 not null comment '入库类型（1-正常采购入库，2-退货入库，3-赠品入库等）',
    status            tinyint default 0 not null comment '状态（0-草稿，1-已审核，2-已上架）',
    remark            varchar(500)      null comment '备注',
    operator          varchar(100)      null comment '操作人',
    in_date           datetime          not null comment '实际入库日期',
    create_time       datetime          not null comment '创建时间',
    update_time       datetime          null comment '更新时间',
    constraint uk_in_stock_no
        unique (in_stock_no)
)
    comment '采购入库单表';

create index idx_batch
    on `smart-erp`.pur_in_stock (batch_no);

create index idx_in_date
    on `smart-erp`.pur_in_stock (in_date);

create index idx_material
    on `smart-erp`.pur_in_stock (material_id);

create index idx_purchase_order
    on `smart-erp`.pur_in_stock (purchase_order_id);

create index idx_purchase_order_no
    on `smart-erp`.pur_in_stock (purchase_order_no);

create index idx_status
    on `smart-erp`.pur_in_stock (status);

create index idx_warehouse
    on `smart-erp`.pur_in_stock (warehouse_id);

