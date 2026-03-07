package uz.pdp.online_university.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.pdp.online_university.dto.request.PolicyUpdateRequest;
import uz.pdp.online_university.dto.response.PolicyVersionResponse;
import uz.pdp.online_university.entity.PolicyVersion;
import uz.pdp.online_university.enums.PolicyKey;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.PolicyVersionRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyVersionRepository policyVersionRepository;

    @Mock
    private PolicyCache policyCache;

    @InjectMocks
    private PolicyService policyService;

    private PolicyVersion v1;
    private PolicyVersion v2;

    @BeforeEach
    void setUp() {
        v1 = PolicyVersion.builder()
                .id(1L)
                .policyKey(PolicyKey.ATTENDANCE_THRESHOLD)
                .version(1)
                .valueJson("80")
                .createdBy(null)
                .changeReason("Initial system default")
                .build();

        v2 = PolicyVersion.builder()
                .id(2L)
                .policyKey(PolicyKey.ATTENDANCE_THRESHOLD)
                .version(2)
                .valueJson("85")
                .createdBy(10L)
                .changeReason("Raised threshold")
                .build();
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update should create a new version with incremented version number")
    void update_createsNewVersionWithIncrementedNumber() {
        when(policyVersionRepository.findMaxVersionByPolicyKey(PolicyKey.ATTENDANCE_THRESHOLD))
                .thenReturn(1);
        when(policyVersionRepository.save(any(PolicyVersion.class))).thenReturn(v2);

        PolicyUpdateRequest request = new PolicyUpdateRequest();
        request.setValueJson("85");
        request.setChangeReason("Raised threshold");

        PolicyVersionResponse response = policyService.update(PolicyKey.ATTENDANCE_THRESHOLD, request, 10L);

        ArgumentCaptor<PolicyVersion> captor = ArgumentCaptor.forClass(PolicyVersion.class);
        verify(policyVersionRepository).save(captor.capture());

        PolicyVersion saved = captor.getValue();
        assertThat(saved.getVersion()).isEqualTo(2);
        assertThat(saved.getValueJson()).isEqualTo("85");
        assertThat(saved.getChangeReason()).isEqualTo("Raised threshold");
        assertThat(saved.getCreatedBy()).isEqualTo(10L);
        assertThat(saved.getPolicyKey()).isEqualTo(PolicyKey.ATTENDANCE_THRESHOLD);

        // Cache should be updated
        verify(policyCache).put(PolicyKey.ATTENDANCE_THRESHOLD, v2.getValueJson());

        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getValueJson()).isEqualTo("85");
    }

    @Test
    @DisplayName("update from baseline 0 creates version 1")
    void update_firstVersionIsOne() {
        when(policyVersionRepository.findMaxVersionByPolicyKey(PolicyKey.DATA_RETENTION_DAYS))
                .thenReturn(0);
        PolicyVersion first = PolicyVersion.builder()
                .id(10L)
                .policyKey(PolicyKey.DATA_RETENTION_DAYS)
                .version(1)
                .valueJson("730")
                .createdBy(5L)
                .changeReason("Extended retention")
                .build();
        when(policyVersionRepository.save(any())).thenReturn(first);

        PolicyUpdateRequest request = new PolicyUpdateRequest();
        request.setValueJson("730");
        request.setChangeReason("Extended retention");

        PolicyVersionResponse response = policyService.update(PolicyKey.DATA_RETENTION_DAYS, request, 5L);
        assertThat(response.getVersion()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // getLatest
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getLatest returns latest version for existing key")
    void getLatest_returnsLatestVersion() {
        when(policyVersionRepository.findTopByPolicyKeyOrderByVersionDesc(PolicyKey.ATTENDANCE_THRESHOLD))
                .thenReturn(Optional.of(v2));

        PolicyVersionResponse response = policyService.getLatest(PolicyKey.ATTENDANCE_THRESHOLD);

        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getValueJson()).isEqualTo("85");
        assertThat(response.getPolicyKey()).isEqualTo(PolicyKey.ATTENDANCE_THRESHOLD);
    }

    @Test
    @DisplayName("getLatest throws ResourceNotFoundException when no version exists")
    void getLatest_throwsWhenNotFound() {
        when(policyVersionRepository.findTopByPolicyKeyOrderByVersionDesc(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getLatest(PolicyKey.EXAM_ELIGIBILITY_MIN_ATTENDANCE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getAllLatest
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllLatest returns one entry per policy key")
    void getAllLatest_returnsOnePerKey() {
        List<PolicyVersion> latestList = List.of(v2,
                PolicyVersion.builder()
                        .id(3L).policyKey(PolicyKey.EXAM_ELIGIBILITY_MIN_ATTENDANCE)
                        .version(1).valueJson("75").changeReason("default").build());

        when(policyVersionRepository.findLatestForAllKeys()).thenReturn(latestList);

        List<PolicyVersionResponse> result = policyService.getAllLatest();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPolicyKey()).isEqualTo(PolicyKey.ATTENDANCE_THRESHOLD);
        assertThat(result.get(1).getPolicyKey()).isEqualTo(PolicyKey.EXAM_ELIGIBILITY_MIN_ATTENDANCE);
    }

    // -------------------------------------------------------------------------
    // getHistory
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getHistory returns all versions ordered newest first")
    void getHistory_returnsAllVersionsOrdered() {
        when(policyVersionRepository.findAllByPolicyKeyOrderByVersionDesc(PolicyKey.ATTENDANCE_THRESHOLD))
                .thenReturn(List.of(v2, v1));

        List<PolicyVersionResponse> history = policyService.getHistory(PolicyKey.ATTENDANCE_THRESHOLD);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getVersion()).isEqualTo(2);
        assertThat(history.get(1).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("getHistory throws ResourceNotFoundException when no history exists")
    void getHistory_throwsWhenEmpty() {
        when(policyVersionRepository.findAllByPolicyKeyOrderByVersionDesc(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> policyService.getHistory(PolicyKey.DATA_RETENTION_DAYS))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // seedDefaults
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("seedDefaults inserts a row for each key that has no existing version")
    void seedDefaults_insertsAllMissingKeys() {
        // Simulate no rows exist for any key
        when(policyVersionRepository.existsByPolicyKey(any())).thenReturn(false);
        when(policyVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(policyVersionRepository.findTopByPolicyKeyOrderByVersionDesc(any()))
                .thenReturn(Optional.empty());

        policyService.seedDefaults();

        // Should save one row per PolicyKey
        verify(policyVersionRepository, times(PolicyKey.values().length)).save(any());
    }

    @Test
    @DisplayName("seedDefaults skips keys that already exist")
    void seedDefaults_skipsExistingKeys() {
        // All keys already exist
        when(policyVersionRepository.existsByPolicyKey(any())).thenReturn(true);
        when(policyVersionRepository.findTopByPolicyKeyOrderByVersionDesc(any()))
                .thenReturn(Optional.of(v1));

        policyService.seedDefaults();

        // No new rows should be saved
        verify(policyVersionRepository, never()).save(any());
        // Cache should still be warmed up for all keys
        verify(policyCache, times(PolicyKey.values().length)).put(any(), any());
    }
}
