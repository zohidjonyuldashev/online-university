package uz.pdp.online_university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.online_university.entity.Course;
import uz.pdp.online_university.enums.CourseStatus;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByOwnerTeacherId(UUID ownerTeacherId);

    List<Course> findByStatus(CourseStatus status);

}
