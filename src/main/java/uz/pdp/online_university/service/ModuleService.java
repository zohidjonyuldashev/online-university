package uz.pdp.online_university.service;

import org.springframework.data.domain.Pageable;
import uz.pdp.online_university.dto.request.CreateModuleRequest;
import uz.pdp.online_university.dto.request.ReorderModulesRequest;
import uz.pdp.online_university.dto.request.UpdateModuleRequest;
import uz.pdp.online_university.dto.response.ModuleResponse;
import uz.pdp.online_university.dto.response.PagedResponse;

import java.util.List;

public interface ModuleService {

    ModuleResponse create(Long courseId, CreateModuleRequest request);

    List<ModuleResponse> getAllByCourseId(Long courseId);

    PagedResponse<ModuleResponse> getAllByCourseIdPaged(Long courseId, Pageable pageable);

    ModuleResponse getById(Long courseId, Long moduleId);

    ModuleResponse update(Long courseId, Long moduleId, UpdateModuleRequest request);

    void delete(Long courseId, Long moduleId);

    PagedResponse<ModuleResponse> reorder(Long courseId, ReorderModulesRequest request);
}
