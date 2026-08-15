package org.smart.erp.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DeptVO {

    private String id;

    private String name;

    private String code;

    private String parentName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private String createTime;

    private String updateTime;

}
