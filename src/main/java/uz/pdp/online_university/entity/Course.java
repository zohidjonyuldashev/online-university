package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.online_university.enums.CourseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String language;

    private String level;

    @Column(columnDefinition = "TEXT")
    private String learningOutcomes;

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    @Column(columnDefinition = "TEXT")
    private String gradingPolicy;

    @Column(nullable = false)
    private UUID ownerTeacherId;

    private UUID programId;

    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @Version
    private Long version;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
        if (status == null) {
            status = CourseStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
