package uz.pdp.online_university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.Course;
import uz.pdp.online_university.enums.CourseStatus;

import java.util.Set;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // ---- Filtered search (Admin/AQAD — sees all) ----

    @Query("""
            SELECT c FROM Course c
            WHERE (:search IS NULL
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (:status IS NULL OR c.status = :status)
            AND (:ownerTeacherId IS NULL OR c.ownerTeacherId = :ownerTeacherId)
            """)
    Page<Course> findAllWithFilters(
            @Param("search") String search,
            @Param("status") CourseStatus status,
            @Param("ownerTeacherId") Long ownerTeacherId,
            Pageable pageable
    );

    // ---- Scoped by accessible IDs (Teacher/Student) ----

    @Query("""
            SELECT c FROM Course c
            WHERE c.id IN :courseIds
            AND (:search IS NULL
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (:status IS NULL OR c.status = :status)
            """)
    Page<Course> findAllByIdInWithFilters(
            @Param("courseIds") Set<Long> courseIds,
            @Param("search") String search,
            @Param("status") CourseStatus status,
            Pageable pageable
    );

    // ---- Student-specific (only PUBLISHED + enrolled) ----

    @Query("""
            SELECT c FROM Course c
            WHERE c.id IN :courseIds
            AND c.status = 'PUBLISHED'
            AND (:search IS NULL
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Course> findPublishedByIdInWithSearch(
            @Param("courseIds") Set<Long> courseIds,
            @Param("search") String search,
            Pageable pageable
    );

    // ---- Counts ----

    long countByOwnerTeacherId(Long ownerTeacherId);

    long countByStatus(CourseStatus status);
}
