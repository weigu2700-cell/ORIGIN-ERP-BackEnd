package org.smart.erp.system.dto;

import lombok.Data;
import org.smart.erp.system.Enum.UserStatus;

@Data
public class UserStatusUpdateDto {

    private UserStatus status;
}
