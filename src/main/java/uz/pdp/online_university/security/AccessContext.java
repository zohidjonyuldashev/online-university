package uz.pdp.online_university.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.online_university.enums.EnrollmentStatus;
import uz.pdp.online_university.enums.RoleName;
import uz.pdp.online_university.repository.EnrollmentRepository;
import uz.pdp.online_university.repository.StudentGroupRepository;
import uz.pdp.online_university.repository.TeacherAssignmentRepository;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessContext {

    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentGroupRepository studentGroupRepository;

    private static final Set<RoleName> FULL_ACCESS_ROLES = Set.of(
            RoleName.ADMIN,
            RoleName.DEPUTY_DIRECTOR
    );

    private static final Set<RoleName> READ_ACCESS_ROLES = Set.of(
            RoleName.AQAD,
            RoleName.ACADEMIC_DEPARTMENT
    );

    public boolean canAccessCourse(Long courseId) {
        CurrentUser user = getCurrentUser();

        if (hasFullAccess(user)) {
            return true;
        }

        if (hasReadAccess(user)) {
            return true;
        }

        if (user.hasRole(RoleName.TEACHER)) {
            boolean allowed = teacherAssignmentRepository.existsByTeacherIdAndCourseId(
                    user.getId(), courseId);
            if (!allowed) {
                logDenied(user, "Course", courseId);
            }
            return allowed;
        }

        if (user.hasRole(RoleName.STUDENT)) {
            boolean allowed = enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                    user.getId(), courseId, EnrollmentStatus.ACTIVE);
            if (!allowed) {
                logDenied(user, "Course", courseId);
            }
            return allowed;
        }

        logDenied(user, "Course", courseId);
        return false;
    }

    public boolean canAccessLecture(Long courseId) {
        return canAccessCourse(courseId);
    }

    public boolean canAccessGroup(Long groupId) {
        CurrentUser user = getCurrentUser();

        if (hasFullAccess(user)) {
            return true;
        }

        if (hasReadAccess(user)) {
            return true;
        }

        if (user.hasRole(RoleName.TEACHER)) {
            boolean allowed = teacherAssignmentRepository.existsByTeacherIdAndGroupId(
                    user.getId(), groupId);
            if (!allowed) {
                logDenied(user, "Group", groupId);
            }
            return allowed;
        }

        if (user.hasRole(RoleName.STUDENT)) {
            boolean allowed = studentGroupRepository.existsByStudentIdAndGroupId(
                    user.getId(), groupId);
            if (!allowed) {
                logDenied(user, "Group", groupId);
            }
            return allowed;
        }

        logDenied(user, "Group", groupId);
        return false;
    }

    public boolean canAccessMaterial(Long courseId) {
        return canAccessCourse(courseId);
    }

    public boolean canAccessEnrollment(Long studentId) {
        CurrentUser user = getCurrentUser();

        if (hasFullAccess(user)) {
            return true;
        }

        if (hasReadAccess(user)) {
            return true;
        }

        if (user.hasRole(RoleName.TEACHER)) {
            Set<Long> teacherCourseIds = teacherAssignmentRepository
                    .findCourseIdsByTeacherId(user.getId());
            Set<Long> studentCourseIds = enrollmentRepository
                    .findAllCourseIdsByStudentId(studentId);

            boolean allowed = teacherCourseIds.stream()
                    .anyMatch(studentCourseIds::contains);
            if (!allowed) {
                logDenied(user, "Enrollment(studentId)", studentId);
            }
            return allowed;
        }

        if (user.hasRole(RoleName.STUDENT)) {
            boolean allowed = user.getId().equals(studentId);
            if (!allowed) {
                logDenied(user, "Enrollment(studentId)", studentId);
            }
            return allowed;
        }

        logDenied(user, "Enrollment(studentId)", studentId);
        return false;
    }

    public Set<Long> getAccessibleCourseIds() {
        CurrentUser user = getCurrentUser();

        if (hasFullAccess(user) || hasReadAccess(user)) {
            return null;
        }

        if (user.hasRole(RoleName.TEACHER)) {
            return teacherAssignmentRepository.findCourseIdsByTeacherId(user.getId());
        }

        if (user.hasRole(RoleName.STUDENT)) {
            return enrollmentRepository.findCourseIdsByStudentIdAndStatus(
                    user.getId(), EnrollmentStatus.ACTIVE);
        }

        return Set.of();
    }

    public Set<Long> getAccessibleGroupIds() {
        CurrentUser user = getCurrentUser();

        if (hasFullAccess(user) || hasReadAccess(user)) {
            return null;
        }

        if (user.hasRole(RoleName.TEACHER)) {
            return teacherAssignmentRepository.findGroupIdsByTeacherId(user.getId());
        }

        if (user.hasRole(RoleName.STUDENT)) {
            return studentGroupRepository.findGroupIdsByStudentId(user.getId());
        }

        return Set.of();
    }

    // ---- Internal Helpers ----

    private CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Authentication required");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return new CurrentUser(userDetails);
    }

    private boolean hasFullAccess(CurrentUser user) {
        return FULL_ACCESS_ROLES.stream().anyMatch(user::hasRole);
    }

    private boolean hasReadAccess(CurrentUser user) {
        return READ_ACCESS_ROLES.stream().anyMatch(user::hasRole);
    }

    private void logDenied(CurrentUser user, String resourceType, Long resourceId) {
        log.warn("ACCESS DENIED: User {} (id={}, roles={}) attempted to access {} with id={}",
                user.getEmail(), user.getId(), user.getRoleNames(), resourceType, resourceId);
    }
}
