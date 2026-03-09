package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.dto.request.*;
import uz.pdp.online_university.dto.response.MessageResponse;
import uz.pdp.online_university.dto.response.PagedResponse;
import uz.pdp.online_university.dto.response.RoleResponse;
import uz.pdp.online_university.dto.response.UserDetailResponse;
import uz.pdp.online_university.entity.Permission;
import uz.pdp.online_university.entity.Role;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.AccessState;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.enums.UserStatus;
import uz.pdp.online_university.exception.DuplicateResourceException;
import uz.pdp.online_university.exception.InvalidOperationException;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.RoleRepository;
import uz.pdp.online_university.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        Set<Role> roles = resolveRoles(request.getRoles());

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(true)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        log.info("Admin created user: {} ({}) with roles: {}",
                user.getEmail(), user.getId(), request.getRoles());

        return mapToDetailResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long userId) {
        User user = findUserOrThrow(userId);
        return mapToDetailResponse(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDetailResponse> getUsers(
            String search, UserStatus status, RoleName role, Pageable pageable) {

        Page<UserDetailResponse> page = userRepository
                .findAllWithFilters(search, status, role, pageable)
                .map(this::mapToDetailResponse);

        return PagedResponse.of(page);
    }

    @Transactional
    public UserDetailResponse updateUser(Long userId, UpdateUserRequest request) {

        User user = findUserOrThrow(userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().toLowerCase().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new DuplicateResourceException("User", "email", newEmail);
                }
                user.setEmail(newEmail);
                user.setEmailVerified(false);
                user.incrementTokenVersion();
            }
        }

        if (request.getPhone() != null) {
            if (request.getPhone().isBlank()) {
                user.setPhone(null);
            } else if (!request.getPhone().equals(user.getPhone())) {
                if (userRepository.existsByPhone(request.getPhone())) {
                    throw new DuplicateResourceException("User", "phone", request.getPhone());
                }
                user.setPhone(request.getPhone());
            }
        }

        user = userRepository.save(user);

        log.info("Admin updated user profile: {} ({})", user.getEmail(), user.getId());

        return mapToDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {

        User user = findUserOrThrow(userId);

        Set<String> oldRoleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        Set<Role> newRoles = resolveRoles(request.getRoles());

        user.setRoles(newRoles);
        user.incrementTokenVersion();
        user = userRepository.save(user);

        log.info("Admin updated roles for user {} ({}): {} → {}",
                user.getEmail(), user.getId(), oldRoleNames, request.getRoles());

        return mapToDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {

        User user = findUserOrThrow(userId);
        UserStatus newStatus = request.getStatus();
        UserStatus oldStatus = user.getStatus();

        if (oldStatus == newStatus) {
            throw new InvalidOperationException("User is already " + newStatus);
        }

        user.setStatus(newStatus);

        if (newStatus != UserStatus.ACTIVE) {
            user.incrementTokenVersion();
        }

        user = userRepository.save(user);

        log.info("Admin changed status for user {} ({}): {} → {} (reason: {})",
                user.getEmail(), user.getId(), oldStatus, newStatus, request.getReason());

        return mapToDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse updateAccessState(Long userId, UpdateAccessStateRequest request) {

        User user = findUserOrThrow(userId);
        AccessState newState = request.getAccessState();
        AccessState oldState = user.getAccessState();

        if (oldState == newState) {
            throw new InvalidOperationException("User access state is already " + newState);
        }

        user.setAccessState(newState);

        if (newState != AccessState.ACTIVE && newState != AccessState.TEMPORARY_OVERRIDE) {
            user.incrementTokenVersion();
        }

        user = userRepository.save(user);

        log.info("Admin changed access state for user {} ({}): {} → {} (reason: {})",
                user.getEmail(), user.getId(), oldState, newState, request.getReason());

        return mapToDetailResponse(user);
    }

    @Transactional
    public MessageResponse terminateSessions(Long userId) {

        User user = findUserOrThrow(userId);

        user.incrementTokenVersion();
        userRepository.save(user);

        log.info("Admin terminated all sessions for user: {} ({})", user.getEmail(), user.getId());

        return MessageResponse.builder()
                .message("All sessions terminated for user " + user.getEmail())
                .build();
    }

    // ---- Helper Methods ----

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();

        for (RoleName roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
            roles.add(role);
        }

        return roles;
    }

    private UserDetailResponse mapToDetailResponse(User user) {
        Set<RoleResponse> roleResponses = user.getRoles().stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName().name())
                        .description(role.getDescription())
                        .permissions(role.getPermissions().stream()
                                .map(Permission::getKey)
                                .collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toSet());

        return UserDetailResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .emailVerified(user.isEmailVerified())
                .status(user.getStatus().name())
                .accessState(user.getAccessState().name())
                .roles(roleResponses)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}
