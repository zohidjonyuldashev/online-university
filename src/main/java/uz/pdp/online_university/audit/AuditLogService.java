package uz.pdp.online_university.audit;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.dto.request.AuditLogFilterRequest;
import uz.pdp.online_university.entity.AuditLog;
import uz.pdp.online_university.enums.AuditAction;
import uz.pdp.online_university.enums.AuditSource;
import uz.pdp.online_university.repository.AuditLogRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Core service for creating and querying audit log entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final RequestContext requestContext;

    // -------------------------------------------------------------------------
    // Write Side
    // -------------------------------------------------------------------------

    /**
     * Asynchronously persists an audit log entry.
     * Uses REQUIRES_NEW transaction so that the audit write is independent of the
     * caller's transaction.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(Long actorId,
            String actorRoles,
            String entityType,
            String entityId,
            AuditAction action,
            String beforeSnapshot,
            String afterSnapshot) {
        try {
            String requestId = null;
            AuditSource source = AuditSource.SYSTEM;
            try {
                requestId = requestContext.getRequestId();
                source = requestContext.getSource();
            } catch (Exception ignored) {
                // RequestContext may not be available in async thread — safe to skip
            }

            AuditLog entry = AuditLog.builder()
                    .actorId(actorId)
                    .actorRoles(actorRoles)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .beforeSnapshot(beforeSnapshot)
                    .afterSnapshot(afterSnapshot)
                    .requestId(requestId)
                    .correlationId(requestId)
                    .source(source)
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(entry);
            log.debug("Audit log saved: action={}, entityType={}, entityId={}", action, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to save audit log entry: action={}, entityType={}, entityId={}: {}",
                    action, entityType, entityId, e.getMessage(), e);
        }
    }

    /**
     * Convenience overload for manual (non-AOP) audit entries.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(String entityType,
            String entityId,
            AuditAction action,
            String beforeSnapshot,
            String afterSnapshot) {
        logAsync(null, null, entityType, entityId, action, beforeSnapshot, afterSnapshot);
    }

    // -------------------------------------------------------------------------
    // Read Side
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<AuditLog> search(AuditLogFilterRequest filter, Pageable pageable) {
        return auditLogRepository.searchFiltered(
                filter.getActorId(),
                filter.getEntityType(),
                filter.getEntityId(),
                filter.getFrom(),
                filter.getTo(),
                pageable);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(AuditLogFilterRequest filter) {
        List<AuditLog> rows = auditLogRepository.searchFilteredAll(
                filter.getActorId(),
                filter.getEntityType(),
                filter.getEntityId(),
                filter.getFrom(),
                filter.getTo());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter csv = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            // Header
            csv.writeNext(new String[] {
                    "id", "timestamp", "actorId", "actorRoles",
                    "entityType", "entityId", "action",
                    "requestId", "correlationId", "source",
                    "beforeSnapshot", "afterSnapshot"
            });

            // Rows
            for (AuditLog a : rows) {
                csv.writeNext(new String[] {
                        str(a.getId()),
                        str(a.getTimestamp()),
                        str(a.getActorId()),
                        a.getActorRoles(),
                        a.getEntityType(),
                        a.getEntityId(),
                        str(a.getAction()),
                        a.getRequestId(),
                        a.getCorrelationId(),
                        str(a.getSource()),
                        a.getBeforeSnapshot(),
                        a.getAfterSnapshot()
                });
            }
        } catch (IOException e) {
            log.error("Failed to generate CSV export: {}", e.getMessage(), e);
            throw new RuntimeException("CSV export failed", e);
        }
        return baos.toByteArray();
    }

    private String str(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}
