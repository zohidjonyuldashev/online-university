package uz.pdp.online_university.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.pdp.online_university.audit.AuditLogService;
import uz.pdp.online_university.dto.request.AuditLogFilterRequest;
import uz.pdp.online_university.dto.response.AuditLogResponse;
import uz.pdp.online_university.dto.response.PagedResponse;
import uz.pdp.online_university.entity.AuditLog;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Admin-only REST API for the audit log console.
 * Accessible to: ADMIN and IT_OPERATIONS roles only.
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'IT_OPERATIONS')")
@Tag(name = "Audit Console", description = "View and export audit logs (Admin / IT Ops only)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Filtered, paginated audit log query.
     * All filter params are optional.
     * <p>
     * GET
     * /api/admin/audit?from=&to=&actor_id=&entity_type=&entity_id=&page=0&size=20
     */
    @GetMapping
    @Operation(summary = "Search audit logs", description = "Returns a paginated list of audit entries filtered by the given criteria.")
    public ResponseEntity<PagedResponse<AuditLogResponse>> search(
            @RequestParam(value = "actor_id", required = false) Long actorId,
            @RequestParam(value = "entity_type", required = false) String entityType,
            @RequestParam(value = "entity_id", required = false) String entityId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // Clamp page size to max 100
        int clampedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, clampedSize);

        AuditLogFilterRequest filter = buildFilter(actorId, entityType, entityId, from, to);
        Page<AuditLog> resultPage = auditLogService.search(filter, pageable);

        PagedResponse<AuditLogResponse> response = PagedResponse.of(
                resultPage.map(AuditLogResponse::from));

        return ResponseEntity.ok(response);
    }

    /**
     * Export audit logs as CSV for the given filter.
     * <p>
     * POST /api/admin/audit/export
     */
    @PostMapping("/export")
    @Operation(summary = "Export audit logs as CSV")
    public ResponseEntity<byte[]> exportCsv(@RequestBody AuditLogFilterRequest filter) {
        byte[] csv = auditLogService.exportCsv(filter);

        String filename = "audit_export_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now()) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AuditLogFilterRequest buildFilter(Long actorId, String entityType, String entityId,
                                              Instant from, Instant to) {
        AuditLogFilterRequest f = new AuditLogFilterRequest();
        f.setActorId(actorId);
        f.setEntityType(entityType);
        f.setEntityId(entityId);
        f.setFrom(from);
        f.setTo(to);
        return f;
    }
}
