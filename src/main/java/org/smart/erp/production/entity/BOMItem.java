package org.smart.erp.production.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("prd_bom_item")
public class BOMItem {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long bomId;

    private Integer lineNo;

    private Long componentMaterialId;

    private BigDecimal quantity;

    private BigDecimal lossRate;

    private String remark;

}
