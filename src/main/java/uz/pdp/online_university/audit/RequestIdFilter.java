package uz.pdp.online_university.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.pdp.online_university.enums.AuditSource;

import java.io.IOException;
import java.util.UUID;

/**
 * Generates a unique requestId per HTTP request and stores it in MDC (for log
 * correlation)
 * and in the RequestContext bean (for audit log entries).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "requestId";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";
    private static final String HEADER_SOURCE = "X-Audit-Source";

    private final RequestContext requestContext;

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        AuditSource source = resolveSource(request);

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            requestContext.setRequestId(requestId);
            requestContext.setSource(source);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private AuditSource resolveSource(HttpServletRequest request) {
        String sourceHeader = request.getHeader(HEADER_SOURCE);
        if (sourceHeader != null) {
            try {
                return AuditSource.valueOf(sourceHeader.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to default
            }
        }
        // Infer from path prefix
        String uri = request.getRequestURI();
        if (uri.startsWith("/web/") || uri.startsWith("/dashboard/")) {
            return AuditSource.UI;
        }
        return AuditSource.API;
    }
}
