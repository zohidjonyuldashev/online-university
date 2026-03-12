package uz.pdp.online_university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.Module;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    // ---- Not deleted ----

    @Query("""
            SELECT m FROM Module m
            WHERE m.courseId = :courseId
            AND m.deletedAt IS NULL
            ORDER BY m.orderIndex ASC
            """)
    List<Module> findByCourseIdOrderByIndex(@Param("courseId") Long courseId);

    @Query("""
            SELECT m FROM Module m
            WHERE m.courseId = :courseId
            AND m.deletedAt IS NULL
            ORDER BY m.orderIndex ASC
            """)
    Page<Module> findByCourseIdPagedOrderByIndex(
            @Param("courseId") Long courseId,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM Module m
            WHERE m.id = :id AND m.deletedAt IS NULL
            """)
    Optional<Module> findByIdNotDeleted(@Param("id") Long id);

    @Query("""
            SELECT m FROM Module m
            WHERE m.courseId = :courseId
            AND m.deletedAt IS NULL
            AND m.id = :id
            """)
    Optional<Module> findByIdAndCourseIdNotDeleted(
            @Param("id") Long id,
            @Param("courseId") Long courseId
    );

    // ---- Counts (for deletion validation) ----

    long countByCourseIdAndDeletedAtIsNull(Long courseId);

    // ---- Reordering ----

    @Query("""
            SELECT COALESCE(MAX(m.orderIndex), 0) FROM Module m
            WHERE m.courseId = :courseId AND m.deletedAt IS NULL
            """)
    Integer findMaxOrderIndexByCourseId(@Param("courseId") Long courseId);

    @Query("""
            SELECT m FROM Module m
            WHERE m.courseId = :courseId
            AND m.orderIndex >= :orderIndex
            AND m.deletedAt IS NULL
            ORDER BY m.orderIndex ASC
            """)
    List<Module> findModulesFromOrderIndex(
            @Param("courseId") Long courseId,
            @Param("orderIndex") Integer orderIndex
    );

    // ---- Soft delete operations ----

    @Modifying
    @Query("""
            UPDATE Module m
            SET m.deletedAt = :deletedAt, m.updatedAt = NOW()
            WHERE m.id = :id
            """)
    void softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Query("""
            SELECT COUNT(m) FROM Module m
            WHERE m.courseId = :courseId AND m.deletedAt IS NULL
            """)
    long countActiveModulesByCourseId(@Param("courseId") Long courseId);
}
