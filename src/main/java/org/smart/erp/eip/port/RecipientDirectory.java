package org.smart.erp.eip.port;

import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.enums.RecipientSelectorType;
import org.smart.erp.eip.vo.RecipientOptionVO;

import java.util.Set;
import java.util.List;

/** 收件人目录端口，由系统用户/角色/权限模块提供实现。 */
public interface RecipientDirectory {
    Set<Long> resolve(RecipientSelectorDTO selector);

    /** 语义化别名，供适配器或测试使用。 */
    default Set<Long> findRecipients(RecipientSelectorDTO selector) {
        return resolve(selector);
    }

    default List<RecipientOptionVO> options(RecipientSelectorType type, String keyword) {
        return List.of();
    }
}
