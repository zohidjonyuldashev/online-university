package uz.pdp.online_university.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.annotation.Auditable;
import uz.pdp.online_university.dto.request.PolicyUpdateRequest;
import uz.pdp.online_university.dto.response.PolicyVersionResponse;
import uz.pdp.online_university.entity.PolicyVersion;
import uz.pdp.online_university.enums.AuditAction;
import uz.pdp.online_university.enums.PolicyKey;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.PolicyVersionRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Core service for the policy engine.
 *
 * <p>
 * All writes create a new {@link PolicyVersion} row — existing rows are never
 * mutated.
 * After each write the {@link PolicyCache} is refreshed so downstream services
 * always
 * read the latest value from memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyVersionRepository policyVersionRepository;
    private final PolicyCache policyCache;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /** Returns the latest version record for a single key. */
    @Transactional(readOnly = true)
    public PolicyVersionResponse getLatest(PolicyKey key) {
        return policyVersionRepository.findTopByPolicyKeyOrderByVersionDesc(key)
                .map(PolicyVersionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("PolicyVersion", "policyKey", key));
    }

    /** Returns one latest record per each known policy key. */
    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> getAllLatest() {
        return policyVersionRepository.findLatestForAllKeys()
                .stream()
                .map(PolicyVersionResponse::from)
                .toList();
    }

    /** Returns all historical versions for a key, newest first. */
    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> getHistory(PolicyKey key) {
        List<PolicyVersion> history = policyVersionRepository.findAllByPolicyKeyOrderByVersionDesc(key);
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("PolicyVersion", "policyKey", key);
        }
        return history.stream().map(PolicyVersionResponse::from).toList();
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Creates a new version for the given policy key.
     *
     * @param key     policy to update
     * @param request contains new valueJson and mandatory changeReason
     * @param actorId ID of the admin performing the update (may be null in system
     *                context)
     * @return the newly created version record
     */
    @Transactional
    @Auditable(entityType = "Policy", action = AuditAction.OTHER, entityIdExpression = "#result")
    public PolicyVersionResponse update(PolicyKey key, PolicyUpdateRequest request, Long actorId) {
        int nextVersion = policyVersionRepository.findMaxVersionByPolicyKey(key) + 1;

        PolicyVersion policyVersion = PolicyVersion.builder()
                .policyKey(key)
                .version(nextVersion)
                .valueJson(request.getValueJson())
                .createdBy(actorId)
                .changeReason(request.getChangeReason())
                .build();

        PolicyVersion saved = policyVersionRepository.save(policyVersion);

        // Keep cache in sync
        policyCache.put(key, saved.getValueJson());

        log.info("Policy updated: key={}, version={}, actor={}", key, nextVersion, actorId);
        return PolicyVersionResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Startup cache warm-up (called by DataSeeder)
    // -------------------------------------------------------------------------

    /**
     * Seeds default values for every {@link PolicyKey} that has no existing record,
     * then populates the in-memory cache for all keys.
     *
     * <p>
     * Called once at application startup by
     * {@link uz.pdp.online_university.config.DataSeeder}.
     */
    @Transactional
    public void seedDefaults() {
        for (PolicyKey key : PolicyKey.values()) {
            if (!policyVersionRepository.existsByPolicyKey(key)) {
                PolicyVersion defaultVersion = PolicyVersion.builder()
                        .policyKey(key)
                        .version(1)
                        .valueJson(defaultValue(key))
                        .createdBy(null)
                        .changeReason("Initial system default")
                        .build();
                policyVersionRepository.save(defaultVersion);
                log.info("Seeded default policy: key={}, value={}", key, defaultValue(key));
            }
        }

        // Warm up cache for all keys
        Arrays.stream(PolicyKey.values())
                .forEach(key -> policyVersionRepository.findTopByPolicyKeyOrderByVersionDesc(key)
                        .ifPresent(pv -> policyCache.put(key, pv.getValueJson())));
        log.info("PolicyCache warmed up for {} keys", PolicyKey.values().length);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String defaultValue(PolicyKey key) {
        return switch (key) {
            case ATTENDANCE_THRESHOLD -> "80";
            case EXAM_ELIGIBILITY_MIN_ATTENDANCE -> "75";
            case EXAM_ELIGIBILITY_REQUIRE_NO_DEBT -> "true";
            case DATA_RETENTION_DAYS -> "365";
        };
    }
}
