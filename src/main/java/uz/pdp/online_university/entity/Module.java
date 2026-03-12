package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.online_university.entity.base.BaseEntity;
import uz.pdp.online_university.enums.ModuleStatus;

@Entity
@Table(name = "modules", indexes = {
        @Index(name = "idx_module_course", columnList = "course_id"),
        @Index(name = "idx_module_order", columnList = "course_id, order_index"),
        @Index(name = "idx_module_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module extends BaseEntity {

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ModuleStatus status = ModuleStatus.ACTIVE;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Soft delete check
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
