package com.jhddt.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditAction)")
    public Object around(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        Long userId = null;
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long id) {
                userId = id;
            } else if (principal instanceof String text) {
                try {
                    userId = Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
            username = auth.getName();
        }

        try {
            Object result = joinPoint.proceed();
            writeLog(auditAction, request, userId, username, 1, null, result);
            return result;
        } catch (Throwable ex) {
            writeLog(auditAction, request, userId, username, 0, ex.getMessage(), null);
            throw ex;
        }
    }

    private void writeLog(AuditAction auditAction,
                          HttpServletRequest request,
                          Long userId,
                          String username,
                          int success,
                          String errorMessage,
                          Object result) {
        try {
            String requestId = request != null ? request.getHeader("X-Request-Id") : null;
            String afterData = result == null ? null : objectMapper.writeValueAsString(result);
            auditLogMapper.insert(AuditLogEntity.builder()
                    .userId(userId)
                    .username(username)
                    .action(auditAction.value())
                    .targetType(auditAction.targetType())
                    .requestMethod(request != null ? request.getMethod() : null)
                    .requestPath(request != null ? request.getRequestURI() : null)
                    .requestIp(request != null ? request.getRemoteAddr() : null)
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .requestId(requestId)
                    .success(success)
                    .resultCode(success == 1 ? 200 : 500)
                    .errorMessage(errorMessage)
                    .afterData(afterData)
                    .build());
        } catch (Exception e) {
            log.warn("写审计日志失败: {}", e.getMessage());
        }
    }
}
