package uz.pdp.online_university.audit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import uz.pdp.online_university.enums.AuditSource;

/**
 * Request-scoped bean holding correlation data for the current HTTP request.
 * Populated by RequestIdFilter at the start of each request.
 */
@Getter
@Setter
@Component
@RequestScope
public class RequestContext {

    private String requestId;
    private AuditSource source = AuditSource.API;
}
