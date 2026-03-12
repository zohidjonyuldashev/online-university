package uz.pdp.online_university.service;

import uz.pdp.online_university.dto.request.CreateCourseRequest;
import uz.pdp.online_university.dto.request.UpdateCourseRequest;
import uz.pdp.online_university.dto.response.CourseResponse;

import java.util.List;
import java.util.UUID;

public interface CourseService {

    CourseResponse create(CreateCourseRequest request);

    List<CourseResponse> getAll();

    CourseResponse getById(UUID id);

    CourseResponse update(UUID id, UpdateCourseRequest request);

    CourseResponse submitForReview(UUID id);

    CourseResponse publish(UUID id);

    CourseResponse archive(UUID id);

}
