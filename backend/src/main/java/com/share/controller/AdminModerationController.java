package com.share.controller;

import com.share.dto.AdminModerationRequest;
import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.AdminModerationService;
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
/**
 * 管理端审核治理入口：
 * - 内容审核：查询、下架、恢复。
 * - 评论审核：查询、隐藏、恢复。
 */
public class AdminModerationController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final AdminModerationService adminModerationService;

    public AdminModerationController(CurrentUserService currentUserService,
                                     AdminAccessService adminAccessService,
                                     AdminModerationService adminModerationService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.adminModerationService = adminModerationService;
    }

    // 内容审核列表：支持状态、审核状态、作者等筛选。
    @GetMapping({"/admin/contents", "/api/admin/contents"})
    public ApiResponse getContents(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) String reviewStatus,
                                   @RequestParam(required = false) Long userId) {
        Long operatorId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(operatorId, AdminPermissionCodes.CONTENT_READ);
        return ApiResponse.success(adminModerationService.getContents(page, pageSize, keyword, status, reviewStatus, userId));
    }

    // 下架内容（软治理，不物理删除）。
    @PostMapping({"/admin/contents/{id}/off-shelf", "/api/admin/contents/{id}/off-shelf"})
    public ApiResponse offShelfContent(@PathVariable("id") Long contentId,
                                       @Valid @RequestBody(required = false) AdminModerationRequest request,
                                       HttpServletRequest servletRequest) {
        Long operatorId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(operatorId, AdminPermissionCodes.CONTENT_OFF_SHELF);
        String reason = request == null ? null : request.getReason();
        return ApiResponse.success(adminModerationService.offShelfContent(
                operatorId,
                contentId,
                reason,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 恢复内容上架。
    @PostMapping({"/admin/contents/{id}/restore", "/api/admin/contents/{id}/restore"})
    public ApiResponse restoreContent(@PathVariable("id") Long contentId,
                                      @Valid @RequestBody(required = false) AdminModerationRequest request,
                                      HttpServletRequest servletRequest) {
        Long operatorId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(operatorId, AdminPermissionCodes.CONTENT_RESTORE);
        String reason = request == null ? null : request.getReason();
        return ApiResponse.success(adminModerationService.restoreContent(
                operatorId,
                contentId,
                reason,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 评论审核列表。
    @GetMapping({"/admin/comments", "/api/admin/comments"})
    public ApiResponse getComments(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                   @RequestParam(required = false) Long contentId,
                                   @RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) String reviewStatus) {
        Long operatorId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(operatorId, AdminPermissionCodes.COMMENT_READ);
        return ApiResponse.success(adminModerationService.getComments(page, pageSize, contentId, userId, reviewStatus));
    }

    @PostMapping({"/admin/comments/{id}/hide", "/api/admin/comments/{id}/hide"})
    public ApiResponse hideComment(@PathVariable("id") Long commentId,
                                   @Valid @RequestBody(required = false) AdminModerationRequest request,
                                   HttpServletRequest servletRequest) {
        Long operatorId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(operatorId, AdminPermissionCodes.COMMENT_HIDE);
        String reason = request == null ? null : request.getReason();
        return ApiResponse.success(adminModerationService.hideComment(
                operatorId,
                commentId,
                reason,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping({"/admin/comments/{id}/restore", "/api/admin/comments/{id}/restore"})
    public ApiResponse restoreComment(@PathVariable("id") Long commentId,
                                      @Valid @RequestBody(required = false) AdminModerationRequest request,
                                      HttpServletRequest servletRequest) {
        Long operatorId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(operatorId, AdminPermissionCodes.COMMENT_HIDE);
        String reason = request == null ? null : request.getReason();
        return ApiResponse.success(adminModerationService.restoreComment(
                operatorId,
                commentId,
                reason,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }
}
