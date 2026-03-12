package uz.pdp.online_university.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uz.pdp.online_university.enums.ModuleStatus;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModuleRequest {

    @Size(min = 2, max = 255, message = "Title must be between 2 and 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private ModuleStatus status;

    private Long version;
}
