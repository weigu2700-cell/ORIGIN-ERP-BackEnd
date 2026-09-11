package org.smart.erp.master.dto.CustomerDto;

import lombok.Data;
import org.smart.erp.master.enums.CustomerStatus;

@Data
public class CustomerPageDto {

    private Integer page = 1;

    private Integer pageSize = 10;

    private String name;

    private String code;

    private CustomerStatus status;
}
