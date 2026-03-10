package uz.pdp.online_university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.enums.UserStatus;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (:status IS NULL OR u.status = :status)
            AND (:role IS NULL OR EXISTS (
                SELECT r FROM u.roles r WHERE r.name = :role
            ))
            """)
    Page<User> findAllWithFilters(
            @Param("search") String search,
            @Param("status") UserStatus status,
            @Param("role") RoleName role,
            Pageable pageable
    );

}
