package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.dto.ReportCreateRequest;
import com.share.security.CurrentUserService;
import com.share.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/reports", "/api/reports"})
/**
 * 用户侧举报入口：
 * - 提供违规模板查询、提交举报、查看我的举报记录。
 * - 仅允许登录用户提交与查看，匿名用户不可进入举报链路。
 */
public class ReportController {

    private final CurrentUserService currentUserService;
    private final ReportService reportService;

    public ReportController(CurrentUserService currentUserService, ReportService reportService) {
        this.currentUserService = currentUserService;
        this.reportService = reportService;
    }

    // 举报模板用于前端下拉选择，避免把违规文案写死在页面中。
    @GetMapping("/templates")
    public ApiResponse getReportTemplates() {
        return ApiResponse.success(reportService.listReportTemplates());
    }

    // 提交举报会触发频控、重复举报校验和目标快照记录（服务层实现）。
    @PostMapping
    public ApiResponse createReport(@Valid @RequestBody ReportCreateRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(reportService.createReport(userId, request));
    }

    // 我的举报记录用于用户侧闭环可视化（待处理/处理中/已处理/已驳回）。
    @GetMapping("/my")
    public ApiResponse getMyReports(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "20") Integer pageSize,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String targetType) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(reportService.getMyReports(userId, page, pageSize, status, targetType));
    }
}
