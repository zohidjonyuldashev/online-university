package uz.pdp.online_university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.AuditLog;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

        @Query(value = """
                        SELECT * FROM audit_log a
                        WHERE (:actorId IS NULL OR a.actor_id = :actorId)
                          AND (:entityType IS NULL OR a.entity_type = CAST(:entityType AS text))
                          AND (:entityId IS NULL OR a.entity_id = CAST(:entityId AS text))
                          AND (CAST(:from AS timestamptz) IS NULL OR a.timestamp >= CAST(:from AS timestamptz))
                          AND (CAST(:to AS timestamptz) IS NULL OR a.timestamp <= CAST(:to AS timestamptz))
                        ORDER BY a.timestamp DESC
                        """, countQuery = """
                        SELECT COUNT(*) FROM audit_log a
                        WHERE (:actorId IS NULL OR a.actor_id = :actorId)
                          AND (:entityType IS NULL OR a.entity_type = CAST(:entityType AS text))
                          AND (:entityId IS NULL OR a.entity_id = CAST(:entityId AS text))
                          AND (CAST(:from AS timestamptz) IS NULL OR a.timestamp >= CAST(:from AS timestamptz))
                          AND (CAST(:to AS timestamptz) IS NULL OR a.timestamp <= CAST(:to AS timestamptz))
                        """, nativeQuery = true)
        Page<AuditLog> searchFiltered(
                        @Param("actorId") Long actorId,
                        @Param("entityType") String entityType,
                        @Param("entityId") String entityId,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        Pageable pageable);

        @Query(value = """
                        SELECT * FROM audit_log a
                        WHERE (:actorId IS NULL OR a.actor_id = :actorId)
                          AND (:entityType IS NULL OR a.entity_type = CAST(:entityType AS text))
                          AND (:entityId IS NULL OR a.entity_id = CAST(:entityId AS text))
                          AND (CAST(:from AS timestamptz) IS NULL OR a.timestamp >= CAST(:from AS timestamptz))
                          AND (CAST(:to AS timestamptz) IS NULL OR a.timestamp <= CAST(:to AS timestamptz))
                        ORDER BY a.timestamp DESC
                        """, nativeQuery = true)
        java.util.List<AuditLog> searchFilteredAll(
                        @Param("actorId") Long actorId,
                        @Param("entityType") String entityType,
                        @Param("entityId") String entityId,
                        @Param("from") Instant from,
                        @Param("to") Instant to);
}
