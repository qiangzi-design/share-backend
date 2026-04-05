package com.share.service;

import com.share.dto.PageResult;
import com.share.dto.ReportCreateRequest;

import java.util.List;
import java.util.Map;

public interface ReportService {

    Map<String, Object> createReport(Long reporterId, ReportCreateRequest request);

    List<Map<String, Object>> listReportTemplates();

    PageResult<Map<String, Object>> getMyReports(Long reporterId,
                                                 Integer page,
                                                 Integer pageSize,
                                                 String status,
                                                 String targetType);

    PageResult<Map<String, Object>> getReports(Integer page,
                                               Integer pageSize,
                                               String status,
                                               String targetType);

    Map<String, Object> assignReport(Long operatorUserId,
                                     Long reportId,
                                     Long assigneeUserId,
                                     String handleNote,
                                     String ip,
                                     String userAgent);

    Map<String, Object> handleReport(Long operatorUserId,
                                     Long reportId,
                                     String decision,
                                     String action,
                                     String violationTemplateCode,
                                     String violationTemplateLabel,
                                     String violationReason,
                                     String handleNote,
                                     String ip,
                                     String userAgent);

    Map<String, Object> resolveReport(Long operatorUserId,
                                      Long reportId,
                                      String action,
                                      String handleNote,
                                      String ip,
                                      String userAgent);

    Map<String, Object> rejectReport(Long operatorUserId,
                                     Long reportId,
                                     String handleNote,
                                     String ip,
                                     String userAgent);

    Map<String, Object> getReportTargetPreview(Long reportId);
}
