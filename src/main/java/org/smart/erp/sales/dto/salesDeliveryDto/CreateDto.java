package org.smart.erp.sales.dto.salesDeliveryDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.smart.erp.sales.dto.salesDeliveryItemDto.CreateItemDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateDto {

    @NotNull(message = "销售订单ID不能为空")
    private Long salesOrderId;

    @NotNull(message = "出库日期不能为空")
    private LocalDateTime deliveryDate;

    @NotEmpty(message = "出库明细不能为空")
    @Valid
    private List<CreateItemDto> items;

    private String remark;
}
