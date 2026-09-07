create table `smart-erp`.md_production_line
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
    on `smart-erp`.md_production_line (workshop_id);

