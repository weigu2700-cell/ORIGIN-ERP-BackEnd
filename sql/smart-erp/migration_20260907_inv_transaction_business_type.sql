ALTER TABLE inv_transaction
    MODIFY COLUMN business_type varchar(50) NOT NULL COMMENT '来源业务类型',
    MODIFY COLUMN business_no   varchar(50) NOT NULL COMMENT '来源业务单号';
