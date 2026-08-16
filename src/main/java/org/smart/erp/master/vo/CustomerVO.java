package org.smart.erp.master.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerVO {

    private Long id;

    private String name;

    private String code;

    private String shortName;

    private String contactName;

    private String address;

    private String contact;

    private String phone;

    private String email;

    private String remark;

    private Integer status;

    private LocalDateTime createdTime;
}
