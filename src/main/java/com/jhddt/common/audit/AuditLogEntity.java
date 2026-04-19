package com.jhddt.common.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogEntity {
    private Long logId;
    private Long userId;
    private String username;
    private String action;
    private String targetType;
    private String targetId;
    private String requestMethod;
    private String requestPath;
    private String requestIp;
    private String userAgent;
    private String requestId;
    private Integer resultCode;
    private Integer success;
    private String errorMessage;
    private String beforeData;
    private String afterData;
    private LocalDateTime createdAt;
}
