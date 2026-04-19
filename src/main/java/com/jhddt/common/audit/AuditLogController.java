package com.jhddt.common.audit;

import com.jhddt.common.result.Result;
import com.jhddt.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;
    private final CurrentUser currentUser;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<AuditLogEntity>> listLogs(
            @RequestParam(defaultValue = "100") Integer limit,
            Authentication authentication) {
        try {
            Long userId = currentUser.id(authentication);
            int safeLimit = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);
            log.info("查询审计日志，userId={}, limit={}", userId, safeLimit);
            return Result.success("查询成功", auditLogMapper.selectRecent(safeLimit));
        } catch (Exception e) {
            log.error("查询审计日志失败，limit={}, error={}", limit, e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
