package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.AdminAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/audit-logs", "/api/admin/audit-logs"})
/**
 * 控制器职责：提供管理端审计日志查询入口。
 * 该控制器只做鉴权与参数透传，审计口径与分页规则统一在服务层维护，
 * 以保证不同管理页面读取到同一份可追溯数据。
 */
public class AdminAuditController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final AdminAuditService adminAuditService;

    public AdminAuditController(CurrentUserService currentUserService,
                                AdminAccessService adminAccessService,
                                AdminAuditService adminAuditService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.adminAuditService = adminAuditService;
    }

    /**
     * 查询管理动作审计日志。
     * 业务边界：只有具备 audit.read 权限的管理员才能访问。
     */
    @GetMapping
    public ApiResponse getAuditLogs(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer pageSize,
                                    @RequestParam(required = false) Long operatorUserId,
                                    @RequestParam(required = false) String action,
                                    @RequestParam(required = false) String targetType) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.AUDIT_READ);
        return ApiResponse.success(adminAuditService.getAuditLogs(page, pageSize, operatorUserId, action, targetType));
    }
}
