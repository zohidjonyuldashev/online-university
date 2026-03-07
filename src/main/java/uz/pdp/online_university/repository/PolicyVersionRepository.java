package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.PolicyVersion;
import uz.pdp.online_university.enums.PolicyKey;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, Long> {

    /**
     * Returns the most recent version for a given policy key.
     */
    Optional<PolicyVersion> findTopByPolicyKeyOrderByVersionDesc(PolicyKey policyKey);

    /**
     * Returns the full version history for a policy key, newest first.
     */
    List<PolicyVersion> findAllByPolicyKeyOrderByVersionDesc(PolicyKey policyKey);

    /**
     * Returns the highest version number currently stored for a key.
     * Used to compute the next version number on write.
     */
    @Query("SELECT COALESCE(MAX(pv.version), 0) FROM PolicyVersion pv WHERE pv.policyKey = :key")
    int findMaxVersionByPolicyKey(@Param("key") PolicyKey key);

    /**
     * Returns true if at least one version exists for the given key (used by
     * seeder).
     */
    boolean existsByPolicyKey(PolicyKey policyKey);

    /**
     * Returns the latest version for every distinct policy key.
     * Used by GET /admin/policies — one record per key.
     */
    @Query("""
            SELECT pv FROM PolicyVersion pv
            WHERE pv.version = (
                SELECT MAX(pv2.version) FROM PolicyVersion pv2
                WHERE pv2.policyKey = pv.policyKey
            )
            ORDER BY pv.policyKey
            """)
    List<PolicyVersion> findLatestForAllKeys();
}
