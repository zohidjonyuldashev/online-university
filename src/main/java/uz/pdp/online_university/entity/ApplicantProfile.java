package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.online_university.enums.ApplicantState;

import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicantProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 30)
    private ApplicantState currentState;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
