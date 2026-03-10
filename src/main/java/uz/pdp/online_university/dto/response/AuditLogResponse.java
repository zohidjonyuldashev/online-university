package uz.pdp.online_university.dto.response;

import lombok.Builder;
import lombok.Getter;
import uz.pdp.online_university.entity.AuditLog;
import uz.pdp.online_university.enums.AuditAction;
import uz.pdp.online_university.enums.AuditSource;

import java.time.Instant;

@Getter
@Builder
public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String actorRoles;
    private String entityType;
    private String entityId;
    private AuditAction action;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String requestId;
    private String correlationId;
    private AuditSource source;
    private Instant timestamp;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorRoles(log.getActorRoles())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .beforeSnapshot(log.getBeforeSnapshot())
                .afterSnapshot(log.getAfterSnapshot())
                .requestId(log.getRequestId())
                .correlationId(log.getCorrelationId())
                .source(log.getSource())
                .timestamp(log.getTimestamp())
                .build();
    }
}
