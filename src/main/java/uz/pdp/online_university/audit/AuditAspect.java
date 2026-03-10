package uz.pdp.online_university.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uz.pdp.online_university.annotation.Auditable;
import uz.pdp.online_university.enums.AuditAction;
import uz.pdp.online_university.security.CustomUserDetails;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

/**
 * AOP aspect that intercepts methods annotated with {@link Auditable}
 * and automatically creates audit log entries.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final SensitiveFieldMasker masker;

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    @Around("@annotation(uz.pdp.online_university.annotation.Auditable)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        String entityType = auditable.entityType();
        AuditAction action = auditable.action();
        Object[] args = pjp.getArgs();

        // Capture before-snapshot for UPDATE / DELETE / STATE_CHANGE
        String beforeJson = null;
        if (action == AuditAction.UPDATE || action == AuditAction.DELETE || action == AuditAction.STATE_CHANGE) {
            Object beforeObj = resolveExpression(auditable.beforeExpression(), args, null, sig.getParameterNames());
            if (beforeObj == null && args.length > 0) {
                beforeObj = args[0]; // fallback: first argument
            }
            beforeJson = masker.mask(beforeObj);
        }

        // Execute the actual method
        Object result = pjp.proceed();

        // Capture after-snapshot for CREATE / UPDATE / STATE_CHANGE
        String afterJson = null;
        if (action != AuditAction.DELETE) {
            afterJson = masker.mask(result);
        }

        // Resolve entity ID from SpEL
        String entityId = "unknown";
        try {
            Object idObj = resolveExpression(auditable.entityIdExpression(), args, result, sig.getParameterNames());
            if (idObj != null) {
                entityId = idObj.toString();
            }
        } catch (Exception e) {
            log.warn("Could not resolve entityId for @Auditable on {}: {}", method.getName(), e.getMessage());
        }

        // Extract actor info from security context
        Long actorId = null;
        String actorRoles = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof CustomUserDetails ud) {
                actorId = ud.getId();
            }
            actorRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5)) // strip "ROLE_" prefix
                    .collect(Collectors.joining(", "));
        }

        auditLogService.logAsync(
                actorId,
                actorRoles,
                entityType,
                entityId,
                action,
                beforeJson,
                afterJson);

        return result;
    }

    private Object resolveExpression(String expression, Object[] args, Object result, String[] paramNames) {
        if (expression == null || expression.isBlank())
            return null;
        try {
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("result", result);
            // bind parameters by name and positional index
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
            }
            return PARSER.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            log.debug("SpEL evaluation failed for '{}': {}", expression, e.getMessage());
            return null;
        }
    }
}
