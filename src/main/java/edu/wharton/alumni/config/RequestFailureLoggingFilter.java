package edu.wharton.alumni.config;

import edu.wharton.alumni.security.JwtUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestFailureLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestFailureLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Instant startedAt = Instant.now();
        String requestId = requestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        Throwable failure = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Throwable exception) {
            failure = exception;
            throw exception;
        } finally {
            int status = response.getStatus();
            if (failure != null && status < 400) {
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            if (status < 200 || status >= 300) {
                log.warn(
                        "api_request_failed requestId={} method={} path={} query={} status={} durationMs={} userId={} remoteAddr={} userAgent=\"{}\" exception={}",
                        requestId,
                        request.getMethod(),
                        request.getRequestURI(),
                        valueOrDash(request.getQueryString()),
                        status,
                        Duration.between(startedAt, Instant.now()).toMillis(),
                        userId(),
                        request.getRemoteAddr(),
                        valueOrDash(request.getHeader("User-Agent")),
                        failure == null ? "-" : failure.getClass().getSimpleName() + ": " + failure.getMessage()
                );
            }
        }
    }

    private String requestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header;
    }

    private String userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUser user) {
            return user.id().toString();
        }
        return "-";
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
