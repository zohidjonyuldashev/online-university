package uz.pdp.online_university.service;

import org.springframework.data.domain.Pageable;
import uz.pdp.online_university.dto.request.CreateCourseRequest;
import uz.pdp.online_university.dto.request.UpdateCourseRequest;
import uz.pdp.online_university.dto.response.CourseResponse;
import uz.pdp.online_university.dto.response.PagedResponse;

public interface CourseService {

    CourseResponse create(CreateCourseRequest request);

    PagedResponse<CourseResponse> getAll(String search, 
                                         uz.pdp.online_university.enums.CourseStatus status, 
                                         Pageable pageable);

    CourseResponse getById(Long id);

    CourseResponse update(Long id, UpdateCourseRequest request);

    CourseResponse submitForReview(Long id);

    CourseResponse publish(Long id);

    CourseResponse archive(Long id);
}
