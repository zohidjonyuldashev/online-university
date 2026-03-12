package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.entity.ApplicantProfile;
import uz.pdp.online_university.entity.ApplicantStatusHistory;
import uz.pdp.online_university.enums.ApplicantState;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.ApplicantProfileRepository;
import uz.pdp.online_university.repository.ApplicantStatusHistoryRepository;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantProfileService {

    private final ApplicantProfileRepository profileRepository;
    private final ApplicantStatusHistoryRepository historyRepository;

    private static final Map<ApplicantState, Set<ApplicantState>> VALID_TRANSITIONS = new EnumMap<>(ApplicantState.class);

    static {
        VALID_TRANSITIONS.put(ApplicantState.APPLIED, EnumSet.of(ApplicantState.DOCS_PENDING));
        VALID_TRANSITIONS.put(ApplicantState.DOCS_PENDING, EnumSet.of(ApplicantState.DOCS_IN_REVIEW));
        VALID_TRANSITIONS.put(ApplicantState.DOCS_IN_REVIEW, EnumSet.of(ApplicantState.VERIFIED, ApplicantState.REJECTED_DOCS));
        VALID_TRANSITIONS.put(ApplicantState.REJECTED_DOCS, EnumSet.of(ApplicantState.DOCS_PENDING));
        VALID_TRANSITIONS.put(ApplicantState.VERIFIED, EnumSet.of(ApplicantState.EXAM_SCHEDULED));
        VALID_TRANSITIONS.put(ApplicantState.EXAM_SCHEDULED, EnumSet.of(ApplicantState.EXAM_IN_PROGRESS));
        VALID_TRANSITIONS.put(ApplicantState.EXAM_IN_PROGRESS, EnumSet.of(ApplicantState.EXAM_COMPLETED));
        VALID_TRANSITIONS.put(ApplicantState.EXAM_COMPLETED, EnumSet.of(ApplicantState.PASSED, ApplicantState.FAILED, ApplicantState.UNDER_REVIEW));
        VALID_TRANSITIONS.put(ApplicantState.PASSED, EnumSet.of(ApplicantState.ENROLLED));
    }

    @Transactional
    public void changeStatus(Long profileId, ApplicantState newState, Long changedBy) {
        ApplicantProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicantProfile", "id", profileId));

        ApplicantState oldState = profile.getCurrentState();

        if (oldState == newState) {
            return;
        }

        validateTransition(oldState, newState);

        profile.setCurrentState(newState);
        profileRepository.save(profile);

        ApplicantStatusHistory history = ApplicantStatusHistory.builder()
                .applicantProfile(profile)
                .oldState(oldState)
                .newState(newState)
                .changedBy(changedBy)
                .build();
        historyRepository.save(history);

        log.info("Applicant profile {} changed status from {} to {} by user {}", profileId, oldState, newState, changedBy);
    }

    private void validateTransition(ApplicantState oldState, ApplicantState newState) {
        Set<ApplicantState> allowed = VALID_TRANSITIONS.get(oldState);
        if (allowed == null || !allowed.contains(newState)) {
            throw new IllegalArgumentException("Invalid state transition from " + oldState + " to " + newState);
        }
    }

    @Transactional(readOnly = true)
    public ApplicantProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicantProfile", "userId", userId));
    }
}
