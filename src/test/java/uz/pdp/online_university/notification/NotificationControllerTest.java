package uz.pdp.online_university.notification;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uz.pdp.online_university.dto.request.NotifyRequest;
import uz.pdp.online_university.dto.response.NotificationResponse;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.*;
import uz.pdp.online_university.repository.AuditLogRepository;
import uz.pdp.online_university.repository.NotificationRepository;
import uz.pdp.online_university.repository.NotificationTemplateRepository;
import uz.pdp.online_university.repository.OtpVerificationRepository;
import uz.pdp.online_university.repository.PermissionRepository;
import uz.pdp.online_university.repository.PolicyVersionRepository;
import uz.pdp.online_university.repository.RoleRepository;
import uz.pdp.online_university.repository.UserRepository;
import uz.pdp.online_university.security.CustomUserDetails;
import uz.pdp.online_university.service.EmailService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
class NotificationControllerTest {

    @TestConfiguration
    static class TestAuditConfig {
        @Bean
        @Primary
        AuditorAware<Long> testAuditorProvider() {
            return Optional::empty;
        }
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    NotificationService notificationService;
    @MockitoBean
    NotificationTemplateService notificationTemplateService;
    @MockitoBean
    RateLimiter rateLimiter;

    // Repo mocks — JPA excluded
    @MockitoBean
    NotificationRepository notificationRepository;
    @MockitoBean
    NotificationTemplateRepository notificationTemplateRepository;
    @MockitoBean
    PolicyVersionRepository policyVersionRepository;
    @MockitoBean
    AuditLogRepository auditLogRepository;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    RoleRepository roleRepository;
    @MockitoBean
    PermissionRepository permissionRepository;
    @MockitoBean
    OtpVerificationRepository otpVerificationRepository;
    @MockitoBean
    JavaMailSender javaMailSender;
    @MockitoBean
    EmailService emailService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private NotificationResponse sampleResponse() {
        return NotificationResponse.builder()
                .id(1L)
                .templateKey(NotificationTemplateKey.EXAM_SCHEDULED)
                .channel(NotificationChannel.IN_APP)
                .subject("Exam: Math")
                .renderedBody("Your exam Math is on 2026-04-01.")
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .unread(true)
                .build();
    }

    private NotifyRequest buildSendRequest() {
        NotifyRequest req = new NotifyRequest();
        req.setTemplateKey(NotificationTemplateKey.EXAM_SCHEDULED);
        req.setChannel(NotificationChannel.IN_APP);
        req.setUserId(1L);
        return req;
    }

    // -------------------------------------------------------------------------
    // Helper: build a CustomUserDetails mock principal with id=1
    // -------------------------------------------------------------------------

    private CustomUserDetails mockPrincipal() {
        User mockUser = User.builder()
                .email("student@test.com")
                .password("pwd")
                .status(UserStatus.ACTIVE)
                .accessState(AccessState.ACTIVE)
                .roles(Set.of())
                .build();
        // set ID via reflection since BaseEntity id is protected
        org.springframework.test.util.ReflectionTestUtils.setField(mockUser, "id", 1L);
        return new CustomUserDetails(mockUser);
    }

    // -------------------------------------------------------------------------
    // POST /api/internal/notify — Event 1: Exam Scheduled
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/internal/notify returns 201 for ADMIN (Exam Scheduled event)")
    @WithMockUser(roles = "ADMIN")
    void send_adminCreatesNotification() throws Exception {
        when(notificationService.send(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/internal/notify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSendRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateKey").value("EXAM_SCHEDULED"))
                .andExpect(jsonPath("$.channel").value("IN_APP"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("POST /api/internal/notify returns 403 for STUDENT")
    @WithMockUser(roles = "STUDENT")
    void send_studentGets403() throws Exception {
        mockMvc.perform(post("/api/internal/notify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSendRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/internal/notify returns 401 for unauthenticated")
    void send_unauthenticatedGets401() throws Exception {
        mockMvc.perform(post("/api/internal/notify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildSendRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/internal/notify returns 400 when templateKey is null")
    @WithMockUser(roles = "ADMIN")
    void send_missingTemplateKey_returns400() throws Exception {
        NotifyRequest bad = new NotifyRequest();
        bad.setTemplateKey(null);
        bad.setUserId(1L);

        mockMvc.perform(post("/api/internal/notify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/me/notifications — Event 2: Debt block (view result)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/me/notifications returns 200 for authenticated user")
    void getMyNotifications_returns200() throws Exception {
        NotificationResponse debtBlockedNotif = NotificationResponse.builder()
                .id(2L).templateKey(NotificationTemplateKey.DEBT_BLOCKED)
                .channel(NotificationChannel.IN_APP).subject("Account Blocked")
                .renderedBody("Blocked due to debt of $500.")
                .status(NotificationStatus.SENT).unread(true)
                .createdAt(Instant.now()).build();

        when(notificationService.getMyNotifications(any(), any()))
                .thenReturn(new PageImpl<>(List.of(debtBlockedNotif)));

        mockMvc.perform(get("/api/me/notifications")
                .with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].templateKey").value("DEBT_BLOCKED"))
                .andExpect(jsonPath("$.content[0].unread").value(true));
    }

    @Test
    @DisplayName("GET /api/me/notifications returns 401 for unauthenticated")
    void getMyNotifications_unauthenticatedGets401() throws Exception {
        mockMvc.perform(get("/api/me/notifications"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/me/notifications/unread-count
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/me/notifications/unread-count returns badge count")
    void unreadCount_returnsBadgeCount() throws Exception {
        when(notificationService.countUnread(any())).thenReturn(3L);

        mockMvc.perform(get("/api/me/notifications/unread-count")
                .with(user(mockPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    // -------------------------------------------------------------------------
    // POST /api/me/notifications/{id}/read
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/me/notifications/{id}/read returns 200 for authenticated user")
    void markRead_returns200() throws Exception {
        NotificationResponse read = sampleResponse();
        read.setReadAt(Instant.now());
        read.setUnread(false);
        when(notificationService.markRead(eq(1L), any())).thenReturn(read);

        mockMvc.perform(post("/api/me/notifications/1/read")
                .with(user(mockPrincipal()))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(false));
    }
}
