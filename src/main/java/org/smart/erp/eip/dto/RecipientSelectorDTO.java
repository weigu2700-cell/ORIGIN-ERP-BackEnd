package org.smart.erp.eip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.LinkedHashSet;
import java.util.Set;

/** A transport object for composing recipients without coupling callers to system tables. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientSelectorDTO {
    @Builder.Default
    private Set<Long> userIds = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> roleCodes = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> permissionCodes = new LinkedHashSet<>();
    @Builder.Default
    private Set<Long> departmentIds = new LinkedHashSet<>();
    private boolean allActiveUsers = false;
    private boolean includeChildDepartments = false;
    private boolean includeAdministrators = false;

    public static RecipientSelectorDTO permissions(
            Set<String> permissionCodes,
            boolean includeAdministrators
    ) {
        RecipientSelectorDTO selector = new RecipientSelectorDTO();
        selector.setPermissionCodes(permissionCodes == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(permissionCodes));
        selector.setIncludeAdministrators(includeAdministrators);
        return selector;
    }

    public static RecipientSelectorDTO users(Set<Long> userIds) {
        RecipientSelectorDTO selector = new RecipientSelectorDTO();
        selector.setUserIds(userIds == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(userIds));
        return selector;
    }
}
