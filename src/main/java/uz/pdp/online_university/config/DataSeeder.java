package uz.pdp.online_university.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uz.pdp.online_university.entity.Role;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.repository.RoleRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
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
