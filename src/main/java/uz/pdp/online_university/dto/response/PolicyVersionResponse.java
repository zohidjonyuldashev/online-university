package uz.pdp.online_university.dto.response;

import lombok.Builder;
import lombok.Data;
import uz.pdp.online_university.entity.PolicyVersion;
import uz.pdp.online_university.enums.PolicyKey;

import java.time.LocalDateTime;

@Data
@Builder
public class PolicyVersionResponse {

    private Long id;
    private PolicyKey policyKey;
    private int version;
    private String valueJson;
    private LocalDateTime createdAt;
    private Long createdBy;
    private String changeReason;

    public static PolicyVersionResponse from(PolicyVersion pv) {
        return PolicyVersionResponse.builder()
                .id(pv.getId())
                .policyKey(pv.getPolicyKey())
                .version(pv.getVersion())
                .valueJson(pv.getValueJson())
                .createdAt(pv.getCreatedAt())
                .createdBy(pv.getCreatedBy())
                .changeReason(pv.getChangeReason())
                .build();
    }
}
