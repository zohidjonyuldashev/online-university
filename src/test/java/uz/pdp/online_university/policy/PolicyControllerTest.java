package uz.pdp.online_university.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uz.pdp.online_university.dto.request.PolicyUpdateRequest;
import uz.pdp.online_university.dto.response.PolicyVersionResponse;
import uz.pdp.online_university.enums.PolicyKey;
import uz.pdp.online_university.repository.AuditLogRepository;
import uz.pdp.online_university.repository.NotificationRepository;
import uz.pdp.online_university.repository.NotificationTemplateRepository;
import uz.pdp.online_university.repository.OtpVerificationRepository;
import uz.pdp.online_university.repository.PermissionRepository;
import uz.pdp.online_university.repository.PolicyVersionRepository;
import uz.pdp.online_university.repository.RoleRepository;
import uz.pdp.online_university.repository.UserRepository;
import uz.pdp.online_university.service.EmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        MailSenderAutoConfiguration.class
})
class PolicyControllerTest {

    @TestConfiguration
    static class TestAuditConfig {
        @Bean
        @Primary
        AuditorAware<Long> testAuditorProvider() {
            return Optional::empty;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock the service — controller delegates to it
    @MockitoBean
    private PolicyService policyService;

    // Mock the cache — PolicyService depends on it
    @MockitoBean
    private PolicyCache policyCache;

    // Repositories mocked since JPA is excluded
    @MockitoBean
    private PolicyVersionRepository policyVersionRepository;
    @MockitoBean
    private NotificationRepository notificationRepository;
    @MockitoBean
    private NotificationTemplateRepository notificationTemplateRepository;
    @MockitoBean
    private AuditLogRepository auditLogRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private RoleRepository roleRepository;
    @MockitoBean
    private PermissionRepository permissionRepository;
    @MockitoBean
    private OtpVerificationRepository otpVerificationRepository;
    @MockitoBean
    private JavaMailSender javaMailSender;
    @MockitoBean
    private EmailService emailService;

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    private PolicyVersionResponse sampleResponse(PolicyKey key, int version, String value) {
        return PolicyVersionResponse.builder()
                .id((long) version)
                .policyKey(key)
                .version(version)
                .valueJson(value)
                .createdAt(LocalDateTime.of(2026, 3, 6, 10, 0))
                .createdBy(null)
                .changeReason("Test reason")
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/policies — authorization
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/admin/policies returns 200 for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getAllLatest_adminGets200() throws Exception {
        when(policyService.getAllLatest()).thenReturn(List.of(
                sampleResponse(PolicyKey.ATTENDANCE_THRESHOLD, 1, "80"),
                sampleResponse(PolicyKey.EXAM_ELIGIBILITY_MIN_ATTENDANCE, 1, "75")));

        mockMvc.perform(get("/api/admin/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].policyKey").value("ATTENDANCE_THRESHOLD"))
                .andExpect(jsonPath("$[0].valueJson").value("80"))
                .andExpect(jsonPath("$[1].policyKey").value("EXAM_ELIGIBILITY_MIN_ATTENDANCE"));
    }

    @Test
    @DisplayName("GET /api/admin/policies returns 403 for STUDENT")
    @WithMockUser(roles = "STUDENT")
    void getAllLatest_studentGets403() throws Exception {
        mockMvc.perform(get("/api/admin/policies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/policies returns 403 for TEACHER")
    @WithMockUser(roles = "TEACHER")
    void getAllLatest_teacherGets403() throws Exception {
        mockMvc.perform(get("/api/admin/policies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/policies returns 401 for unauthenticated")
    void getAllLatest_unauthenticatedGets401() throws Exception {
        mockMvc.perform(get("/api/admin/policies"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/policies/{key} — history
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/admin/policies/{key} returns version history for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getHistory_adminGetsHistory() throws Exception {
        when(policyService.getHistory(PolicyKey.ATTENDANCE_THRESHOLD)).thenReturn(List.of(
                sampleResponse(PolicyKey.ATTENDANCE_THRESHOLD, 2, "85"),
                sampleResponse(PolicyKey.ATTENDANCE_THRESHOLD, 1, "80")));

        mockMvc.perform(get("/api/admin/policies/ATTENDANCE_THRESHOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].valueJson").value("85"))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[1].valueJson").value("80"));
    }

    // -------------------------------------------------------------------------
    // POST /api/admin/policies/{key} — update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/admin/policies/{key} returns 201 with new version for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void update_adminCreatesNewVersion() throws Exception {
        PolicyVersionResponse newVersion = sampleResponse(PolicyKey.ATTENDANCE_THRESHOLD, 2, "90");
        newVersion.setChangeReason("Increased for new semester");
        when(policyService.update(any(), any(), any())).thenReturn(newVersion);

        PolicyUpdateRequest request = new PolicyUpdateRequest();
        request.setValueJson("90");
        request.setChangeReason("Increased for new semester");

        mockMvc.perform(post("/api/admin/policies/ATTENDANCE_THRESHOLD")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.valueJson").value("90"))
                .andExpect(jsonPath("$.policyKey").value("ATTENDANCE_THRESHOLD"));
    }

    @Test
    @DisplayName("POST /api/admin/policies/{key} returns 400 when valueJson is blank")
    @WithMockUser(roles = "ADMIN")
    void update_blankValueJsonReturns400() throws Exception {
        PolicyUpdateRequest request = new PolicyUpdateRequest();
        request.setValueJson("");
        request.setChangeReason("Some reason");

        mockMvc.perform(post("/api/admin/policies/ATTENDANCE_THRESHOLD")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/policies/{key} returns 400 when changeReason is blank")
    @WithMockUser(roles = "ADMIN")
    void update_blankChangeReasonReturns400() throws Exception {
        PolicyUpdateRequest request = new PolicyUpdateRequest();
        request.setValueJson("85");
        request.setChangeReason("");

        mockMvc.perform(post("/api/admin/policies/ATTENDANCE_THRESHOLD")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/policies/{key} returns 403 for STUDENT")
    @WithMockUser(roles = "STUDENT")
    void update_studentGets403() throws Exception {
        PolicyUpdateRequest request = new PolicyUpdateRequest();
        request.setValueJson("70");
        request.setChangeReason("Should not be allowed");

        mockMvc.perform(post("/api/admin/policies/ATTENDANCE_THRESHOLD")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
