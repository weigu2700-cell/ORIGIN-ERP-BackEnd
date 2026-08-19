package org.smart.erp.master.dto.CustomerDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.smart.erp.master.enums.CustomerStatus;

@Data
public class CustomerStatusDTO {

    @NotNull(message = "客户状态不能为空")
    private CustomerStatus status;
}

