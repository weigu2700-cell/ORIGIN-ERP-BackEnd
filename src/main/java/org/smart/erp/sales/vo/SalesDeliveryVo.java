package org.smart.erp.sales.vo;

import lombok.Data;
import org.smart.erp.sales.enums.SalesDeliveryStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesDeliveryVo {

    private Long id;

    private String deliveryNo;

    private  Long salesOrderId;

    private String salesOrderNo;

    private Long customerId;

    private String customerName;

    private LocalDateTime deliveryDate;

    private SalesDeliveryStatus status;

    private List<SalesDeliveryItemVo> items;

    private String remark;

}
