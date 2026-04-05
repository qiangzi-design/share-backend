package com.share.controller;

import com.share.dto.AdminBanRequest;
import com.share.dto.AdminMuteRequest;
import com.share.dto.AdminRiskMarkRequest;
import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/users", "/api/admin/users"})
/**
 * 管理端用户治理入口：
 * - 查询用户、查看详情、封禁/解封、禁言/解除禁言、风险标记。
 * - 所有治理动作都经过 RBAC 权限校验，服务层负责审计落库。
 */
public class AdminUserController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final AdminUserService adminUserService;

    public AdminUserController(CurrentUserService currentUserService,
                               AdminAccessService adminAccessService,
                               AdminUserService adminUserService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.adminUserService = adminUserService;
    }

    // 用户分页查询。
    @GetMapping
    public ApiResponse getUsers(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "20") Integer pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Integer status) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_READ);
        return ApiResponse.success(adminUserService.getUsers(page, pageSize, keyword, status));
    }

    // 用户详情含治理辅助信息（待审数量、粉丝关注摘要等）。
    @GetMapping("/{id}/detail")
    public ApiResponse getUserDetail(@PathVariable("id") Long targetUserId) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_READ);
        return ApiResponse.success(adminUserService.getUserDetail(targetUserId));
    }

    // 封禁用户：根据当前规则会触发强制下线。
    @PostMapping("/{id}/ban")
    public ApiResponse banUser(@PathVariable("id") Long targetUserId,
                               @Valid @RequestBody(required = false) AdminBanRequest request,
                               HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_BAN);
        String reason = request == null ? null : request.getReason();
        return ApiResponse.success(adminUserService.banUser(
                userId,
                targetUserId,
                reason,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 解封用户。
    @PostMapping("/{id}/unban")
    public ApiResponse unbanUser(@PathVariable("id") Long targetUserId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_BAN);
        return ApiResponse.success(adminUserService.unbanUser(
                userId,
                targetUserId,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 设置禁言（分钟）。
    @PostMapping("/{id}/mute")
    public ApiResponse muteUser(@PathVariable("id") Long targetUserId,
                                @Valid @RequestBody AdminMuteRequest request,
                                HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_MUTE);
        return ApiResponse.success(adminUserService.muteUser(
                userId,
                targetUserId,
                request.getMinutes(),
                request.getReason(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/{id}/unmute")
    public ApiResponse unmuteUser(@PathVariable("id") Long targetUserId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_MUTE);
        return ApiResponse.success(adminUserService.unmuteUser(
                userId,
                targetUserId,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/{id}/risk-mark")
    public ApiResponse markRisk(@PathVariable("id") Long targetUserId,
                                @Valid @RequestBody AdminRiskMarkRequest request,
                                HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_RISK_MARK);
        return ApiResponse.success(adminUserService.markRisk(
                userId,
                targetUserId,
                request.getRiskLevel(),
                request.getRiskNote(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/{id}/risk-unmark")
    public ApiResponse unmarkRisk(@PathVariable("id") Long targetUserId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.USER_RISK_MARK);
        return ApiResponse.success(adminUserService.unmarkRisk(
                userId,
                targetUserId,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }
}
