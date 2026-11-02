package org.smart.erp.master.dto.CustomerDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerAddDto {

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String shortName;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    private String address;

    @NotBlank(message = "联系电话不能为空")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String remark;
}
