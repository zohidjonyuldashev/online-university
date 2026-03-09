package uz.pdp.online_university.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.entity.NotificationTemplate;
import uz.pdp.online_university.entity.Permission;
import uz.pdp.online_university.entity.Role;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.NotificationChannel;
import uz.pdp.online_university.enums.NotificationTemplateKey;
import uz.pdp.online_university.enums.PermissionCatalog;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.service.NotificationTemplateService;
import uz.pdp.online_university.service.PolicyService;
import uz.pdp.online_university.repository.NotificationTemplateRepository;
import uz.pdp.online_university.repository.PermissionRepository;
import uz.pdp.online_university.repository.RoleRepository;
import uz.pdp.online_university.repository.UserRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PolicyService policyService;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationTemplateRepository notificationTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Permission> permissionMap = seedPermissions();
        seedRolesWithPermissions(permissionMap);
        seedAdminUser();

        policyService.seedDefaults();
        seedNotificationTemplates();
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> permissionMap = new HashMap<>();

        for (PermissionCatalog catalog : PermissionCatalog.values()) {
            Permission permission = permissionRepository.findByKey(catalog.getKey())
                    .orElseGet(() -> {
                        Permission newPermission = Permission.builder()
                                .key(catalog.getKey())
                                .module(catalog.getModule())
                                .description(catalog.getDescription())
                                .build();
                        log.info("Seeded permission: {}", catalog.getKey());
                        return permissionRepository.save(newPermission);
                    });

            boolean updated = false;
            if (!catalog.getDescription().equals(permission.getDescription())) {
                permission.setDescription(catalog.getDescription());
                updated = true;
            }
            if (!catalog.getModule().equals(permission.getModule())) {
                permission.setModule(catalog.getModule());
                updated = true;
            }
            if (updated) {
                permission = permissionRepository.save(permission);
                log.info("Updated permission: {}", catalog.getKey());
            }

            permissionMap.put(catalog.getKey(), permission);
        }

        log.info("Total permissions seeded: {}", permissionMap.size());
        return permissionMap;
    }

    private void seedRolesWithPermissions(Map<String, Permission> permissionMap) {
        for (RoleName roleName : RoleName.values()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role newRole = Role.builder()
                                .name(roleName)
                                .description(getRoleDescription(roleName))
                                .build();
                        log.info("Seeded role: {}", roleName);
                        return roleRepository.save(newRole);
                    });

            Set<PermissionCatalog> expectedPermissions = RolePermissionMapping.MATRIX
                    .getOrDefault(roleName, Set.of());

            Set<Permission> resolvedPermissions = new HashSet<>();
            for (PermissionCatalog catalog : expectedPermissions) {
                Permission permission = permissionMap.get(catalog.getKey());
                if (permission != null) {
                    resolvedPermissions.add(permission);
                }
            }

            if (!role.getPermissions().equals(resolvedPermissions)) {
                role.setPermissions(resolvedPermissions);
                roleRepository.save(role);
                log.info("Updated permissions for role {}: {} permissions assigned",
                        roleName, resolvedPermissions.size());
            }
        }
    }

    private void seedNotificationTemplates() {
        seedTemplate(
                NotificationTemplateKey.ADMISSION_STATUS_UPDATE,
                "Admission Status Update",
                "Your admission application status has been updated to: {{status}}. {{message}}",
                "[\"status\",\"message\"]");
        seedTemplate(
                NotificationTemplateKey.EXAM_SCHEDULED,
                "Exam Scheduled: {{examName}}",
                "Your exam {{examName}} has been scheduled for {{date}} at {{time}}. Location: {{location}}.",
                "[\"examName\",\"date\",\"time\",\"location\"]");
        seedTemplate(
                NotificationTemplateKey.EXAM_RESULT_PUBLISHED,
                "Exam Results: {{examName}}",
                "Results for {{examName}} are now available. Your grade: {{grade}}.",
                "[\"examName\",\"grade\"]");
        seedTemplate(
                NotificationTemplateKey.DEBT_BLOCKED,
                "Account Blocked: Outstanding Debt",
                "Your account has been blocked due to an outstanding debt of {{amount}}. Please contact the finance department.",
                "[\"amount\"]");
        seedTemplate(
                NotificationTemplateKey.DEBT_UNBLOCKED,
                "Account Unblocked",
                "Your account has been unblocked. Your debt of {{amount}} has been cleared.",
                "[\"amount\"]");
        seedTemplate(
                NotificationTemplateKey.AQAD_REVIEW_DECISION,
                "AQAD Review Decision",
                "The AQAD committee has made a decision regarding your submission: {{decision}}. {{remarks}}",
                "[\"decision\",\"remarks\"]");
    }

    private void seedTemplate(NotificationTemplateKey key, String subject, String body, String variablesJson) {
        boolean exists = notificationTemplateRepository
                .findByTemplateKeyAndChannelAndActiveTrue(key, NotificationChannel.IN_APP)
                .isPresent();
        if (!exists) {
            notificationTemplateService.save(NotificationTemplate.builder()
                    .templateKey(key)
                    .channel(NotificationChannel.IN_APP)
                    .subject(subject)
                    .body(body)
                    .variablesJson(variablesJson)
                    .active(true)
                    .build());
            log.info("Seeded notification template: {} / {}", key, NotificationChannel.IN_APP);
        }
    }

    private void seedAdminUser() {
        String adminEmail = "admin@university.uz";

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists, skipping.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found. Cannot seed admin user."));

        User admin = User.builder()
                .firstName("System")
                .lastName("Administrator")
                .email(adminEmail)
                .phone("+998900000000")
                .password(passwordEncoder.encode("Admin@123"))
                .emailVerified(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("Seeded admin user: {}", adminEmail);
    }

    private String getRoleDescription(RoleName roleName) {
        return switch (roleName) {
            case ADMIN -> "System administrator with full access";
            case TEACHER -> "Instructor who manages courses, lectures, and assessments";
            case STUDENT -> "Enrolled student with access to learning materials";
            case APPLICANT -> "Prospective student going through admission process";
            case ACADEMIC_DEPARTMENT -> "Manages academic programs, schedules, and enrollment rules";
            case AQAD -> "Academic Quality Assurance — reviews and approves course content";
            case FINANCE -> "Manages payments, contracts, and financial blocking";
            case RESOURCE_DEPARTMENT -> "Manages teacher assignments and workload";
            case DEPUTY_DIRECTOR -> "Executive oversight with access to dashboards and analytics";
            case IT_OPERATIONS -> "System operations, monitoring, and infrastructure management";
        };
    }
}