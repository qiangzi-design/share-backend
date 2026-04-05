package com.share.controller;

import com.share.dto.AdminAnnouncementRequest;
import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/announcements", "/api/admin/announcements"})
/**
 * 管理端公告管理入口：
 * - 承载公告的查询、创建、编辑、发布、下线。
 * - 所有写操作都记录审计日志，便于追溯公告变更责任。
 */
public class AdminAnnouncementController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final AnnouncementService announcementService;

    public AdminAnnouncementController(CurrentUserService currentUserService,
                                       AdminAccessService adminAccessService,
                                       AnnouncementService announcementService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.announcementService = announcementService;
    }

    // 公告列表支持按状态与关键词筛选。
    @GetMapping
    public ApiResponse getAnnouncements(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "20") Integer pageSize,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANNOUNCEMENT_READ);
        return ApiResponse.success(announcementService.getAdminAnnouncements(page, pageSize, status, keyword));
    }

    // 新建公告先进入草稿态，显式发布后才会触达用户端。
    @PostMapping
    public ApiResponse createAnnouncement(@Valid @RequestBody AdminAnnouncementRequest request,
                                          HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANNOUNCEMENT_WRITE);
        return ApiResponse.success(announcementService.createAnnouncement(
                userId,
                request.getTitle(),
                request.getBody(),
                request.getIsPinned(),
                request.getStartTime(),
                request.getEndTime(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 编辑公告不会自动发布，避免误改后立即影响线上用户。
    @PutMapping("/{id}")
    public ApiResponse updateAnnouncement(@PathVariable("id") Long announcementId,
                                          @Valid @RequestBody AdminAnnouncementRequest request,
                                          HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANNOUNCEMENT_WRITE);
        return ApiResponse.success(announcementService.updateAnnouncement(
                userId,
                announcementId,
                request.getTitle(),
                request.getBody(),
                request.getIsPinned(),
                request.getStartTime(),
                request.getEndTime(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 发布后公告进入用户可见池，且按置顶/时间参与排序。
    @PostMapping("/{id}/publish")
    public ApiResponse publishAnnouncement(@PathVariable("id") Long announcementId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANNOUNCEMENT_WRITE);
        return ApiResponse.success(announcementService.publishAnnouncement(
                userId,
                announcementId,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 下线仅改变可见状态，不做物理删除，保留历史审计链路。
    @PostMapping("/{id}/offline")
    public ApiResponse offlineAnnouncement(@PathVariable("id") Long announcementId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANNOUNCEMENT_WRITE);
        return ApiResponse.success(announcementService.offlineAnnouncement(
                userId,
                announcementId,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }
}
