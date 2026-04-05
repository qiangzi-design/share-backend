package com.share.controller;

import com.share.dto.AdminReportTemplateRequest;
import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.ReportViolationTemplateService;
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
@RequestMapping({"/admin/report-templates", "/api/admin/report-templates"})
/**
 * 控制器职责：违规举报模板管理入口。
 * 管理端通过该控制器维护“可选举报原因模板”，用户端举报表单据此动态渲染。
 */
public class AdminReportTemplateController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final ReportViolationTemplateService reportViolationTemplateService;

    public AdminReportTemplateController(CurrentUserService currentUserService,
                                         AdminAccessService adminAccessService,
                                         ReportViolationTemplateService reportViolationTemplateService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.reportViolationTemplateService = reportViolationTemplateService;
    }

    /**
     * 分页查询举报模板。
     * 只读权限即可访问，便于审核员统一查看当前模板配置。
     */
    @GetMapping
    public ApiResponse getTemplates(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer pageSize,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer status) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_READ);
        return ApiResponse.success(reportViolationTemplateService.getTemplates(page, pageSize, keyword, status));
    }

    /**
     * 新增举报模板。
     * 关键点：写操作需要 report.handle 权限，同时记录请求来源用于审计追踪。
     */
    @PostMapping
    public ApiResponse createTemplate(@Valid @RequestBody AdminReportTemplateRequest request,
                                      HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        return ApiResponse.success(reportViolationTemplateService.createTemplate(
                userId,
                request.getCode(),
                request.getLabel(),
                request.getDescription(),
                request.getSortOrder(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    /**
     * 修改举报模板内容。
     * 说明：支持调整 code/label/描述/排序，不直接删除历史模板以保障历史举报可追溯。
     */
    @PutMapping("/{id}")
    public ApiResponse updateTemplate(@PathVariable("id") Long templateId,
                                      @Valid @RequestBody AdminReportTemplateRequest request,
                                      HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        return ApiResponse.success(reportViolationTemplateService.updateTemplate(
                userId,
                templateId,
                request.getCode(),
                request.getLabel(),
                request.getDescription(),
                request.getSortOrder(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    /**
     * 启用模板：启用后会重新出现在用户举报可选项中。
     */
    @PostMapping("/{id}/enable")
    public ApiResponse enableTemplate(@PathVariable("id") Long templateId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        return ApiResponse.success(reportViolationTemplateService.changeTemplateStatus(
                userId,
                templateId,
                1,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    /**
     * 停用模板：历史举报记录保留，新的举报不再展示该模板。
     */
    @PostMapping("/{id}/disable")
    public ApiResponse disableTemplate(@PathVariable("id") Long templateId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.REPORT_HANDLE);
        return ApiResponse.success(reportViolationTemplateService.changeTemplateStatus(
                userId,
                templateId,
                0,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }
}
