package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.Enrollment;
import uz.pdp.online_university.enums.EnrollmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);

    @Query("SELECT e.courseId FROM Enrollment e WHERE e.studentId = :studentId AND e.status = :status")
    Set<Long> findCourseIdsByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status
    );

    @Query("SELECT e.courseId FROM Enrollment e WHERE e.studentId = :studentId")
    Set<Long> findAllCourseIdsByStudentId(@Param("studentId") Long studentId);

    List<Enrollment> findAllByStudentId(Long studentId);

    List<Enrollment> findAllByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    List<Enrollment> findAllByCourseId(Long courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
}
