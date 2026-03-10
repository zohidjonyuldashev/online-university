package uz.pdp.online_university.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.pdp.online_university.dto.request.PolicyUpdateRequest;
import uz.pdp.online_university.dto.response.PolicyVersionResponse;
import uz.pdp.online_university.enums.PolicyKey;
import uz.pdp.online_university.service.PolicyService;
import uz.pdp.online_university.security.CustomUserDetails;

import java.util.List;

/**
 * Admin-only REST API for managing system policies.
 *
 * <p>
 * All endpoints require the {@code ADMIN} role.
 * Every POST creates a new immutable version — no updates or deletes.
 *
 * <pre>
 * GET  /api/admin/policies                → latest version of all keys
 * GET  /api/admin/policies/{key}          → full version history for one key
 * POST /api/admin/policies/{key}          → create a new version for one key
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Policy Engine", description = "Manage versioned system policies (ADMIN only)")
public class PolicyController {

    private final PolicyService policyService;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns the latest version of every known policy key.
     */
    @GetMapping
    @Operation(summary = "List all policies (latest version)", description = "Returns one record per policy key, showing only the most recent version.")
    public ResponseEntity<List<PolicyVersionResponse>> getAllLatest() {
        return ResponseEntity.ok(policyService.getAllLatest());
    }

    /**
     * Returns the full version history for a single policy key (newest first).
     */
    @GetMapping("/{key}")
    @Operation(summary = "Get policy version history", description = "Returns all versions ever created for the given policy key, ordered newest first.")
    public ResponseEntity<List<PolicyVersionResponse>> getHistory(
            @PathVariable PolicyKey key) {
        return ResponseEntity.ok(policyService.getHistory(key));
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Creates a new version for the specified policy key.
     * The previous version is NOT modified — it remains in the history.
     */
    @PostMapping("/{key}")
    @Operation(summary = "Update a policy (creates new version)", description = "Appends a new, immutable version for the given policy key. "
            +
            "The changeReason field is mandatory for audit purposes.")
    public ResponseEntity<PolicyVersionResponse> update(
            @PathVariable PolicyKey key,
            @Valid @RequestBody PolicyUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long actorId = (principal != null) ? principal.getId() : null;
        PolicyVersionResponse created = policyService.update(key, request, actorId);

        return ResponseEntity
                .status(201)
                .header("Location", "/api/admin/policies/" + key)
                .body(created);
    }
}
