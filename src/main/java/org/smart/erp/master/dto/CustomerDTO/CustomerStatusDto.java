package org.smart.erp.master.dto.CustomerDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.smart.erp.master.enums.CustomerStatus;

@Data
public class CustomerStatusDto {

    @NotNull(message = "客户状态不能为空")
    private CustomerStatus status;
}

