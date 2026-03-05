package uz.pdp.online_university.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(name = "key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "description")
    private String description;

    @Column(name = "module", nullable = false, length = 50)
    private String module;
}
