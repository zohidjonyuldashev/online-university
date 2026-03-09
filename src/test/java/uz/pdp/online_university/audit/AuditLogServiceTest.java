//package uz.pdp.online_university.audit;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import uz.pdp.online_university.dto.request.AuditLogFilterRequest;
//import uz.pdp.online_university.entity.AuditLog;
//import uz.pdp.online_university.enums.AuditAction;
//import uz.pdp.online_university.enums.AuditSource;
//import uz.pdp.online_university.repository.AuditLogRepository;
//
//import java.time.Instant;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class AuditLogServiceTest {
//
//    @Mock
//    private AuditLogRepository auditLogRepository;
//
//    @Mock
//    private RequestContext requestContext;
//
//    @InjectMocks
//    private AuditLogService auditLogService;
//
//    private AuditLog sampleLog;
//
//    @BeforeEach
//    void setUp() {
//        sampleLog = AuditLog.builder()
//                .id(1L)
//                .actorId(10L)
//                .actorRoles("ADMIN")
//                .entityType("User")
//                .entityId("42")
//                .action(AuditAction.CREATE)
//                .afterSnapshot("{\"email\":\"test@example.com\"}")
//                .requestId("req-uuid-001")
//                .correlationId("req-uuid-001")
//                .source(AuditSource.API)
//                .timestamp(Instant.now())
//                .build();
//    }
//
//    @Test
//    @DisplayName("logAsync should save an AuditLog with correct fields")
//    void logAsyncShouldSaveCorrectEntry() {
//        when(requestContext.getRequestId()).thenReturn("req-uuid-001");
//        when(requestContext.getSource()).thenReturn(AuditSource.API);
//        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);
//
//        auditLogService.logAsync(10L, "ADMIN", "User", "42", AuditAction.CREATE, null,
//                "{\"email\":\"test@example.com\"}");
//
//        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
//        verify(auditLogRepository).save(captor.capture());
//
//        AuditLog saved = captor.getValue();
//        assertThat(saved.getActorId()).isEqualTo(10L);
//        assertThat(saved.getEntityType()).isEqualTo("User");
//        assertThat(saved.getEntityId()).isEqualTo("42");
//        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
//        assertThat(saved.getBeforeSnapshot()).isNull();
//        assertThat(saved.getAfterSnapshot()).contains("test@example.com");
//        assertThat(saved.getRequestId()).isEqualTo("req-uuid-001");
//    }
//
//    @Test
//    @DisplayName("logAsync should handle RequestContext failure gracefully")
//    void logAsyncShouldHandleContextFailure() {
//        when(requestContext.getRequestId()).thenThrow(new RuntimeException("context unavailable"));
//        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);
//
//        // Should not throw
//        auditLogService.logAsync(null, null, "Role", "5", AuditAction.DELETE, "{\"name\":\"STUDENT\"}", null);
//
//        verify(auditLogRepository).save(any(AuditLog.class));
//    }
//
//    @Test
//    @DisplayName("search should delegate to repository with correct parameters")
//    void searchShouldDelegateToRepository() {
//        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog));
//        when(auditLogRepository.searchFiltered(any(), any(), any(), any(), any(), any())).thenReturn(page);
//
//        AuditLogFilterRequest filter = new AuditLogFilterRequest();
//        filter.setEntityType("User");
//
//        Page<AuditLog> result = auditLogService.search(filter, PageRequest.of(0, 10));
//
//        assertThat(result.getContent()).hasSize(1);
//        verify(auditLogRepository).searchFiltered(null, "User", null, null, null, PageRequest.of(0, 10));
//    }
//
//    @Test
//    @DisplayName("exportCsv should return CSV bytes with header row")
//    void exportCsvShouldReturnValidCsv() {
//        when(auditLogRepository.searchFilteredAll(any(), any(), any(), any(), any()))
//                .thenReturn(List.of(sampleLog));
//
//        byte[] csv = auditLogService.exportCsv(new AuditLogFilterRequest());
//
//        assertThat(csv).isNotEmpty();
//        String csvStr = new String(csv);
//        // Check CSV has header
//        assertThat(csvStr).contains("id");
//        assertThat(csvStr).contains("actorId");
//        assertThat(csvStr).contains("entityType");
//        assertThat(csvStr).contains("action");
//        // Check data row
//        assertThat(csvStr).contains("User");
//        assertThat(csvStr).contains("CREATE");
//    }
//
//    @Test
//    @DisplayName("exportCsv with no matching rows should return just header")
//    void exportCsvWithNoRowsShouldReturnHeader() {
//        when(auditLogRepository.searchFilteredAll(any(), any(), any(), any(), any()))
//                .thenReturn(List.of());
//
//        byte[] csv = auditLogService.exportCsv(new AuditLogFilterRequest());
//        String csvStr = new String(csv);
//
//        assertThat(csvStr).contains("id");
//        assertThat(csvStr.lines().count()).isEqualTo(1L); // only header
//    }
//}
