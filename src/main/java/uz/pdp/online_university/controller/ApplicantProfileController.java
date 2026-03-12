package uz.pdp.online_university.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.pdp.online_university.dto.response.ApplicantProfileResponse;
import uz.pdp.online_university.dto.response.ApplicantStatusHistoryResponse;
import uz.pdp.online_university.entity.ApplicantProfile;
import uz.pdp.online_university.enums.ApplicantState;
import uz.pdp.online_university.repository.ApplicantStatusHistoryRepository;
import uz.pdp.online_university.security.CustomUserDetails;
import uz.pdp.online_university.service.ApplicantProfileService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applicants")
@RequiredArgsConstructor
@Tag(name = "Applicant Profile", description = "Endpoints for applicant profile and lifecycle management")
public class ApplicantProfileController {

    private final ApplicantProfileService profileService;
    private final ApplicantStatusHistoryRepository historyRepository;

    @GetMapping("/me/status")
    @PreAuthorize("hasRole('APPLICANT') or hasRole('ADMIN')")
    @Operation(summary = "Get my status", description = "Retrieves the current status of the authenticated applicant")
    public ResponseEntity<ApplicantProfileResponse> getMyStatus(@AuthenticationPrincipal CustomUserDetails principal) {
        ApplicantProfile profile = profileService.getProfileByUserId(principal.getId());
        return ResponseEntity.ok(mapToResponse(profile));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACADEMIC_DEPARTMENT')")
    @Operation(summary = "Get applicant history", description = "Retrieves the status transition history for a specific applicant")
    public ResponseEntity<List<ApplicantStatusHistoryResponse>> getHistory(@PathVariable Long id) {
        List<ApplicantStatusHistoryResponse> history = historyRepository.findByApplicantProfileIdOrderByTimestampDesc(id)
                .stream()
                .map(h -> ApplicantStatusHistoryResponse.builder()
                        .id(h.getId())
                        .oldState(h.getOldState())
                        .newState(h.getNewState())
                        .changedBy(h.getChangedBy())
                        .timestamp(h.getTimestamp())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACADEMIC_DEPARTMENT') or hasRole('APPLICANT')")
    @Operation(summary = "Change applicant status", description = "Updates the status of an applicant. Strict transition rules apply.")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam ApplicantState newState,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        // Additional business-level authorization checks
        if (newState == ApplicantState.VERIFIED && !isAdminOrAcademic(principal)) {
            return ResponseEntity.status(403).build();
        }

        if (newState == ApplicantState.ENROLLED && principal.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }

        profileService.changeStatus(id, newState, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private boolean isAdminOrAcademic(CustomUserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ACADEMIC_DEPARTMENT"));
    }

    private ApplicantProfileResponse mapToResponse(ApplicantProfile profile) {
        return ApplicantProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .currentState(profile.getCurrentState())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
