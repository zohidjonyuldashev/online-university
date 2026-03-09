package uz.pdp.online_university.audit;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uz.pdp.online_university.dto.request.AuditLogFilterRequest;
import uz.pdp.online_university.entity.AuditLog;
import uz.pdp.online_university.enums.AuditAction;
import uz.pdp.online_university.enums.AuditSource;
import uz.pdp.online_university.repository.AuditLogRepository;
import uz.pdp.online_university.repository.NotificationRepository;
import uz.pdp.online_university.repository.NotificationTemplateRepository;
import uz.pdp.online_university.repository.OtpVerificationRepository;
import uz.pdp.online_university.repository.PermissionRepository;
import uz.pdp.online_university.repository.PolicyVersionRepository;
import uz.pdp.online_university.repository.RoleRepository;
import uz.pdp.online_university.repository.UserRepository;
import uz.pdp.online_university.service.EmailService;

import java.time.Instant;
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
class AuditLogControllerTest {

        /**
         * Provides a no-op AuditorAware so that @EnableJpaAuditing in AuditConfig
         * does not fail when JPA context is excluded.
         */
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

        @MockitoBean
        private AuditLogService auditLogService;

        // Repositories mocked since JPA is excluded
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
        private PolicyVersionRepository policyVersionRepository;
        @MockitoBean
        private NotificationRepository notificationRepository;
        @MockitoBean
        private NotificationTemplateRepository notificationTemplateRepository;
        @MockitoBean
        private JavaMailSender javaMailSender;
        @MockitoBean
        private EmailService emailService;

        // -------------------------------------------------------------------------
        // Test data
        // -------------------------------------------------------------------------

        private static AuditLog sampleLog() {
                return AuditLog.builder()
                                .id(1L).actorId(5L).actorRoles("ADMIN")
                                .entityType("User").entityId("42")
                                .action(AuditAction.UPDATE)
                                .beforeSnapshot("{\"title\":\"Old\"}").afterSnapshot("{\"title\":\"New\"}")
                                .requestId("req-001").correlationId("req-001")
                                .source(AuditSource.API)
                                .timestamp(Instant.parse("2026-03-01T10:00:00Z"))
                                .build();
        }

        // -------------------------------------------------------------------------
        // Authorization
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("ADMIN can access GET /api/admin/audit")
        @WithMockUser(roles = "ADMIN")
        void adminCanAccessAuditEndpoint() throws Exception {
                when(auditLogService.search(any(), any())).thenReturn(new PageImpl<>(List.of(sampleLog())));

                mockMvc.perform(get("/api/admin/audit"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].entityType").value("User"))
                                .andExpect(jsonPath("$.content[0].action").value("UPDATE"))
                                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("IT_OPERATIONS can access GET /api/admin/audit")
        @WithMockUser(roles = "IT_OPERATIONS")
        void itOpsCanAccessAuditEndpoint() throws Exception {
                when(auditLogService.search(any(), any())).thenReturn(new PageImpl<>(List.of()));
                mockMvc.perform(get("/api/admin/audit")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("STUDENT cannot access audit endpoint — 403")
        @WithMockUser(roles = "STUDENT")
        void studentIsForbidden() throws Exception {
                mockMvc.perform(get("/api/admin/audit")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedGets401() throws Exception {
                mockMvc.perform(get("/api/admin/audit")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("ADMIN can filter by entity_type and actor_id")
        @WithMockUser(roles = "ADMIN")
        void adminCanFilter() throws Exception {
                when(auditLogService.search(any(), any())).thenReturn(new PageImpl<>(List.of(sampleLog())));

                mockMvc.perform(get("/api/admin/audit")
                                .param("entity_type", "User").param("actor_id", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].actorId").value(5));
        }

        // -------------------------------------------------------------------------
        // CSV Export
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("ADMIN can export CSV")
        @WithMockUser(roles = "ADMIN")
        void adminCanExportCsv() throws Exception {
                byte[] csv = "id,timestamp\n1,2026-03-01T10:00:00Z\n".getBytes();
                when(auditLogService.exportCsv(any(AuditLogFilterRequest.class))).thenReturn(csv);

                mockMvc.perform(post("/api/admin/audit/export")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AuditLogFilterRequest())))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Disposition",
                                                org.hamcrest.Matchers.containsString("attachment")))
                                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
        }

        @Test
        @DisplayName("TEACHER cannot export CSV — 403")
        @WithMockUser(roles = "TEACHER")
        void teacherCannotExportCsv() throws Exception {
                mockMvc.perform(post("/api/admin/audit/export")
                                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                                .andExpect(status().isForbidden());
        }
}
