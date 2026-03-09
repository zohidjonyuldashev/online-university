package uz.pdp.online_university.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.pdp.online_university.dto.request.*;
import uz.pdp.online_university.dto.response.*;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.enums.UserStatus;
import uz.pdp.online_university.service.RbacService;
import uz.pdp.online_university.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RbacService rbacService;

    @GetMapping("/rbac/matrix")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RbacMatrixResponse> getMatrix() {
        return ResponseEntity.ok(rbacService.getMatrix());
    }

    @GetMapping("/rbac/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> getRoles() {
        return ResponseEntity.ok(rbacService.getAllRoles());
    }

    @GetMapping("/rbac/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PermissionResponse>> getPermissions(
            @RequestParam(required = false) String module) {

        if (module != null && !module.isBlank()) {
            return ResponseEntity.ok(rbacService.getPermissionsByModule(module));
        }
        return ResponseEntity.ok(rbacService.getAllPermissions());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('user.create')")
    public ResponseEntity<UserDetailResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('user.read')")
    public ResponseEntity<UserDetailResponse> getUser(@PathVariable Long userId) {

        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user.read')")
    public ResponseEntity<PagedResponse<UserDetailResponse>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) RoleName role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(userService.getUsers(search, status, role, pageable));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('user.update')")
    public ResponseEntity<UserDetailResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('user.role.assign')")
    public ResponseEntity<UserDetailResponse> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {

        return ResponseEntity.ok(userService.updateUserRoles(userId, request));
    }

    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasAuthority('user.deactivate')")
    public ResponseEntity<UserDetailResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        return ResponseEntity.ok(userService.updateUserStatus(userId, request));
    }

    @PatchMapping("/users/{userId}/access-state")
    @PreAuthorize("hasAuthority('user.deactivate')")
    public ResponseEntity<UserDetailResponse> updateAccessState(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateAccessStateRequest request) {

        return ResponseEntity.ok(userService.updateAccessState(userId, request));
    }

    @PostMapping("/users/{userId}/terminate-sessions")
    @PreAuthorize("hasAuthority('user.session.terminate')")
    public ResponseEntity<MessageResponse> terminateSessions(@PathVariable Long userId) {

        return ResponseEntity.ok(userService.terminateSessions(userId));
    }
}
