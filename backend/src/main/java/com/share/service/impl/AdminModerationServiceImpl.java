package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.dto.PageResult;
import com.share.entity.Comment;
import com.share.entity.Content;
import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.service.AdminAuditService;
import com.share.service.AdminModerationService;
import com.share.service.CommentService;
import com.share.service.ContentService;
import com.share.service.NotificationService;
import com.share.service.NotificationTemplateService;
import com.share.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * 管理端审核服务。
 * 负责内容/评论的查询与审核状态切换，并同步写入审计日志与站内通知。
 */
public class AdminModerationServiceImpl implements AdminModerationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String REVIEW_PENDING = "pending";
    private static final String REVIEW_APPROVED = "approved";
    private static final String REVIEW_REJECTED = "rejected";

    private final ContentService contentService;
    private final CommentService commentService;
    private final UserService userService;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;

    public AdminModerationServiceImpl(ContentService contentService,
                                      CommentService commentService,
                                      UserService userService,
                                      AdminAuditService adminAuditService,
                                      NotificationService notificationService,
                                      NotificationTemplateService notificationTemplateService) {
        this.contentService = contentService;
        this.commentService = commentService;
        this.userService = userService;
        this.adminAuditService = adminAuditService;
        this.notificationService = notificationService;
        this.notificationTemplateService = notificationTemplateService;
    }

    @Override
    public PageResult<Map<String, Object>> getContents(Integer page,
                                                       Integer pageSize,
                                                       String keyword,
                                                       Integer status,
                                                       String reviewStatus,
                                                       Long userId) {
        // 内容审核列表支持多维筛选，pending 同时包含 review_status 为空的历史数据。
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<Content> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(Content::getTitle, kw)
                    .or()
                    .like(Content::getContent, kw));
        }
        if (status != null) {
            queryWrapper.eq(Content::getStatus, status);
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            String normalized = reviewStatus.trim().toLowerCase();
            if (REVIEW_PENDING.equals(normalized)) {
                queryWrapper.and(wrapper -> wrapper
                        .isNull(Content::getReviewStatus)
                        .or()
                        .eq(Content::getReviewStatus, REVIEW_PENDING));
            } else {
                queryWrapper.eq(Content::getReviewStatus, normalized);
            }
        }
        if (userId != null && userId > 0) {
            queryWrapper.eq(Content::getUserId, userId);
        }
        queryWrapper.orderByDesc(Content::getCreateTime);

        Page<Content> pageData = contentService.page(new Page<>(validPage, validPageSize), queryWrapper);
        Map<Long, User> userMap = collectUserMap(pageData.getRecords(), Content::getUserId);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(pageData.getRecords().stream().map(item -> toContentView(item, userMap.get(item.getUserId()))).toList());
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> offShelfContent(Long operatorUserId, Long contentId, String reason, String ip, String userAgent) {
        // 下架采用审核驳回口径，不做物理删除，保证可恢复。
        Content content = mustGetContent(contentId);
        Map<String, Object> before = toContentSnapshot(content);

        content.setReviewStatus(REVIEW_REJECTED);
        content.setReviewReason(normalizeReason(reason));
        content.setReviewerId(operatorUserId);
        content.setReviewTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        contentService.updateById(content);

        Map<String, Object> after = toContentSnapshot(content);
        adminAuditService.log(operatorUserId, "admin.content.off_shelf", "content", contentId, before, after, ip, userAgent);

        Map<String, String> rendered = notificationTemplateService.render(
                "CONTENT_OFF_SHELF",
                Map.of(
                        "contentTitle", content.getTitle() == null || content.getTitle().isBlank() ? "Untitled content" : content.getTitle().trim(),
                        "reason", normalizeReason(reason) == null ? "Not provided" : normalizeReason(reason)
                ),
                "Content Removed Notice",
                "Your content has been taken off shelf by an administrator."
        );
        notificationService.createSystemNotification(
                content.getUserId(),
                NotificationService.TYPE_SYSTEM_NOTICE,
                rendered.get("title"),
                rendered.get("body")
        );
        return after;
    }

    @Override
    @Transactional
    public Map<String, Object> restoreContent(Long operatorUserId, Long contentId, String reason, String ip, String userAgent) {
        // 恢复本质是将审核态改回 approved，前台可重新可见。
        Content content = mustGetContent(contentId);
        Map<String, Object> before = toContentSnapshot(content);

        content.setReviewStatus(REVIEW_APPROVED);
        content.setReviewReason(normalizeReason(reason));
        content.setReviewerId(operatorUserId);
        content.setReviewTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        contentService.updateById(content);

        Map<String, Object> after = toContentSnapshot(content);
        adminAuditService.log(operatorUserId, "admin.content.restore", "content", contentId, before, after, ip, userAgent);
        return after;
    }

    @Override
    public PageResult<Map<String, Object>> getComments(Integer page,
                                                       Integer pageSize,
                                                       Long contentId,
                                                       Long userId,
                                                       String reviewStatus) {
        // 评论审核列表支持按内容和作者过滤，便于快速定位问题评论。
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        if (contentId != null && contentId > 0) {
            queryWrapper.eq(Comment::getContentId, contentId);
        }
        if (userId != null && userId > 0) {
            queryWrapper.eq(Comment::getUserId, userId);
        }
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            queryWrapper.eq(Comment::getReviewStatus, reviewStatus.trim());
        }
        queryWrapper.orderByDesc(Comment::getCreateTime);

        Page<Comment> pageData = commentService.page(new Page<>(validPage, validPageSize), queryWrapper);
        Map<Long, User> userMap = collectUserMap(pageData.getRecords(), Comment::getUserId);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(pageData.getRecords().stream().map(item -> toCommentView(item, userMap.get(item.getUserId()))).toList());
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> hideComment(Long operatorUserId, Long commentId, String reason, String ip, String userAgent) {
        // 隐藏评论使用审核驳回态，并向评论作者发送通知。
        Comment comment = mustGetComment(commentId);
        Map<String, Object> before = toCommentSnapshot(comment);

        comment.setReviewStatus(REVIEW_REJECTED);
        comment.setReviewReason(normalizeReason(reason));
        comment.setReviewerId(operatorUserId);
        comment.setReviewTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        commentService.updateById(comment);

        Map<String, Object> after = toCommentSnapshot(comment);
        adminAuditService.log(operatorUserId, "admin.comment.hide", "comment", commentId, before, after, ip, userAgent);

        String contentTitle = null;
        if (comment.getContentId() != null) {
            Content content = contentService.getById(comment.getContentId());
            if (content != null && content.getTitle() != null && !content.getTitle().isBlank()) {
                contentTitle = content.getTitle().trim();
            }
        }
        Map<String, String> rendered = notificationTemplateService.render(
                "COMMENT_HIDDEN",
                Map.of(
                        "contentTitle", contentTitle == null ? "Related content" : contentTitle,
                        "reason", normalizeReason(reason) == null ? "Not provided" : normalizeReason(reason)
                ),
                "Comment Hidden Notice",
                "Your comment has been hidden by an administrator."
        );
        notificationService.createSystemNotification(
                comment.getUserId(),
                NotificationService.TYPE_SYSTEM_NOTICE,
                rendered.get("title"),
                rendered.get("body")
        );
        return after;
    }

    @Override
    @Transactional
    public Map<String, Object> restoreComment(Long operatorUserId, Long commentId, String reason, String ip, String userAgent) {
        // 恢复评论时保留审核痕迹（reviewer/reviewTime/reason），方便后续审计。
        Comment comment = mustGetComment(commentId);
        Map<String, Object> before = toCommentSnapshot(comment);

        comment.setReviewStatus(REVIEW_APPROVED);
        comment.setReviewReason(normalizeReason(reason));
        comment.setReviewerId(operatorUserId);
        comment.setReviewTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        commentService.updateById(comment);

        Map<String, Object> after = toCommentSnapshot(comment);
        adminAuditService.log(operatorUserId, "admin.comment.restore", "comment", commentId, before, after, ip, userAgent);
        return after;
    }

    private Content mustGetContent(Long contentId) {
        if (contentId == null || contentId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid content id");
        }
        Content content = contentService.getById(contentId);
        if (content == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Content not found");
        }
        return content;
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

    private <T> Map<Long, User> collectUserMap(Collection<T> records, Function<T, Long> userIdExtractor) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = records.stream()
                .map(userIdExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private Map<String, Object> toContentView(Content content, User author) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", content.getId());
        view.put("userId", content.getUserId());
        view.put("authorUsername", author == null ? null : author.getUsername());
        view.put("authorNickname", author == null ? null : author.getNickname());
        view.put("authorAvatar", author == null ? null : author.getAvatar());
        view.put("title", content.getTitle());
        view.put("content", content.getContent());
        view.put("categoryId", content.getCategoryId());
        view.put("tags", content.getTags());
        view.put("status", content.getStatus());
        view.put("reviewStatus", normalizeContentReviewStatus(content.getReviewStatus()));
        view.put("reviewReason", content.getReviewReason());
        view.put("reviewerId", content.getReviewerId());
        view.put("reviewTime", content.getReviewTime());
        view.put("createTime", content.getCreateTime());
        view.put("updateTime", content.getUpdateTime());
        return view;
    }

    private Map<String, Object> toCommentView(Comment comment, User author) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", comment.getId());
        view.put("contentId", comment.getContentId());
        view.put("userId", comment.getUserId());
        view.put("authorUsername", author == null ? null : author.getUsername());
        view.put("authorNickname", author == null ? null : author.getNickname());
        view.put("authorAvatar", author == null ? null : author.getAvatar());
        view.put("parentId", comment.getParentId());
        view.put("commentContent", comment.getCommentContent());
        view.put("status", comment.getStatus());
        view.put("reviewStatus", comment.getReviewStatus());
        view.put("reviewReason", comment.getReviewReason());
        view.put("reviewerId", comment.getReviewerId());
        view.put("reviewTime", comment.getReviewTime());
        view.put("createTime", comment.getCreateTime());
        view.put("updateTime", comment.getUpdateTime());
        return view;
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

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String value = reason.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String normalizeContentReviewStatus(String reviewStatus) {
        if (reviewStatus == null || reviewStatus.isBlank()) {
            return REVIEW_PENDING;
        }
        return reviewStatus.trim().toLowerCase();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
