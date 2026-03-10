package uz.pdp.online_university.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.pdp.online_university.dto.request.NotifyRequest;
import uz.pdp.online_university.dto.response.NotificationResponse;
import uz.pdp.online_university.service.NotificationService;
import uz.pdp.online_university.security.CustomUserDetails;

import java.util.Map;

/**
 * Two groups of endpoints:
 *
 * <ul>
 * <li>{@code /api/internal/notify} — service-to-service trigger (ADMIN
 * only)</li>
 * <li>{@code /api/me/notifications} — user-facing inbox (any authenticated
 * user)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Send and read notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // -------------------------------------------------------------------------
    // Internal (system / admin)
    // -------------------------------------------------------------------------

    /**
     * Sends a notification to a user.
     * Restricted to ADMIN role (internal service-to-service use).
     */
    @PostMapping("/api/internal/notify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send a notification (internal)", description = "Triggers a notification using the specified template, channel and variables. ADMIN only.")
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody NotifyRequest request) {
        NotificationResponse response = notificationService.send(request);
        return ResponseEntity.status(201).body(response);
    }

    // -------------------------------------------------------------------------
    // User inbox (/api/me/notifications)
    // -------------------------------------------------------------------------

    /**
     * Returns the caller's paginated notification inbox.
     */
    @GetMapping("/api/me/notifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notifications", description = "Returns paginated in-app notifications for the authenticated user.")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, clampedSize);
        return ResponseEntity.ok(notificationService.getMyNotifications(principal.getId(), pageable));
    }

    /**
     * Returns the number of unread in-app notifications for the caller.
     */
    @GetMapping("/api/me/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unread notification count", description = "Returns badge count of unread in-app notifications.")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal CustomUserDetails principal) {
        long count = notificationService.countUnread(principal.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Marks a specific notification as read.
     */
    @PostMapping("/api/me/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(notificationService.markRead(id, principal.getId()));
    }
}
