package org.smart.erp.master.dto.CustomerDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerCreateDTO {

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String shortName;

    private String contactName;

    private String address;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String remark;
}
