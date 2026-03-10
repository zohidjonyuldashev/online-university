package uz.pdp.online_university.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.pdp.online_university.enums.AccessState;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccessStateRequest {

    @NotNull(message = "Access state is required")
    private AccessState accessState;

    private String reason;
}
