package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.online_university.entity.base.BaseEntity;
import uz.pdp.online_university.enums.CourseStatus;

@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_course_owner", columnList = "owner_teacher_id"),
        @Index(name = "idx_course_status", columnList = "status"),
        @Index(name = "idx_course_program", columnList = "program_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Column(name = "level", length = 50)
    private String level;

    @Column(name = "learning_outcomes", columnDefinition = "TEXT")
    private String learningOutcomes;

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;

    @Column(name = "grading_policy", columnDefinition = "TEXT")
    private String gradingPolicy;

    @Column(name = "owner_teacher_id", nullable = false)
    private Long ownerTeacherId;

    @Column(name = "program_id")
    private Long programId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Version
    @Column(name = "version")
    private Long version;
}
