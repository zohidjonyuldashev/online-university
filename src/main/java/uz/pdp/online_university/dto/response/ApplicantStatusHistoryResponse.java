package uz.pdp.online_university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.pdp.online_university.enums.ApplicantState;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicantStatusHistoryResponse {
    private Long id;
    private ApplicantState oldState;
    private ApplicantState newState;
    private Long changedBy;
    private LocalDateTime timestamp;
}
