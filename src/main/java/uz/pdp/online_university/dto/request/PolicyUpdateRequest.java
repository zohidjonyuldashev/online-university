package uz.pdp.online_university.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PolicyUpdateRequest {

    @NotBlank(message = "valueJson must not be blank")
    private String valueJson;

    @NotBlank(message = "changeReason must not be blank")
    private String changeReason;
}
