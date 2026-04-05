package com.share.controller;

import com.share.dto.AdminReportAssignRequest;
import com.share.dto.AdminReportHandleRequest;
import com.share.dto.AdminReportResolveRequest;
import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.ReportService;
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
@RequestMapping({"/admin/reports", "/api/admin/reports"})
/**
 * 管理端举报控制器。
 * 负责工单查询、指派、处理与目标预览，所有动作均要求管理员权限校验。
 */
public class AdminReportController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final ReportService reportService;

    public AdminReportController(CurrentUserService currentUserService,
                                 AdminAccessService adminAccessService,
                                 ReportService reportService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse getReports(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String targetType) {
        // 读权限可查看工单列表，但不能执行处理动作。
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_READ);
        return ApiResponse.success(reportService.getReports(page, pageSize, status, targetType));
    }

    @GetMapping("/{id}/target-preview")
    public ApiResponse getTargetPreview(@PathVariable("id") Long reportId) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_READ);
        return ApiResponse.success(reportService.getReportTargetPreview(reportId));
    }

    @PostMapping("/{id}/assign")
    public ApiResponse assignReport(@PathVariable("id") Long reportId,
                                    @Valid @RequestBody AdminReportAssignRequest request,
                                    HttpServletRequest servletRequest) {
        // 指派动作会写审计日志，便于后续追踪责任链路。
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        return ApiResponse.success(reportService.assignReport(
                userId,
                reportId,
                request.getAssigneeUserId(),
                request.getHandleNote(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/{id}/handle")
    public ApiResponse handleReport(@PathVariable("id") Long reportId,
                                    @Valid @RequestBody AdminReportHandleRequest request,
                                    HttpServletRequest servletRequest) {
        // 统一处理入口：有效举报可执行治理动作，无效举报直接驳回。
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        return ApiResponse.success(reportService.handleReport(
                userId,
                reportId,
                request.getDecision(),
                request.getAction(),
                request.getViolationTemplateCode(),
                request.getViolationTemplateLabel(),
                request.getViolationReason(),
                request.getHandleNote(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse resolveReport(@PathVariable("id") Long reportId,
                                     @Valid @RequestBody(required = false) AdminReportResolveRequest request,
                                     HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        String action = request == null ? null : request.getAction();
        String handleNote = request == null ? null : request.getHandleNote();
        return ApiResponse.success(reportService.resolveReport(
                userId,
                reportId,
                action,
                handleNote,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse rejectReport(@PathVariable("id") Long reportId,
                                    @Valid @RequestBody(required = false) AdminReportResolveRequest request,
                                    HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        String handleNote = request == null ? null : request.getHandleNote();
        return ApiResponse.success(reportService.rejectReport(
                userId,
                reportId,
                handleNote,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }
}
