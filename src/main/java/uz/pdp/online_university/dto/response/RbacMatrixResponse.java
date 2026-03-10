package uz.pdp.online_university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RbacMatrixResponse {

    private List<String> roles;
    private List<ModulePermissions> modules;
    private Map<String, Set<String>> matrix; // roleName → set of permission keys

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModulePermissions {
        private String module;
        private List<PermissionResponse> permissions;
    }
}
