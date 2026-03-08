package uz.pdp.online_university.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Table(name = "teacher_assignments", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_teacher_course_group",
                columnNames = {"teacher_id", "course_id", "group_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAssignment extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "group_id")
    private Long groupId;
}
