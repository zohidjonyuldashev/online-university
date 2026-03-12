package uz.pdp.online_university.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderModulesRequest {

    @NotEmpty(message = "Module order list cannot be empty")
    private List<Long> moduleIds;
}
