package uz.pdp.online_university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.Notification;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);

    /** Used by the rate limiter to count recent sends in the sliding window. */
    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
