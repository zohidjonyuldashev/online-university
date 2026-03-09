package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.dto.response.PermissionResponse;
import uz.pdp.online_university.dto.response.RbacMatrixResponse;
import uz.pdp.online_university.dto.response.RoleResponse;
import uz.pdp.online_university.entity.Permission;
import uz.pdp.online_university.entity.Role;
import uz.pdp.online_university.repository.PermissionRepository;
import uz.pdp.online_university.repository.RoleRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public RbacMatrixResponse getMatrix() {

        List<Role> roles = roleRepository.findAll();
        List<Permission> allPermissions = permissionRepository.findAll();

        List<String> roleNames = roles.stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();

        Map<String, Set<String>> matrix = new LinkedHashMap<>();
        for (Role role : roles) {
            Set<String> permissionKeys = role.getPermissions().stream()
                    .map(Permission::getKey)
                    .collect(Collectors.toSet());
            matrix.put(role.getName().name(), permissionKeys);
        }

        Map<String, List<Permission>> grouped = allPermissions.stream()
                .collect(Collectors.groupingBy(
                        Permission::getModule,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RbacMatrixResponse.ModulePermissions> modules = grouped.entrySet().stream()
                .map(entry -> RbacMatrixResponse.ModulePermissions.builder()
                        .module(entry.getKey())
                        .permissions(entry.getValue().stream()
                                .map(p -> PermissionResponse.builder()
                                        .id(p.getId())
                                        .key(p.getKey())
                                        .module(p.getModule())
                                        .description(p.getDescription())
                                        .build())
                                .sorted(Comparator.comparing(PermissionResponse::getKey))
                                .toList())
                        .build())
                .toList();

        return RbacMatrixResponse.builder()
                .roles(roleNames)
                .modules(modules)
                .matrix(matrix)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName().name())
                        .description(role.getDescription())
                        .permissions(role.getPermissions().stream()
                                .map(Permission::getKey)
                                .collect(Collectors.toSet()))
                        .build())
                .sorted(Comparator.comparing(RoleResponse::getName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .key(p.getKey())
                        .module(p.getModule())
                        .description(p.getDescription())
                        .build())
                .sorted(Comparator.comparing(PermissionResponse::getKey))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsByModule(String module) {
        return permissionRepository.findAllByModule(module).stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .key(p.getKey())
                        .module(p.getModule())
                        .description(p.getDescription())
                        .build())
                .sorted(Comparator.comparing(PermissionResponse::getKey))
                .toList();
    }
}
