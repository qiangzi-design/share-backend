package com.share.controller;

import com.share.dto.AdminTemplateUpdateRequest;
import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.NotificationTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/notification-templates", "/api/admin/notification-templates"})
/**
 * 管理端站内消息模板入口：
 * - 维护治理通知模板（封禁/禁言/下架等）。
 * - 模板变更统一经权限校验和审计记录，避免线上文案失控。
 */
public class AdminNotificationTemplateController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final NotificationTemplateService notificationTemplateService;

    public AdminNotificationTemplateController(CurrentUserService currentUserService,
                                               AdminAccessService adminAccessService,
                                               NotificationTemplateService notificationTemplateService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.notificationTemplateService = notificationTemplateService;
    }

    // 模板列表只读查询，供管理端配置页展示。
    @GetMapping
    public ApiResponse getTemplates() {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TEMPLATE_READ);
        return ApiResponse.success(notificationTemplateService.listTemplates());
    }

    // 按模板编码更新内容，编码作为稳定主键不依赖前端传入 ID。
    @PutMapping("/{code}")
    public ApiResponse updateTemplate(@PathVariable("code") String code,
                                      @Valid @RequestBody AdminTemplateUpdateRequest request,
                                      HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TEMPLATE_WRITE);
        return ApiResponse.success(notificationTemplateService.updateTemplate(
                userId,
                code,
                request.getName(),
                request.getTitleTemplate(),
                request.getBodyTemplate(),
                request.getStatus(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }
}
