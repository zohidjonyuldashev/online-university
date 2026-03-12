package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.online_university.enums.ApplicantState;

import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicantStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_profile_id", nullable = false)
    private ApplicantProfile applicantProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_state", length = 30)
    private ApplicantState oldState;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", nullable = false, length = 30)
    private ApplicantState newState;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    public void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
