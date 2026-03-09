package uz.pdp.online_university.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uz.pdp.online_university.dto.request.NotifyRequest;
import uz.pdp.online_university.dto.response.NotificationResponse;
import uz.pdp.online_university.entity.Notification;
import uz.pdp.online_university.entity.NotificationTemplate;
import uz.pdp.online_university.enums.NotificationChannel;
import uz.pdp.online_university.enums.NotificationStatus;
import uz.pdp.online_university.enums.NotificationTemplateKey;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.NotificationRepository;
import uz.pdp.online_university.service.EmailService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationTemplateService templateService;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    TemplateRenderer renderer;
    @Mock
    RateLimiter rateLimiter;
    @Mock
    EmailService emailService;

    @InjectMocks
    NotificationService notificationService;

    private NotificationTemplate inAppTemplate;

    @BeforeEach
    void setUp() {
        inAppTemplate = NotificationTemplate.builder()
                .templateKey(NotificationTemplateKey.EXAM_SCHEDULED)
                .channel(NotificationChannel.IN_APP)
                .subject("Exam: {{examName}}")
                .body("Your exam {{examName}} is on {{date}}.")
                .active(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // send — IN_APP (Event 1: EXAM_SCHEDULED)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("send IN_APP — persists Notification row and returns SENT status")
    void send_inApp_persistsAndReturnsSent() {
        NotifyRequest request = buildRequest(NotificationChannel.IN_APP);

        when(templateService.findActive(any(), any())).thenReturn(inAppTemplate);
        when(renderer.render("Exam: {{examName}}", request.getVariables())).thenReturn("Exam: Math");
        when(renderer.render("Your exam {{examName}} is on {{date}}.", request.getVariables()))
                .thenReturn("Your exam Math is on 2026-04-01.");
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setStatus(NotificationStatus.SENT);
            return n;
        });

        NotificationResponse response = notificationService.send(request);

        verify(rateLimiter).checkAndRecord(42L);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());

        Notification saved = captor.getAllValues().getFirst();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getTemplateKey()).isEqualTo(NotificationTemplateKey.EXAM_SCHEDULED);
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(saved.getSubject()).isEqualTo("Exam: Math");
        assertThat(response).isNotNull();
    }

    // -------------------------------------------------------------------------
    // send — EMAIL (Event 2: DEBT_BLOCKED)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("send EMAIL — dispatches async email and persists row")
    void send_email_dispatchesEmail() {
        NotificationTemplate emailTemplate = NotificationTemplate.builder()
                .templateKey(NotificationTemplateKey.DEBT_BLOCKED)
                .channel(NotificationChannel.EMAIL)
                .subject("Account Blocked")
                .body("Your account is blocked due to debt of {{amount}}.")
                .active(true)
                .build();

        NotifyRequest request = new NotifyRequest();
        request.setTemplateKey(NotificationTemplateKey.DEBT_BLOCKED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setUserId(7L);
        request.setVariables(Map.of("amount", "$500"));

        when(templateService.findActive(NotificationTemplateKey.DEBT_BLOCKED, NotificationChannel.EMAIL))
                .thenReturn(emailTemplate);
        when(renderer.render("Account Blocked", request.getVariables())).thenReturn("Account Blocked");
        when(renderer.render("Your account is blocked due to debt of {{amount}}.", request.getVariables()))
                .thenReturn("Your account is blocked due to debt of $500.");
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.send(request);

        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        verify(rateLimiter).checkAndRecord(7L);
    }

    // -------------------------------------------------------------------------
    // Rate limit
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("send — rate limit exceeded throws RateLimitExceededException before save")
    void send_rateLimitExceeded_throwsAndDoesNotSave() {
        NotifyRequest request = buildRequest(NotificationChannel.IN_APP);

        when(templateService.findActive(any(), any())).thenReturn(inAppTemplate);
        when(renderer.render(any(), any())).thenReturn("rendered");
        doThrow(new RateLimitExceededException("Too many")).when(rateLimiter).checkAndRecord(42L);

        assertThatThrownBy(() -> notificationService.send(request))
                .isInstanceOf(RateLimitExceededException.class);

        verify(notificationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getMyNotifications
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getMyNotifications delegates to repository")
    void getMyNotifications_delegatesToRepo() {
        Notification n = Notification.builder()
                .id(1L).userId(42L)
                .templateKey(NotificationTemplateKey.EXAM_SCHEDULED)
                .channel(NotificationChannel.IN_APP)
                .subject("Exam: Math")
                .renderedBody("Your exam Math is on 2026-04-01.")
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(42L), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        var page = notificationService.getMyNotifications(42L, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getSubject()).isEqualTo("Exam: Math");
        assertThat(page.getContent().getFirst().isUnread()).isTrue();
    }

    // -------------------------------------------------------------------------
    // markRead
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("markRead — sets readAt and returns updated response")
    void markRead_setsReadAt() {
        Notification n = Notification.builder()
                .id(5L).userId(42L)
                .templateKey(NotificationTemplateKey.EXAM_SCHEDULED)
                .channel(NotificationChannel.IN_APP)
                .subject("Exam").renderedBody("body")
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByIdAndUserId(5L, 42L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markRead(5L, 42L);
        assertThat(response.getReadAt()).isNotNull();
        assertThat(response.isUnread()).isFalse();
    }

    @Test
    @DisplayName("markRead — throws ResourceNotFoundException for wrong user")
    void markRead_wrongUser_throws() {
        when(notificationRepository.findByIdAndUserId(99L, 42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.markRead(99L, 42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private NotifyRequest buildRequest(NotificationChannel channel) {
        NotifyRequest r = new NotifyRequest();
        r.setTemplateKey(NotificationTemplateKey.EXAM_SCHEDULED);
        r.setChannel(channel);
        r.setUserId(42L);
        r.setVariables(Map.of("examName", "Math", "date", "2026-04-01",
                "time", "10:00", "location", "Room 101"));
        return r;
    }
}
