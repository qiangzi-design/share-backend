package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.dto.PageResult;
import com.share.dto.ReportCreateRequest;
import com.share.entity.Comment;
import com.share.entity.Content;
import com.share.entity.Report;
import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.mapper.ReportMapper;
import com.share.service.AdminAuditService;
import com.share.service.CommentService;
import com.share.service.ContentService;
import com.share.service.NotificationService;
import com.share.service.NotificationTemplateService;
import com.share.service.ReportService;
import com.share.service.ReportViolationTemplateService;
import com.share.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * 举报工单服务。
 * 统一处理“创建举报 -> 管理员处置 -> 审计留痕 -> 通知回执”的完整闭环。
 * 这样可以保证规则集中，避免控制器或前端绕开业务约束直接改状态。
 */
public class ReportServiceImpl implements ReportService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int CREATE_LIMIT_WINDOW_MINUTES = 10;
    private static final int CREATE_LIMIT_MAX_COUNT = 20;

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_ASSIGNED = "assigned";
    private static final String STATUS_RESOLVED = "resolved";
    private static final String STATUS_REJECTED = "rejected";

    private static final String TARGET_CONTENT = "content";
    private static final String TARGET_COMMENT = "comment";
    private static final String TARGET_USER = "user";

    private static final String DECISION_VALID = "valid";
    private static final String DECISION_INVALID = "invalid";

    private static final String ACTION_REJECT_REPORT = "reject_report";
    private static final String ACTION_OFF_SHELF_CONTENT = "off_shelf_content";
    private static final String ACTION_HIDE_COMMENT = "hide_comment";
    private static final String ACTION_DELETE_COMMENT = "delete_comment";
    private static final String ACTION_VALID_NO_ACTION = "valid_no_action";

    private static final String REVIEW_REJECTED = "rejected";

    private static final Set<String> VALID_TARGET_TYPES = Set.of(TARGET_CONTENT, TARGET_COMMENT, TARGET_USER);
    private static final Set<String> VALID_STATUSES = Set.of(STATUS_PENDING, STATUS_ASSIGNED, STATUS_RESOLVED, STATUS_REJECTED);
    private static final Set<String> VALID_DECISIONS = Set.of(DECISION_VALID, DECISION_INVALID);

    private final ReportMapper reportMapper;
    private final ContentService contentService;
    private final CommentService commentService;
    private final UserService userService;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    private final ReportViolationTemplateService reportViolationTemplateService;
    private final ObjectMapper objectMapper;

    public ReportServiceImpl(ReportMapper reportMapper,
                             ContentService contentService,
                             CommentService commentService,
                             UserService userService,
                             AdminAuditService adminAuditService,
                             NotificationService notificationService,
                             NotificationTemplateService notificationTemplateService,
                             ReportViolationTemplateService reportViolationTemplateService,
                             ObjectMapper objectMapper) {
        this.reportMapper = reportMapper;
        this.contentService = contentService;
        this.commentService = commentService;
        this.userService = userService;
        this.adminAuditService = adminAuditService;
        this.notificationService = notificationService;
        this.notificationTemplateService = notificationTemplateService;
        this.reportViolationTemplateService = reportViolationTemplateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> listReportTemplates() {
        return reportViolationTemplateService.listActiveTemplates();
    }

    @Override
    public Map<String, Object> createReport(Long reporterId, ReportCreateRequest request) {
        // 登录态与入参兜底，避免非法请求进入后续业务流程。
        if (reporterId == null || reporterId < 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "Login required");
        }
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid request");
        }

        String targetType = normalizeOptional(request.getTargetType());
        if (targetType == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "targetType is required");
        }

        // 举报创建时就固化目标快照，后续即使目标被删除也能还原上下文。
        Map<String, Object> snapshot = validateAndBuildTargetSnapshot(targetType, request.getTargetId());
        // 三道保护：禁止自举报、时间窗限流、防止同目标重复未闭环工单。
        ensureNotSelfReport(reporterId, targetType, snapshot);
        ensureReportRateLimit(reporterId);
        ensureNoOpenDuplicate(reporterId, targetType, request.getTargetId());

        // 模板与自定义描述合并为统一理由，保证后台与通知展示口径一致。
        String finalReason = buildReportReason(request.getTemplateCode(), request.getTemplateLabel(), request.getReason());
        if (finalReason == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Please select a violation template or provide a description");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setReason(finalReason);
        report.setStatus(STATUS_PENDING);
        report.setTargetSnapshot(toJson(snapshot));
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.insert(report);

        Map<String, Object> result = new HashMap<>();
        result.put("id", report.getId());
        result.put("status", report.getStatus());
        result.put("targetType", report.getTargetType());
        result.put("targetId", report.getTargetId());
        result.put("reason", report.getReason());
        result.put("targetSnapshot", snapshot);
        result.put("createTime", report.getCreateTime());
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getMyReports(Long reporterId, Integer page, Integer pageSize, String status, String targetType) {
        // 用户侧“我的举报”只读取本人数据，支持状态和目标类型筛选。
        if (reporterId == null || reporterId < 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "Login required");
        }
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);
        String normalizedStatus = normalizeOptional(status);
        String normalizedTargetType = normalizeOptional(targetType);
        if (normalizedStatus != null && !VALID_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid report status");
        }
        if (normalizedTargetType != null && !VALID_TARGET_TYPES.contains(normalizedTargetType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid target type");
        }

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, reporterId);
        if (normalizedStatus != null) wrapper.eq(Report::getStatus, normalizedStatus);
        if (normalizedTargetType != null) wrapper.eq(Report::getTargetType, normalizedTargetType);
        wrapper.orderByDesc(Report::getCreateTime);

        Page<Report> pageData = reportMapper.selectPage(new Page<>(validPage, validPageSize), wrapper);
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(pageData.getRecords().stream().map(this::toMyReportView).toList());
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getReports(Integer page, Integer pageSize, String status, String targetType) {
        // 管理侧列表会把举报人/处理人信息一起组装，减少前端重复查询。
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);
        String normalizedStatus = normalizeOptional(status);
        String normalizedTargetType = normalizeOptional(targetType);

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (normalizedStatus != null) wrapper.eq(Report::getStatus, normalizedStatus);
        if (normalizedTargetType != null) wrapper.eq(Report::getTargetType, normalizedTargetType);
        wrapper.orderByDesc(Report::getCreateTime);

        Page<Report> pageData = reportMapper.selectPage(new Page<>(validPage, validPageSize), wrapper);
        Set<Long> userIds = pageData.getRecords().stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getReporterId(), item.getAssigneeId()))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(pageData.getRecords().stream().map(item -> toReportView(item, userMap)).toList());
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> assignReport(Long operatorUserId, Long reportId, Long assigneeUserId, String handleNote, String ip, String userAgent) {
        // 指派只负责“责任人变更 + 状态推进为 assigned”，不执行治理动作。
        Report report = mustGetReport(reportId);
        assertStatusCanAssign(report);
        User assignee = mustGetUser(assigneeUserId);
        Map<String, Object> before = toSnapshot(report);

        report.setAssigneeId(assignee.getId());
        report.setHandleNote(normalizeText(handleNote, 500));
        report.setStatus(STATUS_ASSIGNED);
        report.setHandleTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);

        Map<String, Object> after = toSnapshot(report);
        adminAuditService.log(operatorUserId, "admin.report.assign", "report", reportId, before, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    public Map<String, Object> handleReport(Long operatorUserId,
                                            Long reportId,
                                            String decision,
                                            String action,
                                            String violationTemplateCode,
                                            String violationTemplateLabel,
                                            String violationReason,
                                            String handleNote,
                                            String ip,
                                            String userAgent) {
        // 处理入口统一承载“有效/无效”分流，防止状态更新散落在多个接口。
        Report report = mustGetReport(reportId);
        assertStatusCanHandle(report);

        String normalizedDecision = normalizeOptional(decision);
        if (normalizedDecision == null || !VALID_DECISIONS.contains(normalizedDecision)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid decision");
        }

        String normalizedAction = normalizeOptional(action);
        String normalizedViolation = buildViolationReason(violationTemplateCode, violationTemplateLabel, violationReason);
        String normalizedHandleNote = normalizeText(handleNote, 500);

        Map<String, Object> before = toSnapshot(report);

        if (DECISION_INVALID.equals(normalizedDecision)) {
            // 无效举报：驳回工单，不触发内容/评论治理动作。
            report.setStatus(STATUS_REJECTED);
            report.setResolveAction(ACTION_REJECT_REPORT);
        } else {
            // 有效举报：执行对应治理动作并落审计日志。
            String appliedAction = applyValidAction(operatorUserId, report, normalizedAction, normalizedViolation, ip, userAgent);
            report.setStatus(STATUS_RESOLVED);
            report.setResolveAction(appliedAction);
        }

        report.setHandleNote(normalizedHandleNote);
        if (report.getAssigneeId() == null || report.getAssigneeId() < 1) {
            report.setAssigneeId(operatorUserId);
        }
        report.setHandleTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        reportMapper.updateById(report);

        Map<String, Object> after = toSnapshot(report);
        after.put("decision", normalizedDecision);
        adminAuditService.log(operatorUserId, "admin.report.handle", "report", reportId, before, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    public Map<String, Object> resolveReport(Long operatorUserId,
                                             Long reportId,
                                             String action,
                                             String handleNote,
                                             String ip,
                                             String userAgent) {
        return handleReport(operatorUserId, reportId, DECISION_VALID, action, null, null, null, handleNote, ip, userAgent);
    }

    @Override
    @Transactional
    public Map<String, Object> rejectReport(Long operatorUserId,
                                            Long reportId,
                                            String handleNote,
                                            String ip,
                                            String userAgent) {
        return handleReport(operatorUserId, reportId, DECISION_INVALID, ACTION_REJECT_REPORT, null, null, null, handleNote, ip, userAgent);
    }

    @Override
    public Map<String, Object> getReportTargetPreview(Long reportId) {
        Report report = mustGetReport(reportId);
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", report.getId());
        result.put("targetType", report.getTargetType());
        result.put("targetId", report.getTargetId());
        result.put("status", report.getStatus());
        result.put("snapshot", parseJsonMap(report.getTargetSnapshot()));
        result.put("current", fetchCurrentTargetPreview(report.getTargetType(), report.getTargetId()));
        return result;
    }

    private String applyValidAction(Long operatorUserId,
                                    Report report,
                                    String action,
                                    String violationReason,
                                    String ip,
                                    String userAgent) {
        // 目标类型与动作必须匹配，避免例如“用户举报去执行删评论”这类越权动作。
        String targetType = normalizeOptional(report.getTargetType());
        if (targetType == null || !VALID_TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid report target type");
        }

        if (TARGET_CONTENT.equals(targetType)) {
            if (ACTION_VALID_NO_ACTION.equals(action)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Content report must choose off shelf action");
            }
            String finalReason = violationReason == null ? normalizeText(report.getReason(), 500) : violationReason;
            if (finalReason == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Violation reason is required for content handling");
            }
            handleContentViolation(operatorUserId, report.getTargetId(), finalReason, ip, userAgent);
            return ACTION_OFF_SHELF_CONTENT;
        }

        if (TARGET_COMMENT.equals(targetType)) {
            String finalReason = violationReason == null ? normalizeText(report.getReason(), 500) : violationReason;
            if (ACTION_DELETE_COMMENT.equals(action)) {
                deleteComment(operatorUserId, report.getTargetId(), finalReason, ip, userAgent);
                return ACTION_DELETE_COMMENT;
            }
            hideComment(operatorUserId, report.getTargetId(), finalReason, ip, userAgent);
            return ACTION_HIDE_COMMENT;
        }

        if (TARGET_USER.equals(targetType)) {
            return ACTION_VALID_NO_ACTION;
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Unsupported report target type");
    }

    private void handleContentViolation(Long operatorUserId,
                                        Long contentId,
                                        String violationReason,
                                        String ip,
                                        String userAgent) {
        // 内容违规采用“审核驳回即下架”口径，不做物理删除，便于后续复核与申诉。
        if (contentId == null || contentId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid content id");
        }
        Content content = contentService.getById(contentId);
        if (content == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Content not found");
        }

        Map<String, Object> before = toContentSnapshot(content);
        content.setReviewStatus(REVIEW_REJECTED);
        content.setReviewReason(violationReason);
        content.setReviewerId(operatorUserId);
        content.setReviewTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        contentService.updateById(content);

        Map<String, Object> after = toContentSnapshot(content);
        adminAuditService.log(operatorUserId, "admin.content.off_shelf", "content", contentId, before, after, ip, userAgent);

        if (content.getUserId() != null && content.getUserId() > 0) {
            String contentTitle = normalizeText(content.getTitle(), 120);
            if (contentTitle == null) {
                contentTitle = "未命名作品";
            }
            Map<String, String> rendered = notificationTemplateService.render(
                    "CONTENT_OFF_SHELF",
                    Map.of("contentTitle", contentTitle, "reason", violationReason, "violation", violationReason),
                    "内容下架通知",
                    "你的作品《" + contentTitle + "》，涉嫌" + violationReason + "，现已下架"
            );
            notificationService.createSystemNotification(
                    content.getUserId(),
                    NotificationService.TYPE_SYSTEM_NOTICE,
                    rendered.get("title"),
                    rendered.get("body")
            );
        }
    }

    private void hideComment(Long operatorUserId, Long commentId, String reason, String ip, String userAgent) {
        Comment comment = mustGetComment(commentId);
        Map<String, Object> before = toCommentSnapshot(comment);

        String finalReason = reason == null ? "违规评论" : reason;
        comment.setReviewStatus(REVIEW_REJECTED);
        comment.setReviewReason(finalReason);
        comment.setReviewerId(operatorUserId);
        comment.setReviewTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        commentService.updateById(comment);

        Map<String, Object> after = toCommentSnapshot(comment);
        adminAuditService.log(operatorUserId, "admin.comment.hide", "comment", commentId, before, after, ip, userAgent);
    }

    private void deleteComment(Long operatorUserId, Long commentId, String reason, String ip, String userAgent) {
        Comment comment = mustGetComment(commentId);
        Map<String, Object> before = toCommentSnapshot(comment);

        String finalReason = reason == null ? "违规评论" : reason;
        comment.setStatus(0);
        comment.setReviewStatus(REVIEW_REJECTED);
        comment.setReviewReason(finalReason);
        comment.setReviewerId(operatorUserId);
        comment.setReviewTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        commentService.updateById(comment);

        Map<String, Object> after = toCommentSnapshot(comment);
        adminAuditService.log(operatorUserId, "admin.comment.delete", "comment", commentId, before, after, ip, userAgent);
    }

    private Map<String, Object> validateAndBuildTargetSnapshot(String targetType, Long targetId) {
        // 根据目标类型提取最小必要字段，避免快照过大但保证可追溯。
        if (targetType == null || targetType.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "targetType is required");
        }
        if (targetId == null || targetId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "targetId is invalid");
        }

        String type = targetType.trim().toLowerCase();
        if (!VALID_TARGET_TYPES.contains(type)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Unsupported targetType");
        }

        if (TARGET_CONTENT.equals(type)) {
            Content content = contentService.getById(targetId);
            if (content == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Report target not found");
            }
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("id", content.getId());
            snapshot.put("title", content.getTitle());
            snapshot.put("content", normalizeText(content.getContent(), 500));
            snapshot.put("userId", content.getUserId());
            snapshot.put("status", content.getStatus());
            snapshot.put("reviewStatus", content.getReviewStatus());
            snapshot.put("createTime", content.getCreateTime());
            return snapshot;
        }

        if (TARGET_COMMENT.equals(type)) {
            Comment comment = commentService.getById(targetId);
            if (comment == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Report target not found");
            }
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("id", comment.getId());
            snapshot.put("contentId", comment.getContentId());
            snapshot.put("userId", comment.getUserId());
            snapshot.put("commentContent", normalizeText(comment.getCommentContent(), 500));
            snapshot.put("status", comment.getStatus());
            snapshot.put("reviewStatus", comment.getReviewStatus());
            snapshot.put("createTime", comment.getCreateTime());
            return snapshot;
        }

        User user = userService.getById(targetId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Report target not found");
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", user.getId());
        snapshot.put("username", user.getUsername());
        snapshot.put("nickname", user.getNickname());
        snapshot.put("status", user.getStatus());
        snapshot.put("createTime", user.getCreateTime());
        return snapshot;
    }

    private Map<String, Object> fetchCurrentTargetPreview(String targetType, Long targetId) {
        // 预览优先返回目标“当前状态”，用于管理员判断是否仍需处理该工单。
        Map<String, Object> current = new HashMap<>();
        current.put("targetType", targetType);
        current.put("targetId", targetId);

        String type = targetType == null ? "" : targetType.trim().toLowerCase();
        if (TARGET_CONTENT.equals(type)) {
            Content content = contentService.getById(targetId);
            current.put("exists", content != null);
            if (content != null) {
                current.put("title", content.getTitle());
                current.put("content", normalizeText(content.getContent(), 500));
                current.put("userId", content.getUserId());
                current.put("status", content.getStatus());
                current.put("reviewStatus", content.getReviewStatus());
                current.put("createTime", content.getCreateTime());
            }
            return current;
        }

        if (TARGET_COMMENT.equals(type)) {
            Comment comment = commentService.getById(targetId);
            current.put("exists", comment != null);
            if (comment != null) {
                current.put("contentId", comment.getContentId());
                current.put("userId", comment.getUserId());
                current.put("commentContent", normalizeText(comment.getCommentContent(), 500));
                current.put("status", comment.getStatus());
                current.put("reviewStatus", comment.getReviewStatus());
                current.put("createTime", comment.getCreateTime());
            }
            return current;
        }

        if (TARGET_USER.equals(type)) {
            User user = userService.getById(targetId);
            current.put("exists", user != null);
            if (user != null) {
                current.put("username", user.getUsername());
                current.put("nickname", user.getNickname());
                current.put("status", user.getStatus());
                current.put("createTime", user.getCreateTime());
            }
            return current;
        }

        current.put("exists", false);
        return current;
    }

    private Report mustGetReport(Long reportId) {
        if (reportId == null || reportId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid report id");
        }
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Report not found");
        }
        return report;
    }

    private Comment mustGetComment(Long commentId) {
        if (commentId == null || commentId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid comment id");
        }
        Comment comment = commentService.getById(commentId);
        if (comment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Comment not found");
        }
        return comment;
    }

    private User mustGetUser(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid assignee user id");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Assignee user not found");
        }
        return user;
    }

    private Map<String, Object> toReportView(Report report, Map<Long, User> userMap) {
        Map<String, Object> view = new HashMap<>();
        User reporter = report.getReporterId() == null ? null : userMap.get(report.getReporterId());
        User assignee = report.getAssigneeId() == null ? null : userMap.get(report.getAssigneeId());

        view.put("id", report.getId());
        view.put("reporterId", report.getReporterId());
        view.put("reporterUsername", reporter == null ? null : reporter.getUsername());
        view.put("reporterNickname", reporter == null ? null : reporter.getNickname());
        view.put("targetType", report.getTargetType());
        view.put("targetId", report.getTargetId());
        view.put("reason", report.getReason());
        view.put("status", report.getStatus());
        view.put("assigneeId", report.getAssigneeId());
        view.put("assigneeUsername", assignee == null ? null : assignee.getUsername());
        view.put("assigneeNickname", assignee == null ? null : assignee.getNickname());
        view.put("handleNote", report.getHandleNote());
        view.put("resolveAction", report.getResolveAction());
        view.put("handleTime", report.getHandleTime());
        view.put("targetSnapshot", parseJsonMap(report.getTargetSnapshot()));
        long durationMinutes = resolveDurationMinutes(report);
        view.put("handleDurationMinutes", durationMinutes);
        view.put("overtime", isOvertime(report, durationMinutes));
        view.put("createTime", report.getCreateTime());
        view.put("updateTime", report.getUpdateTime());
        return view;
    }

    private Map<String, Object> toMyReportView(Report report) {
        Map<String, Object> view = new HashMap<>();
        Map<String, Object> snapshot = parseJsonMap(report.getTargetSnapshot());
        Map<String, Object> current = fetchCurrentTargetPreview(report.getTargetType(), report.getTargetId());

        long durationMinutes = resolveDurationMinutes(report);
        view.put("id", report.getId());
        view.put("targetType", report.getTargetType());
        view.put("targetId", report.getTargetId());
        view.put("reason", report.getReason());
        view.put("status", report.getStatus());
        view.put("handleNote", report.getHandleNote());
        view.put("resolveAction", report.getResolveAction());
        view.put("handleTime", report.getHandleTime());
        view.put("createTime", report.getCreateTime());
        view.put("updateTime", report.getUpdateTime());
        view.put("handleDurationMinutes", durationMinutes);
        view.put("overtime", isOvertime(report, durationMinutes));
        view.put("targetSnapshot", snapshot);
        view.put("current", current);
        view.put("targetSummary", buildTargetSummary(report.getTargetType(), report.getTargetId(), snapshot, current));
        return view;
    }

    private Map<String, Object> toSnapshot(Report report) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", report.getId());
        snapshot.put("status", report.getStatus());
        snapshot.put("assigneeId", report.getAssigneeId());
        snapshot.put("handleNote", report.getHandleNote());
        snapshot.put("resolveAction", report.getResolveAction());
        snapshot.put("handleTime", report.getHandleTime());
        snapshot.put("updateTime", report.getUpdateTime());
        return snapshot;
    }

    private Map<String, Object> toContentSnapshot(Content content) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", content.getId());
        snapshot.put("status", content.getStatus());
        snapshot.put("reviewStatus", content.getReviewStatus());
        snapshot.put("reviewReason", content.getReviewReason());
        snapshot.put("reviewerId", content.getReviewerId());
        snapshot.put("reviewTime", content.getReviewTime());
        return snapshot;
    }

    private Map<String, Object> toCommentSnapshot(Comment comment) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", comment.getId());
        snapshot.put("status", comment.getStatus());
        snapshot.put("reviewStatus", comment.getReviewStatus());
        snapshot.put("reviewReason", comment.getReviewReason());
        snapshot.put("reviewerId", comment.getReviewerId());
        snapshot.put("reviewTime", comment.getReviewTime());
        return snapshot;
    }

    private String buildTargetSummary(String targetType,
                                      Long targetId,
                                      Map<String, Object> snapshot,
                                      Map<String, Object> current) {
        String normalizedType = normalizeOptional(targetType);
        if (TARGET_CONTENT.equals(normalizedType)) {
            String title = firstNonBlank(
                    safeText(current == null ? null : current.get("title")),
                    safeText(snapshot == null ? null : snapshot.get("title"))
            );
            return title == null ? "内容 #" + targetId : "内容：" + title;
        }
        if (TARGET_COMMENT.equals(normalizedType)) {
            String content = firstNonBlank(
                    safeText(current == null ? null : current.get("commentContent")),
                    safeText(snapshot == null ? null : snapshot.get("commentContent"))
            );
            if (content == null) {
                return "评论 #" + targetId;
            }
            return "评论：" + (content.length() > 32 ? content.substring(0, 32) + "..." : content);
        }
        if (TARGET_USER.equals(normalizedType)) {
            String nickname = firstNonBlank(
                    safeText(current == null ? null : current.get("nickname")),
                    safeText(snapshot == null ? null : snapshot.get("nickname"))
            );
            String username = firstNonBlank(
                    safeText(current == null ? null : current.get("username")),
                    safeText(snapshot == null ? null : snapshot.get("username"))
            );
            String name = firstNonBlank(nickname, username);
            return name == null ? "用户 #" + targetId : "用户：" + name;
        }
        return "目标 #" + targetId;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String safeText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private long resolveDurationMinutes(Report report) {
        if (report == null || report.getCreateTime() == null) return 0L;
        LocalDateTime end = report.getHandleTime() == null ? LocalDateTime.now() : report.getHandleTime();
        if (end.isBefore(report.getCreateTime())) return 0L;
        return ChronoUnit.MINUTES.between(report.getCreateTime(), end);
    }

    private boolean isOvertime(Report report, long durationMinutes) {
        if (report == null) return false;
        if (STATUS_RESOLVED.equals(report.getStatus()) || STATUS_REJECTED.equals(report.getStatus())) return false;
        return durationMinutes > 24 * 60;
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private void ensureNotSelfReport(Long reporterId, String targetType, Map<String, Object> targetSnapshot) {
        if (reporterId == null || reporterId < 1 || targetSnapshot == null) return;
        Long ownerUserId = null;
        if (TARGET_CONTENT.equals(targetType) || TARGET_COMMENT.equals(targetType)) {
            ownerUserId = toLong(targetSnapshot.get("userId"));
        } else if (TARGET_USER.equals(targetType)) {
            ownerUserId = toLong(targetSnapshot.get("id"));
        }
        if (ownerUserId != null && ownerUserId.equals(reporterId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "You cannot report your own content/comment/account");
        }
    }

    private void ensureReportRateLimit(Long reporterId) {
        // 固定时间窗限流，防止批量恶意举报把工单池灌满。
        LocalDateTime fromTime = LocalDateTime.now().minusMinutes(CREATE_LIMIT_WINDOW_MINUTES);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, reporterId).ge(Report::getCreateTime, fromTime);
        Long count = reportMapper.selectCount(wrapper);
        if (count != null && count >= CREATE_LIMIT_MAX_COUNT) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, 429, "Too many reports in a short period");
        }
    }

    private void ensureNoOpenDuplicate(Long reporterId, String targetType, Long targetId) {
        // 对同一目标存在未闭环工单时拒绝重复创建，降低审核噪音。
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .in(Report::getStatus, STATUS_PENDING, STATUS_ASSIGNED)
                .orderByDesc(Report::getCreateTime)
                .last("LIMIT 1");
        Report duplicate = reportMapper.selectOne(wrapper);
        if (duplicate != null) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "You have already reported this target and it is still being processed");
        }
    }

    private void assertStatusCanAssign(Report report) {
        if (report == null) return;
        String status = normalizeStatus(report.getStatus());
        if (STATUS_RESOLVED.equals(status) || STATUS_REJECTED.equals(status)) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "Report ticket is already closed");
        }
    }

    private void assertStatusCanHandle(Report report) {
        if (report == null) return;
        String status = normalizeStatus(report.getStatus());
        if (!STATUS_PENDING.equals(status) && !STATUS_ASSIGNED.equals(status)) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "Only pending/assigned reports can be handled");
        }
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildReportReason(String templateCode, String templateLabel, String customReason) {
        String normalizedCustom = normalizeText(customReason, 500);
        String selectedTemplate = resolveTemplateLabel(templateCode, templateLabel);
        if (selectedTemplate == null && normalizedCustom == null) return null;
        if (selectedTemplate == null) return normalizedCustom;
        if (normalizedCustom == null) return selectedTemplate;
        String merged = selectedTemplate + "；补充：" + normalizedCustom;
        return merged.length() > 500 ? merged.substring(0, 500) : merged;
    }

    private String buildViolationReason(String templateCode, String templateLabel, String reason) {
        String normalizedReason = normalizeText(reason, 500);
        String selectedTemplate = resolveTemplateLabel(templateCode, templateLabel);
        if (selectedTemplate == null) return normalizedReason;
        if (normalizedReason == null) return selectedTemplate;
        String merged = selectedTemplate + "；" + normalizedReason;
        return merged.length() > 500 ? merged.substring(0, 500) : merged;
    }

    private String resolveTemplateLabel(String templateCode, String templateLabel) {
        String fromCode = reportViolationTemplateService.resolveActiveLabelByCode(templateCode);
        if (fromCode != null) {
            return fromCode;
        }
        return normalizeText(templateLabel, 80);
    }
}
