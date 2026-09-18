package org.smart.erp.eip.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.smart.erp.eip.enums.RecipientSelectorType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipientOptionVO {
    private RecipientSelectorType type;
    private String value;
    private String label;
}
