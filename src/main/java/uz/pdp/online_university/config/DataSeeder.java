package uz.pdp.online_university.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uz.pdp.online_university.entity.NotificationTemplate;
import uz.pdp.online_university.entity.Role;
import uz.pdp.online_university.enums.NotificationChannel;
import uz.pdp.online_university.enums.NotificationTemplateKey;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.notification.NotificationTemplateService;
import uz.pdp.online_university.policy.PolicyService;
import uz.pdp.online_university.repository.NotificationTemplateRepository;
import uz.pdp.online_university.repository.RoleRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PolicyService policyService;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationTemplateRepository notificationTemplateRepository;

    @Override
    public void run(String... args) {
        seedRoles();
        policyService.seedDefaults();
        seedNotificationTemplates();
    }

    // -------------------------------------------------------------------------
    // Roles
    // -------------------------------------------------------------------------

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .description(getDescription(roleName))
                        .build();
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Notification templates (IN_APP defaults)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getDescription(RoleName roleName) {
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
