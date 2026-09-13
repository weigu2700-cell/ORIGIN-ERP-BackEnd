create table `smart-erp`.inv_finish_warehousing
(
    id                   bigint            not null comment 'id'
        primary key,
    warehousing_no       varchar(100)      not null comment '入库单号',
    production_order_id  bigint            not null comment '生产订单id',
    production_report_id bigint            not null comment '生产报工id',
    material_id          bigint            not null comment '入库物料id',
    warehouse_id         bigint            not null comment '仓库id',
    warehousing_quantity decimal(18, 4)    not null comment '入库数量',
    warehousing_user_id  bigint            not null comment '入库人',
    warehousing_time     datetime          null comment '入库时间',
    status               tinyint default 0 not null comment '入库单状态：0草稿，1已审批，2已入库，3已取消',
    remark               varchar(255)      null comment '备注',
    version              int     default 0 not null comment '乐观锁版本号',
    create_time          datetime          not null comment '创建时间',
    update_time          datetime          null comment '更新时间',
    constraint uk_warehousing_no
        unique (warehousing_no)
)
    comment '成品入库表';

create index idx_production_order_id
    on `smart-erp`.inv_finish_warehousing (production_order_id);

create index idx_production_report_id
    on `smart-erp`.inv_finish_warehousing (production_report_id);

create index idx_warehouse_material_time
    on `smart-erp`.inv_finish_warehousing (warehouse_id, material_id, warehousing_time);

