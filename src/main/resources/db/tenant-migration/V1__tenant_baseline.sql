-- Frozen tenant V1: application DDL from 5118ebc, plus nullable purchase receipt date.
-- No CREATE DATABASE, USE, qualified schema names, seed accounts or history tables.

-- sql/smart-erp/SPRING_AI_CHAT_MEMORY.sql
create table SPRING_AI_CHAT_MEMORY
(
    conversation_id varchar(36)                                  not null,
    content         text                                         not null,
    type            enum ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') not null,
    timestamp       timestamp                                    not null,
    sequence_id     bigint                                       not null
);

create index SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_SEQUENCE_ID_IDX
    on SPRING_AI_CHAT_MEMORY (conversation_id, sequence_id);

create index SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    on SPRING_AI_CHAT_MEMORY (conversation_id, timestamp);


-- sql/smart-erp/ai_conversation.sql
create table ai_conversation
(
    id          bigint                        not null comment '会话id'
        primary key,
    user_id     bigint                        not null comment '用户id',
    title       varchar(128) default '新对话' not null comment '会话标题',
    status      tinyint      default 1        not null comment '会话状态：0未开始 1进行中 2已结束',
    create_time datetime                      not null comment '创建时间',
    update_time datetime                      not null comment '更新时间',
    deleted     tinyint      default 0        not null comment '逻辑删除：0否 1是'
)
    comment 'AI会话表';

create index idx_user_update_time
    on ai_conversation (user_id, deleted, update_time);


-- sql/smart-erp/ai_message.sql
create table ai_message
(
    id              bigint            not null comment '消息id'
        primary key,
    conversation_id bigint            not null comment '会话id',
    role            varchar(32)       not null comment '消息角色：USER/ASSISTANT',
    content         mediumtext        not null comment '消息内容',
    create_time     datetime          not null comment '创建时间',
    update_time     datetime          not null comment '更新时间',
    deleted         tinyint default 0 not null comment '逻辑删除：0否 1是'
)
    comment 'AI会话消息表';

create index idx_conversation_time
    on ai_message (conversation_id, deleted, create_time, id);


-- sql/smart-erp/eip_notification.sql
create table eip_notification
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
    on eip_notification (business_type, business_id);

create index idx_notification_inbox
    on eip_notification (user_id, is_read, create_time, id);

create index idx_notification_publish_id
    on eip_notification (publish_id);


-- sql/smart-erp/eip_notification_publish.sql
create table eip_notification_publish
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
    on eip_notification_publish (business_type, business_id);


-- sql/smart-erp/eip_notification_template.sql
create table eip_notification_template
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
    on eip_notification_template (status);


-- sql/smart-erp/eip_notification_template_recipient.sql
create table eip_notification_template_recipient
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
    on eip_notification_template_recipient (template_id);


-- sql/smart-erp/inv_finish_warehousing.sql
create table inv_finish_warehousing
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
    on inv_finish_warehousing (production_order_id);

create index idx_production_report_id
    on inv_finish_warehousing (production_report_id);

create index idx_warehouse_material_time
    on inv_finish_warehousing (warehouse_id, material_id, warehousing_time);


-- sql/smart-erp/inv_material_stock.sql
create table inv_material_stock
(
    id           bigint         not null comment 'id'
        primary key,
    warehouse_id bigint         not null comment '仓库id',
    material_id  bigint         not null comment '物料id',
    on_hand      decimal(18, 4) not null comment '实际库存',
    reserved     decimal(18, 4) not null comment '预占库存',
    version      int default 0  not null comment '乐观锁版本号',
    create_time  datetime       not null comment '创建时间',
    update_time  datetime       null comment '修改时间',
    constraint uk_material_warehouse_id
        unique (material_id, warehouse_id)
)
    comment '库存信息表';

create index idx_material_id
    on inv_material_stock (material_id);


-- sql/smart-erp/inv_transaction.sql
create table inv_transaction
(
    id               bigint         not null comment 'id'
        primary key,
    warehouse_id     bigint         not null comment '仓库id',
    material_id      bigint         not null comment '物料id',
    transaction_type tinyint        not null comment '库存业务类型',
    business_type    varchar(64)    not null comment '来源业务类型',
    business_no      varchar(100)   not null comment '来源业务单号',
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
    on inv_transaction (business_type, business_no);

create index idx_material_id
    on inv_transaction (material_id);

create index idx_warehouse_id
    on inv_transaction (warehouse_id);


-- sql/smart-erp/md_customer.sql
create table md_customer
(
    id           bigint            not null comment '客户id'
        primary key,
    code         varchar(50)       not null comment '用户编码',
    name         varchar(100)      not null comment '客户名称',
    short_name   varchar(50)       null comment '客户简称',
    contact_name varchar(50)       not null comment '联系人',
    phone        varchar(15)       not null comment '联系电话',
    email        varchar(30)       null comment '用户邮箱',
    address      varchar(255)      null comment '用户地址',
    status       tinyint default 1 null comment '用户状态',
    remark       varchar(255)      null comment '备注',
    create_time  datetime          not null comment '创建时间',
    update_time  datetime          null comment '更新时间',
    deleted      tinyint default 0 null comment '删除',
    constraint code
        unique (code),
    constraint phone
        unique (phone),
    constraint uk_customer_code
        unique (code)
)
    comment '客户信息表';


-- sql/smart-erp/md_factory.sql
create table md_factory
(
    id          bigint            not null comment 'id'
        primary key,
    name        varchar(100)      not null comment '工厂名称',
    short_name  varchar(50)       null comment '工厂简称',
    code        varchar(100)      not null comment '工厂编码',
    address     varchar(100)      null comment '工厂地址',
    status      tinyint default 1 null comment '工厂状态',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    constraint uk_factory_code
        unique (code)
)
    comment '工厂信息表';


-- sql/smart-erp/md_material.sql
create table md_material
(
    id           bigint                        not null comment 'id'
        primary key,
    code         varchar(100)                  not null comment '物料编码',
    name         varchar(100)                  not null comment '物料名称',
    type         tinyint                       not null comment '物料类型',
    spec         varchar(100)                  null comment '规格型号',
    unit         varchar(20)                   not null comment '物料单位',
    safety_stock decimal(18, 4) default 0.0000 not null comment '安全库存',
    status       tinyint        default 1      not null comment '物料状态',
    remark       varchar(255)                  null comment '备注',
    create_time  datetime                      not null comment '创建时间',
    update_time  datetime                      null comment '更新时间',
    deleted      tinyint        default 0      null comment '删除',
    constraint uk_material_code
        unique (code)
)
    comment '物料信息表';


-- sql/smart-erp/md_material_supplier.sql
create table md_material_supplier
(
    id                     bigint                        not null
        primary key,
    material_id            bigint                        not null,
    supplier_id            bigint                        not null,
    material_supplier_code varchar(100)                  not null,
    purchase_price         decimal(18, 4) default 0.0000 not null,
    lead_time_days         int            default 0      not null,
    preferred              tinyint        default 0      not null,
    min_order_qty          decimal(18, 4) default 0.0000 not null,
    status                 tinyint        default 1      not null,
    remark                 varchar(255)                  null,
    create_time            datetime                      not null,
    update_time            datetime                      null,
    deleted                tinyint        default 0      not null,
    constraint uk_material_supplier_code
        unique (material_supplier_code),
    constraint uk_material_supplier_pair
        unique (material_id, supplier_id)
)
    comment '物料供应商关系';


-- sql/smart-erp/md_production_line.sql
create table md_production_line
(
    id               bigint            not null comment 'id'
        primary key,
    name             varchar(100)      not null comment '产线名称',
    code             varchar(100)      not null comment '产线编码',
    workshop_id      bigint            not null comment '车间id',
    capacity_per_day decimal(18, 2)    not null comment '日产能',
    remark           varchar(255)      null comment '备注',
    status           tinyint default 1 null comment '状态',
    create_time      datetime          not null comment '创建日期',
    update_time      datetime          null comment '修改日期',
    deleted          tinyint default 0 null comment '删除',
    constraint uk_production_line_code
        unique (code)
)
    comment '产线信息表';

create index idx_production_line_workshop
    on md_production_line (workshop_id);


-- sql/smart-erp/md_supplier.sql
create table md_supplier
(
    id           bigint            not null comment '供应商id'
        primary key,
    code         varchar(50)       not null comment '供应商编码',
    name         varchar(100)      not null comment '供应商名称',
    short_name   varchar(50)       null comment '供应商简称',
    contact_name varchar(50)       null comment '联系人',
    phone        varchar(15)       not null comment '联系电话',
    email        varchar(100)      null comment '供应商邮箱',
    address      varchar(255)      null comment '供应商地址',
    status       tinyint default 1 null comment '供应商状态',
    remark       varchar(255)      null comment '备注',
    create_time  datetime          not null comment '创建时间',
    update_time  datetime          null comment '更新时间',
    deleted      tinyint default 0 null comment '删除',
    constraint uk_supplier_unit
        unique (code)
)
    comment '供应商信息表';


-- sql/smart-erp/md_warehouse.sql
create table md_warehouse
(
    id          bigint            not null comment '仓库Id'
        primary key,
    name        varchar(100)      not null comment '仓库名称',
    code        varchar(100)      not null comment '仓库编码',
    type        tinyint           not null comment '仓库类型',
    factory_id  bigint            not null comment '所属工厂id',
    address     varchar(100)      null comment '仓库地址',
    status      tinyint default 1 null comment '仓库状态',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    constraint uk_warehouse_code
        unique (code)
)
    comment '仓库信息表';

create index idx_warehouse_factory
    on md_warehouse (factory_id);


-- sql/smart-erp/md_workshop.sql
create table md_workshop
(
    id          bigint            not null comment '车间id'
        primary key,
    code        varchar(100)      not null comment '车间编码',
    name        varchar(100)      null comment '车间名称',
    short_name  varchar(50)       null comment '车间简称',
    factory_id  bigint            not null comment '所属工厂id',
    status      tinyint default 1 null comment '车间状态',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    constraint uk_workshop_code
        unique (code)
)
    comment '车间信息表';

create index idx_workshop_factory
    on md_workshop (factory_id);


-- sql/smart-erp/prd_bom.sql
create table prd_bom
(
    id          bigint            not null comment 'id'
        primary key,
    bom_no      varchar(100)      not null comment 'BOM编码',
    material_id bigint            not null comment '物料id',
    status      tinyint default 0 not null comment 'BOM状态',
    version     tinyint default 0 not null comment '版本',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    constraint uk_bom_no
        unique (bom_no),
    constraint uk_material_version
        unique (material_id, version)
)
    comment 'BOM信息表';

create index idx_material_id
    on prd_bom (material_id);


-- sql/smart-erp/prd_bom_item.sql
create table prd_bom_item
(
    id                    bigint                        not null comment 'id'
        primary key,
    bom_id                bigint                        not null comment 'BOMId',
    line_no               int            default 10     not null comment '行号',
    component_material_id bigint                        not null comment '组成物料id',
    quantity              decimal(18, 4) default 0.0000 not null comment '物料数量',
    loss_rate             decimal(8, 4)  default 0.0000 not null comment '损坏率',
    remark                varchar(255)                  null comment '备注',
    constraint uk_bom_line_no
        unique (bom_id, line_no)
)
    comment 'BOM明细表';

create index idx_bom_id
    on prd_bom_item (bom_id);

create index idx_component_material_id
    on prd_bom_item (component_material_id);


-- sql/smart-erp/prd_production_demand.sql
create table prd_production_demand
(
    id              bigint            not null comment 'id'
        primary key,
    demand_no       varchar(100)      not null comment '需求单号',
    material_id     bigint            not null comment '所需物料id',
    warehouse_id    bigint            null comment '目标入库仓库id',
    demand_quantity decimal(18, 4)    not null comment '所需数量',
    source_type     tinyint           not null comment '来源类型',
    source_no       varchar(100)      not null comment '来源单号',
    status          tinyint default 0 not null comment '状态',
    create_time     datetime          not null comment '创建时间',
    update_time     datetime          null comment '更新时间',
    quantity        decimal(18, 4)    null comment '需求数量',
    constraint uk_demand_no
        unique (demand_no)
)
    comment '生产订单信息表';

create index idx_material_id
    on prd_production_demand (material_id);

create index idx_source
    on prd_production_demand (source_type, source_no);

create index idx_source_material_warehouse
    on prd_production_demand (source_type, source_no, material_id, warehouse_id);


-- sql/smart-erp/prd_production_order.sql
create table prd_production_order
(
    id                   bigint                        not null comment 'id'
        primary key,
    production_order_no  varchar(100)                  not null comment '生产订单编号',
    production_demand_id bigint                        null comment '生产需求id',
    material_id          bigint                        not null comment '生产物料id',
    warehouse_id         bigint                        null comment '目标入库仓库id',
    planned_quantity     decimal(18, 4)                not null comment '计划生产数量',
    completed_quantity   decimal(18, 4) default 0.0000 not null comment '已完成数量',
    status               tinyint        default 0      not null comment '订单状态',
    planned_start_time   datetime                      not null comment '计划开始时间',
    planned_end_time     datetime                      not null comment '计划完成时间',
    actual_start_time    datetime                      null comment '实际开始时间',
    actual_end_time      datetime                      null comment '实际完成时间',
    remark               varchar(255)                  null comment '备注',
    create_time          datetime                      not null comment '创建时间',
    update_time          datetime                      null comment '更新时间',
    constraint uk_production_order_no
        unique (production_order_no)
)
    comment '生产订单表';

create index idx_material_id
    on prd_production_order (material_id);

create index idx_production_demand_id
    on prd_production_order (production_demand_id);

create index idx_production_status
    on prd_production_order (status);


-- sql/smart-erp/prd_production_picking.sql
create table prd_production_picking
(
    id                  bigint                        not null comment 'id'
        primary key,
    picking_no          varchar(100)                  not null comment '领料单号',
    production_order_id bigint                        not null comment '生产订单id',
    material_id         bigint                        not null comment '物料id',
    warehouse_id        bigint                        null comment '仓库id；缺料草稿待采购上架后回填，人工新增和确认领料前仍需指定',
    planned_quantity    decimal(18, 4) default 0.0000 not null comment '计划领料数量',
    actual_quantity     decimal(18, 4) default 0.0000 not null comment '实际领料数量',
    status              tinyint        default 0      not null comment '领料单状态',
    picking_time        datetime                      null comment '领料时间',
    create_time         datetime                      not null comment '创建时间',
    update_time         datetime                      null comment '修改时间',
    purchase_demand_id  bigint                        null comment '采购需求id',
    constraint uk_picking_no
        unique (picking_no)
)
    comment '生产领料表';

create index idx_material_id
    on prd_production_picking (material_id);

create index idx_production_order_id
    on prd_production_picking (production_order_id);

create index idx_warehouse_id
    on prd_production_picking (warehouse_id);


-- sql/smart-erp/prd_production_report.sql
create table prd_production_report
(
    id                   bigint                        not null comment 'id'
        primary key,
    production_report_no varchar(100)                  not null comment '报功单号',
    production_order_id  bigint                        not null comment '生产订单id',
    report_quantity      decimal(18, 4)                not null comment '本次报工数量',
    qualified_quantity   decimal(18, 4) default 0.0000 not null comment '合格数量',
    scrapped_quantity    decimal(18, 4) default 0.0000 not null comment '报废数量',
    status               tinyint        default 0      not null comment '报工状态 0-草稿，1-已审批，2-已完成',
    report_time          datetime                      not null comment '报工时间',
    report_user_id       bigint                        not null comment '报工人',
    create_time          datetime                      not null comment '创建时间',
    update_time          datetime                      null comment '更新时间',
    material_id          bigint                        not null comment '物料id',
    remark               varchar(255)                  null comment '备注',
    version              int            default 0      not null comment '乐观锁版本号',
    warehouse_id         bigint                        null comment '成品入库仓库(报工指定)',
    constraint uk_production_report_no
        unique (production_report_no)
)
    comment '生产报功单';

create index idx_production_order_id
    on prd_production_report (production_order_id);

create index idx_report_user_id
    on prd_production_report (report_user_id);


-- sql/smart-erp/pur_in_stock.sql
create table pur_in_stock
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
    in_date           datetime          null comment '实际入库日期（上架后填写）',
    create_time       datetime          not null comment '创建时间',
    update_time       datetime          null comment '更新时间',
    constraint uk_in_stock_no
        unique (in_stock_no)
)
    comment '采购入库单表';

create index idx_batch
    on pur_in_stock (batch_no);

create index idx_in_date
    on pur_in_stock (in_date);

create index idx_material
    on pur_in_stock (material_id);

create index idx_purchase_order
    on pur_in_stock (purchase_order_id);

create index idx_purchase_order_no
    on pur_in_stock (purchase_order_no);

create index idx_status
    on pur_in_stock (status);

create index idx_warehouse
    on pur_in_stock (warehouse_id);


-- sql/smart-erp/pur_purchase_demand.sql
create table pur_purchase_demand
(
    id                 bigint         not null comment 'id'
        primary key,
    purchase_demand_no varchar(100)   not null comment '采购需求单号',
    material_id        bigint         not null comment '采购物料',
    purchase_quantity  decimal(18, 4) not null comment '采购数量',
    source_type        tinyint        not null comment '来源类型',
    source_no          varchar(100)   not null comment '来源单号',
    status             tinyint        not null comment '状态',
    create_time        datetime       not null comment '创建时间',
    update_time        datetime       null comment '更新时间',
    constraint uk_purchase_no
        unique (purchase_demand_no)
)
    comment '采购需求表';

create index idx_material_id
    on pur_purchase_demand (material_id);

create index idx_source
    on pur_purchase_demand (source_type, source_no);


-- sql/smart-erp/pur_purchase_order.sql
create table pur_purchase_order
(
    id                     bigint                        not null comment '订单ID'
        primary key,
    purchase_order_no      varchar(100)                  not null comment '采购订单号',
    purchase_demand_id     bigint                        not null comment '关联的采购需求ID',
    material_id            bigint                        not null comment '采购物料ID',
    supplier_id            bigint                        null comment '供应商ID（若需要）',
    planned_quantity       decimal(18, 4)                not null comment '计划采购数量',
    complete_quantity      decimal(18, 4) default 0.0000 null comment '已到货/已采购数量',
    unit_price             decimal(18, 4)                null comment '采购单价（含税或不含税，可约定）',
    total_amount           decimal(18, 4)                null comment '订单总金额（= planned_quantity * unit_price，可冗余）',
    order_date             datetime                      not null comment '下单日期',
    expected_delivery_date datetime                      null comment '期望交货日期',
    actual_delivery_date   datetime                      null comment '实际交货日期',
    status                 tinyint                       not null comment '订单状态（0-草稿，1-已审批，2-已发货，3-已收货，4-已关闭等）',
    create_time            datetime                      not null comment '创建时间',
    update_time            datetime                      null comment '更新时间',
    version                int            default 0      not null comment '版本号',
    constraint uk_order_no
        unique (purchase_order_no)
)
    comment '采购订单表';

create index idx_demand_id
    on pur_purchase_order (purchase_demand_id);

create index idx_material
    on pur_purchase_order (material_id);

create index idx_order_date
    on pur_purchase_order (order_date);

create index idx_status
    on pur_purchase_order (status);

create index idx_supplier
    on pur_purchase_order (supplier_id);


-- sql/smart-erp/sal_delivery.sql
create table sal_delivery
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
    version        int     default 0 not null comment '乐观锁版本号',
    create_time    datetime          not null comment '创建时间',
    update_time    datetime          null comment '更新时间',
    constraint uk_delivery_no
        unique (delivery_no)
)
    comment '销售出库信息表';

create index idx_customer_id
    on sal_delivery (customer_id);

create index idx_sales_order_id
    on sal_delivery (sales_order_id);


-- sql/smart-erp/sal_delivery_item.sql
create table sal_delivery_item
(
    id                  bigint                        not null comment 'id'
        primary key,
    delivery_id         bigint                        not null comment '表头id',
    line_no             varchar(100)                  not null comment '行号',
    sales_order_item_id bigint                        not null comment '销售明细id',
    material_id         bigint                        not null comment '物料id',
    warehouse_id        bigint                        not null comment '仓库id',
    quantity            decimal(18, 4)                not null comment '出库数量',
    create_time         datetime                      not null comment '创建时间',
    update_time         datetime                      null comment '更新时间',
    reserved_quantity   decimal(18, 4) default 0.0000 not null comment '本次确认时已预占数量'
)
    comment '销售出库明细表';

create index idx_delivery_id
    on sal_delivery_item (delivery_id);

create index idx_material_warehouse
    on sal_delivery_item (material_id, warehouse_id);

create index idx_sales_order_item_id
    on sal_delivery_item (sales_order_item_id);


-- sql/smart-erp/sal_order.sql
create table sal_order
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
    version       int            default 0    not null comment '乐观锁版本号',
    create_time   datetime                    not null comment '创建时间',
    update_time   datetime                    null comment '更新时间',
    deleted       tinyint        default 0    not null comment '删除',
    constraint uk_order_no
        unique (order_no)
)
    comment '销售订单表';

create index idx_customer_id
    on sal_order (customer_id);

create index idx_order_date
    on sal_order (order_date);


-- sql/smart-erp/sal_order_item.sql
create table sal_order_item
(
    id                 bigint                        not null comment 'id'
        primary key,
    sales_order_id     bigint                        not null comment '销售订单id',
    line_no            int                           not null comment '行号',
    material_id        bigint                        not null comment '物料id',
    quantity           decimal(18, 4)                not null comment '销售数量',
    unit_price         decimal(18, 4)                not null comment '单价',
    amount             decimal(18, 4)                not null comment '总价',
    warehouse_id       bigint                        not null comment '仓库id',
    delivery_date      datetime                      null comment '要求交货日期',
    create_time        datetime                      not null comment '创建时间',
    remark             varchar(255)                  null comment '备注',
    delivered_quantity decimal(18, 4) default 0.0000 not null comment '已发货数量（由销售出库回写）'
)
    comment '销售订单明细表';

create index idx_material_id
    on sal_order_item (material_id);

create index idx_sales_order_id
    on sal_order_item (sales_order_id);

create index idx_sales_order_line
    on sal_order_item (sales_order_id, line_no);

create index idx_warehouse_id
    on sal_order_item (warehouse_id);


-- sql/smart-erp/sys_dept.sql
create table sys_dept
(
    id          bigint            not null comment '部门id'
        primary key,
    name        varchar(50)       not null comment '部门名称',
    code        varchar(50)       not null comment '部门编码',
    parent_id   bigint  default 0 null comment '上级部门编码',
    status      tinyint default 1 not null comment '状态',
    sort        int     default 0 null comment '排序',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 not null comment '删除',
    constraint code
        unique (code),
    constraint name
        unique (name)
)
    comment '部门信息表';


-- sql/smart-erp/sys_menu.sql
create table sys_menu
(
    id              bigint            not null comment '菜单id'
        primary key,
    name            varchar(100)      not null comment '菜单名称',
    path            varchar(100)      not null comment '菜单路径',
    component       varchar(200)      not null comment '组件名称',
    icon            varchar(100)      null comment '菜单图标',
    visible         tinyint default 0 null comment '是否隐藏',
    parent_id       bigint            null comment '上级菜单id',
    status          tinyint default 1 null comment '菜单状态',
    create_time     datetime          not null comment '创建时间',
    update_time     datetime          null comment '修改时间',
    deleted         tinyint default 0 null comment '删除',
    title           varchar(100)      not null comment '菜单标题',
    permission_code varchar(100)      null comment '权限编码',
    constraint name
        unique (name),
    constraint path
        unique (path)
)
    comment '菜单表';


-- sql/smart-erp/sys_permission.sql
create table sys_permission
(
    id          bigint auto_increment comment '权限id'
        primary key,
    name        varchar(20)       not null comment '权限名称',
    code        varchar(50)       not null comment '权限编码',
    type        tinyint default 1 not null comment '权限类型',
    parent_id   bigint            null comment '上级权限id',
    status      tinyint default 1 not null comment '权限状态',
    sort        tinyint default 0 null comment '排序',
    remark      varchar(255)      null comment '备注',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 not null comment '删除',
    constraint code
        unique (code)
)
    comment '权限信息表';


-- sql/smart-erp/sys_role.sql
create table sys_role
(
    id          bigint            not null comment '角色id'
        primary key,
    name        varchar(20)       not null comment '角色名称',
    code        varchar(50)       not null comment '角色编码',
    sort        tinyint default 0 null comment '排序',
    status      tinyint default 1 null comment '状态',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 not null comment '删除',
    constraint code
        unique (code),
    constraint name
        unique (name)
)
    comment '角色形象表';


-- sql/smart-erp/sys_role_menu.sql
create table sys_role_menu
(
    id      bigint not null comment 'id'
        primary key,
    role_id bigint not null comment '角色id',
    menu_id bigint not null comment '菜单id'
);


-- sql/smart-erp/sys_role_permission.sql
create table sys_role_permission
(
    id            bigint not null comment 'id'
        primary key,
    role_id       bigint not null comment '角色id',
    permission_id bigint not null comment '权限id'
)
    comment '角色权限对照表';


-- sql/smart-erp/sys_user.sql
create table sys_user
(
    id          bigint            not null comment '用户id'
        primary key,
    username    varchar(50)       not null comment '用户昵称',
    password    varchar(255)      not null comment '用户密码',
    phone       varchar(20)       null comment '用户手机号',
    status      tinyint default 1 null comment '用户状态',
    create_time datetime          not null comment '创建时间',
    update_time datetime          null comment '修改时间',
    deleted     tinyint default 0 null comment '删除',
    dept_id     bigint            null comment '部门id',
    real_name   varchar(50)       null comment '用户真实姓名',
    constraint username
        unique (username)
)
    comment '用户信息表';


-- sql/smart-erp/sys_user_role.sql
create table sys_user_role
(
    id      bigint not null comment 'id'
        primary key,
    user_id bigint not null comment '用户id',
    role_id bigint not null comment '角色id'
);

