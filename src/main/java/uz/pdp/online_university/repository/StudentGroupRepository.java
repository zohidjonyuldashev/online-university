package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.online_university.entity.StudentGroup;

import java.util.List;
import java.util.Set;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    boolean existsByStudentIdAndGroupId(Long studentId, Long groupId);

    @Query("SELECT sg.groupId FROM StudentGroup sg WHERE sg.studentId = :studentId")
    Set<Long> findGroupIdsByStudentId(@Param("studentId") Long studentId);

    List<StudentGroup> findAllByStudentId(Long studentId);

    List<StudentGroup> findAllByGroupId(Long groupId);

    void deleteByStudentIdAndGroupId(Long studentId, Long groupId);

    long countByGroupId(Long groupId);
}
