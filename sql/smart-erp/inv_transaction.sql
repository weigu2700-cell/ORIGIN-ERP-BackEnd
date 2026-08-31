create table `smart-erp`.inv_transaction
(
    id               bigint         not null comment 'id'
        primary key,
    warehouse_id     bigint         not null comment '仓库id',
    material_id      bigint         not null comment '物料id',
    transaction_type tinyint        not null comment '库存业务类型',
    business_type    tinyint        not null comment '来源业务类型',
    business_no      tinyint        not null comment '来源业务单号',
    quantity         decimal(18, 4) not null comment '本次数量变化',
    before_on_hand   decimal(18, 4) not null comment '变动前在库库存',
    after_on_hand    decimal(18, 4) not null comment '变动后在库库存',
    before_reserved  decimal(18, 4) not null comment '变动前预占库存',
    after_reserved   decimal(18, 4) not null comment '变动后预占库存',
    remark           varchar(255)   null comment '备注',
    create_time      datetime       not null comment '操作时间'
)
    comment '库存流水表';

create index idx_business_type_no
    on `smart-erp`.inv_transaction (business_type, business_no);

create index idx_material_id
    on `smart-erp`.inv_transaction (material_id);

create index idx_warehouse_id
    on `smart-erp`.inv_transaction (warehouse_id);

