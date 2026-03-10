package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.TeacherAssignment;

import java.util.List;
import java.util.Set;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    boolean existsByTeacherIdAndCourseId(Long teacherId, Long courseId);

    boolean existsByTeacherIdAndGroupId(Long teacherId, Long groupId);

    boolean existsByTeacherIdAndCourseIdAndGroupId(Long teacherId, Long courseId, Long groupId);

    @Query("SELECT DISTINCT ta.courseId FROM TeacherAssignment ta WHERE ta.teacherId = :teacherId")
    Set<Long> findCourseIdsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT ta.groupId FROM TeacherAssignment ta WHERE ta.teacherId = :teacherId AND ta.groupId IS NOT NULL")
    Set<Long> findGroupIdsByTeacherId(@Param("teacherId") Long teacherId);

    List<TeacherAssignment> findAllByTeacherId(Long teacherId);

    List<TeacherAssignment> findAllByCourseId(Long courseId);

    List<TeacherAssignment> findAllByGroupId(Long groupId);

    void deleteByTeacherIdAndCourseId(Long teacherId, Long courseId);

    void deleteByTeacherIdAndCourseIdAndGroupId(Long teacherId, Long courseId, Long groupId);
}
