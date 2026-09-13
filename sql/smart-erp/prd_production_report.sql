create table `smart-erp`.prd_production_report
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
    constraint uk_production_report_no
        unique (production_report_no)
)
    comment '生产报功单';

create index idx_production_order_id
    on `smart-erp`.prd_production_report (production_order_id);

create index idx_report_user_id
    on `smart-erp`.prd_production_report (report_user_id);

