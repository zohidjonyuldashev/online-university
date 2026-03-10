package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.online_university.enums.AuditAction;
import uz.pdp.online_university.enums.AuditSource;

import java.time.Instant;

/**
 * Immutable audit trail record.
 * Does NOT extend BaseEntity — audit rows must never be modified/updated.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_actor_id", columnList = "actor_id"),
        @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
        @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    // Who performed the action
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_roles", length = 500)
    private String actorRoles; // comma-separated role list

    // What was changed
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private AuditAction action;

    // State snapshots (JSON)
    @Column(name = "before_snapshot", columnDefinition = "TEXT")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", columnDefinition = "TEXT")
    private String afterSnapshot;

    // Correlation
    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    // Origin
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private AuditSource source;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;
}
