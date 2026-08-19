package org.smart.erp.master.dto.CustomerDTO;

import lombok.Data;
import org.smart.erp.master.enums.CustomerStatus;

@Data
public class CustomerUpdateDTO {

    private String id;

    private String name;

    private String shortName;

    private String contactName;

    private String address;

    private String phone;

    private String email;

    private String remark;

    private CustomerStatus status;
}
