package uz.pdp.online_university.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.dto.request.CreateModuleRequest;
import uz.pdp.online_university.dto.request.ReorderModulesRequest;
import uz.pdp.online_university.dto.request.UpdateModuleRequest;
import uz.pdp.online_university.dto.response.ModuleResponse;
import uz.pdp.online_university.dto.response.PagedResponse;
import uz.pdp.online_university.entity.Course;
import uz.pdp.online_university.entity.Module;
import uz.pdp.online_university.exception.InvalidOperationException;
import uz.pdp.online_university.exception.ResourceNotFoundException;
import uz.pdp.online_university.repository.CourseRepository;
import uz.pdp.online_university.repository.ModuleRepository;
import uz.pdp.online_university.security.AccessContext;
import uz.pdp.online_university.security.CurrentUser;
import uz.pdp.online_university.security.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final AccessContext accessContext;

    // ---- Create ----

    @Override
    @Transactional
    public ModuleResponse create(Long courseId, CreateModuleRequest request) {
        CurrentUser currentUser = getCurrentUser();

        // Verify course exists and user can access it
        Course course = findCourseOrThrow(courseId);
        validateCourseAccess(courseId);

        // Get next order index
        Integer maxOrder = moduleRepository.findMaxOrderIndexByCourseId(courseId);
        Integer nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;

        Module module = Module.builder()
                .courseId(courseId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .orderIndex(nextOrder)
                .build();

        module = moduleRepository.save(module);

        log.info("Module created: '{}' (id={}, courseId={}, order={}) by user {}",
                module.getTitle(), module.getId(), courseId, nextOrder, currentUser.getEmail());

        return toResponse(module);
    }

    // ---- Get All (list) ----

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponse> getAllByCourseId(Long courseId) {
        validateCourseAccess(courseId);

        return moduleRepository.findByCourseIdOrderByIndex(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ModuleResponse> getAllByCourseIdPaged(Long courseId, Pageable pageable) {
        validateCourseAccess(courseId);

        Page<ModuleResponse> page = moduleRepository.findByCourseIdPagedOrderByIndex(courseId, pageable)
                .map(this::toResponse);

        return PagedResponse.of(page);
    }

    // ---- Get By ID ----

    @Override
    @Transactional(readOnly = true)
    public ModuleResponse getById(Long courseId, Long moduleId) {
        validateCourseAccess(courseId);

        Module module = moduleRepository.findByIdAndCourseIdNotDeleted(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        return toResponse(module);
    }

    // ---- Update ----

    @Override
    @Transactional
    public ModuleResponse update(Long courseId, Long moduleId, UpdateModuleRequest request) {
        CurrentUser currentUser = getCurrentUser();
        validateCourseAccess(courseId);

        Module module = moduleRepository.findByIdAndCourseIdNotDeleted(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        validateVersion(module, request.getVersion());

        if (request.getTitle() != null) {
            module.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            module.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            module.setStatus(request.getStatus());
        }

        try {
            module = moduleRepository.save(module);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new InvalidOperationException("Module was updated by another user. Please refresh and try again.");
        }

        log.info("Module updated: '{}' (id={}, courseId={}) by user {}",
                module.getTitle(), module.getId(), courseId, currentUser.getEmail());

        return toResponse(module);
    }

    // ---- Delete (soft) ----

    @Override
    @Transactional
    public void delete(Long courseId, Long moduleId) {
        CurrentUser currentUser = getCurrentUser();
        validateCourseAccess(courseId);

        Module module = moduleRepository.findByIdAndCourseIdNotDeleted(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        LocalDateTime now = LocalDateTime.now();
        moduleRepository.softDeleteById(moduleId, now, now);

        // Reorder remaining modules to close gaps
        reorderModulesAfterDeletion(courseId, module.getOrderIndex());

        log.info("Module deleted (soft): '{}' (id={}, courseId={}) by user {}",
                module.getTitle(), module.getId(), courseId, currentUser.getEmail());
    }


    // ---- Reorder ----

    @Override
    @Transactional
    public PagedResponse<ModuleResponse> reorder(Long courseId, ReorderModulesRequest request) {
        CurrentUser currentUser = getCurrentUser();
        validateCourseAccess(courseId);

        List<Long> moduleIds = request.getModuleIds();

        // Validate all modules belong to this course and exist
        List<Module> modules = moduleRepository.findByCourseIdOrderByIndex(courseId);
        if (modules.size() != moduleIds.size()) {
            throw new InvalidOperationException(
                    "Module count mismatch. Expected " + modules.size() + ", got " + moduleIds.size());
        }

        for (Long id : moduleIds) {
            if (modules.stream().noneMatch(m -> m.getId().equals(id))) {
                throw new ResourceNotFoundException("Module", "id", id);
            }
        }

        // Update order indices
        for (int i = 0; i < moduleIds.size(); i++) {
            int finalI = i;
            Module module = modules.stream()
                    .filter(m -> m.getId().equals(moduleIds.get(finalI)))
                    .findFirst()
                    .orElseThrow();

            module.setOrderIndex(i);
            try {
                moduleRepository.save(module);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new InvalidOperationException(
                        "Concurrent reorder detected. Module was modified by another user. Please try again.");
            }
        }

        log.info("Modules reordered in course {} by user {}: {}",
                courseId, currentUser.getEmail(), moduleIds);

        // Return updated modules in new order
        Page<ModuleResponse> page = moduleRepository.findByCourseIdPagedOrderByIndex(courseId, Pageable.unpaged())
                .map(this::toResponse);

        return PagedResponse.of(page);
    }

    // =========================================================================
    // Validation + Helper Methods
    // =========================================================================

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
    }

    private void validateCourseAccess(Long courseId) {
        // Use AccessContext to check if user can access this course
        if (!accessContext.canAccessCourse(courseId)) {
            throw new InvalidOperationException("You do not have access to this course");
        }
    }

    private void validateVersion(Module module, Long requestVersion) {
        if (requestVersion != null && !requestVersion.equals(module.getVersion())) {
            throw new InvalidOperationException(
                    "Module was updated by another user. Please refresh and try again.");
        }
    }

    private void reorderModulesAfterDeletion(Long courseId, Integer deletedOrderIndex) {
        // Get all modules with orderIndex >= deletedIndex
        List<Module> modulesToShift = moduleRepository.findModulesFromOrderIndex(courseId, deletedOrderIndex);

        // Shift down by 1
        for (Module m : modulesToShift) {
            m.setOrderIndex(m.getOrderIndex() - 1);
            moduleRepository.save(m);
        }

        log.debug("Reordered {} modules after deletion in course {}", modulesToShift.size(), courseId);
    }

    private CurrentUser getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return new CurrentUser(userDetails);
    }

    private ModuleResponse toResponse(Module module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .courseId(module.getCourseId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .status(module.getStatus())
                .version(module.getVersion())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }

}