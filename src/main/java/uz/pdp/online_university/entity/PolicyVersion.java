package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import uz.pdp.online_university.enums.PolicyKey;

import java.time.LocalDateTime;

/**
 * Immutable versioned record for a single policy key.
 * A new row is inserted (never updated) each time an admin changes a policy.
 *
 * Table: policy_versions
 */
@Entity
@Table(name = "policy_versions", indexes = {
        @Index(name = "idx_pv_key_version", columnList = "policy_key, version DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_key", nullable = false, length = 60)
    private PolicyKey policyKey;

    /**
     * Monotonically increasing per policyKey (managed by PolicyService, not DB
     * sequence).
     */
    @Column(name = "version", nullable = false)
    private int version;

    /**
     * The policy value stored as a JSON string (may be a plain scalar like "80" or
     * a JSON object).
     */
    @Column(name = "value_json", nullable = false, columnDefinition = "TEXT")
    private String valueJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Id of the user (admin) who created this version. Null for system-seeded
     * defaults.
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Human-readable reason for the change, required on every admin update.
     */
    @Column(name = "change_reason", nullable = false, length = 500)
    private String changeReason;
}
