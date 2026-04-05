package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.dto.PageResult;
import com.share.entity.Comment;
import com.share.entity.Content;
import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.security.UserStatusCodes;
import com.share.service.AdminAuditService;
import com.share.service.AdminUserService;
import com.share.service.CommentService;
import com.share.service.ContentService;
import com.share.service.NotificationService;
import com.share.service.NotificationTemplateService;
import com.share.service.UserFollowService;
import com.share.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 管理端用户治理服务。
 * 提供封禁/解封、禁言/解禁、风险标记、用户详情聚合，并保证操作审计可追溯。
 */
public class AdminUserServiceImpl implements AdminUserService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final ContentService contentService;
    private final CommentService commentService;
    private final UserFollowService userFollowService;
    private final AdminAuditService adminAuditService;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;

    public AdminUserServiceImpl(UserService userService,
                                ContentService contentService,
                                CommentService commentService,
                                UserFollowService userFollowService,
                                AdminAuditService adminAuditService,
                                NotificationService notificationService,
                                NotificationTemplateService notificationTemplateService) {
        this.userService = userService;
        this.contentService = contentService;
        this.commentService = commentService;
        this.userFollowService = userFollowService;
        this.adminAuditService = adminAuditService;
        this.notificationService = notificationService;
        this.notificationTemplateService = notificationTemplateService;
    }

    @Override
    public PageResult<Map<String, Object>> getUsers(Integer page, Integer pageSize, String keyword, Integer status) {
        // 管理列表入口：支持按关键词和状态检索，状态用于治理筛查。
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getUsername, kw)
                    .or()
                    .like(User::getNickname, kw)
                    .or()
                    .like(User::getEmail, kw));
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        queryWrapper.orderByDesc(User::getId);

        Page<User> pageData = userService.page(new Page<>(validPage, validPageSize), queryWrapper);
        List<Map<String, Object>> list = pageData.getRecords().stream().map(this::toUserView).toList();

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    public Map<String, Object> getUserDetail(Long targetUserId) {
        // 详情聚合近30天行为与待审核量，辅助运营快速判断风险与活跃度。
        User user = mustGetUser(targetUserId);
        Map<String, Object> result = toUserView(user);

        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<Content> contentWrapper = new LambdaQueryWrapper<>();
        contentWrapper.eq(Content::getUserId, targetUserId)
                .ge(Content::getCreateTime, from);
        long recentContentCount = contentService.count(contentWrapper);

        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getUserId, targetUserId)
                .ge(Comment::getCreateTime, from);
        long recentCommentCount = commentService.count(commentWrapper);

        LambdaQueryWrapper<Content> pendingContentWrapper = new LambdaQueryWrapper<>();
        pendingContentWrapper.eq(Content::getUserId, targetUserId)
                .eq(Content::getStatus, 1)
                .and(wrapper -> wrapper.isNull(Content::getReviewStatus).or().ne(Content::getReviewStatus, "approved"));
        long pendingContentCount = contentService.count(pendingContentWrapper);

        LambdaQueryWrapper<Comment> pendingCommentWrapper = new LambdaQueryWrapper<>();
        pendingCommentWrapper.eq(Comment::getUserId, targetUserId)
                .eq(Comment::getStatus, 1)
                .and(wrapper -> wrapper.isNull(Comment::getReviewStatus).or().ne(Comment::getReviewStatus, "approved"));
        long pendingCommentCount = commentService.count(pendingCommentWrapper);

        result.put("recentContentCount", recentContentCount);
        result.put("recentCommentCount", recentCommentCount);
        result.put("pendingReviewContentCount", pendingContentCount);
        result.put("pendingReviewCommentCount", pendingCommentCount);
        result.put("followerCount", userFollowService.getFollowerCount(targetUserId));
        result.put("followingCount", userFollowService.getFollowingCount(targetUserId));
        result.put("unfollowCount", userFollowService.getUnfollowCount(targetUserId));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> banUser(Long operatorUserId, Long targetUserId, String reason, String ip, String userAgent) {
        // 封禁优先级最高：用户处于封禁时，状态展示与权限拦截都以封禁为准。
        User target = mustGetUser(targetUserId);
        validateNotSelf(operatorUserId, targetUserId);

        Map<String, Object> before = toAuditUserSnapshot(target);
        LocalDateTime now = LocalDateTime.now();

        String normalizedReason = normalizeReason(reason);
        target.setStatus(UserStatusCodes.BANNED);
        target.setBanReason(normalizedReason);
        target.setBanTime(now);
        target.setUpdateTime(now);
        userService.updateById(target);

        Map<String, Object> after = toAuditUserSnapshot(target);
        adminAuditService.log(operatorUserId, "admin.user.ban", "user", targetUserId, before, after, ip, userAgent);

        Map<String, String> rendered = notificationTemplateService.render(
                "USER_BANNED",
                Map.of("reason", normalizedReason == null ? "Not provided" : normalizedReason),
                "Account Ban Notice",
                "Your account has been banned by an administrator."
        );
        notificationService.createSystemNotification(targetUserId, NotificationService.TYPE_SYSTEM_NOTICE, rendered.get("title"), rendered.get("body"));

        return toUserActionResult(target);
    }

    @Override
    @Transactional
    public Map<String, Object> unbanUser(Long operatorUserId, Long targetUserId, String ip, String userAgent) {
        // 解封后若仍在禁言窗口内，回落到禁言态，否则回到正常态。
        User target = mustGetUser(targetUserId);
        validateNotSelf(operatorUserId, targetUserId);

        Map<String, Object> before = toAuditUserSnapshot(target);
        LocalDateTime now = LocalDateTime.now();
        boolean stillMuted = target.getMuteUntil() != null && target.getMuteUntil().isAfter(now);

        target.setStatus(stillMuted ? UserStatusCodes.MUTED : UserStatusCodes.NORMAL);
        target.setBanReason(null);
        target.setBanTime(null);
        target.setUpdateTime(now);
        userService.updateById(target);

        Map<String, Object> after = toAuditUserSnapshot(target);
        adminAuditService.log(operatorUserId, "admin.user.unban", "user", targetUserId, before, after, ip, userAgent);
        return toUserActionResult(target);
    }

    @Override
    @Transactional
    public Map<String, Object> muteUser(Long operatorUserId, Long targetUserId, Integer minutes, String reason, String ip, String userAgent) {
        // 禁言不覆盖封禁态：已封禁账号只更新禁言到期时间，不改变封禁状态。
        if (minutes == null || minutes < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Mute duration must be greater than 0 minutes");
        }
        User target = mustGetUser(targetUserId);
        validateNotSelf(operatorUserId, targetUserId);

        Map<String, Object> before = toAuditUserSnapshot(target);
        LocalDateTime now = LocalDateTime.now();
        String normalizedReason = normalizeReason(reason);

        target.setMuteUntil(now.plusMinutes(minutes));
        if (!UserStatusCodes.isBanned(target.getStatus())) {
            target.setStatus(UserStatusCodes.MUTED);
        }
        if (normalizedReason != null) {
            target.setBanReason(normalizedReason);
        }
        target.setUpdateTime(now);
        userService.updateById(target);

        Map<String, Object> after = toAuditUserSnapshot(target);
        Map<String, Object> extra = new HashMap<>();
        extra.put("minutes", minutes);
        extra.put("reason", normalizedReason);
        after.put("muteMeta", extra);
        adminAuditService.log(operatorUserId, "admin.user.mute", "user", targetUserId, before, after, ip, userAgent);

        Map<String, String> rendered = notificationTemplateService.render(
                "USER_MUTED",
                Map.of(
                        "minutes", minutes,
                        "reason", normalizedReason == null ? "Not provided" : normalizedReason
                ),
                "Account Mute Notice",
                "Your account has been muted by an administrator."
        );
        notificationService.createSystemNotification(targetUserId, NotificationService.TYPE_SYSTEM_NOTICE, rendered.get("title"), rendered.get("body"));

        return toUserActionResult(target);
    }

    @Override
    @Transactional
    public Map<String, Object> unmuteUser(Long operatorUserId, Long targetUserId, String ip, String userAgent) {
        // 解除禁言时同样遵循“封禁优先”规则，避免误解除封禁。
        User target = mustGetUser(targetUserId);
        validateNotSelf(operatorUserId, targetUserId);

        Map<String, Object> before = toAuditUserSnapshot(target);
        LocalDateTime now = LocalDateTime.now();

        target.setMuteUntil(null);
        if (!UserStatusCodes.isBanned(target.getStatus())) {
            target.setStatus(UserStatusCodes.NORMAL);
        }
        target.setUpdateTime(now);
        userService.updateById(target);

        Map<String, Object> after = toAuditUserSnapshot(target);
        adminAuditService.log(operatorUserId, "admin.user.unmute", "user", targetUserId, before, after, ip, userAgent);
        return toUserActionResult(target);
    }

    @Override
    @Transactional
    public Map<String, Object> markRisk(Long operatorUserId, Long targetUserId, String riskLevel, String riskNote, String ip, String userAgent) {
        // 风险标记用于运营关注，不直接改变用户可用状态。
        User target = mustGetUser(targetUserId);
        validateNotSelf(operatorUserId, targetUserId);
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
        if (normalizedRiskLevel == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "riskLevel must be low, medium or high");
        }

        Map<String, Object> before = toAuditUserSnapshot(target);
        LocalDateTime now = LocalDateTime.now();
        target.setRiskLevel(normalizedRiskLevel);
        target.setRiskNote(normalizeReason(riskNote));
        target.setRiskMarkBy(operatorUserId);
        target.setRiskMarkTime(now);
        target.setUpdateTime(now);
        userService.updateById(target);

        Map<String, Object> after = toAuditUserSnapshot(target);
        adminAuditService.log(operatorUserId, "admin.user.risk_mark", "user", targetUserId, before, after, ip, userAgent);
        return toUserView(target);
    }

    @Override
    @Transactional
    public Map<String, Object> unmarkRisk(Long operatorUserId, Long targetUserId, String ip, String userAgent) {
        // 取消标记会清理风险等级、备注和标记人/标记时间。
        User target = mustGetUser(targetUserId);
        validateNotSelf(operatorUserId, targetUserId);

        Map<String, Object> before = toAuditUserSnapshot(target);
        target.setRiskLevel(null);
        target.setRiskNote(null);
        target.setRiskMarkBy(null);
        target.setRiskMarkTime(null);
        target.setUpdateTime(LocalDateTime.now());
        userService.updateById(target);

        Map<String, Object> after = toAuditUserSnapshot(target);
        adminAuditService.log(operatorUserId, "admin.user.risk_unmark", "user", targetUserId, before, after, ip, userAgent);
        return toUserView(target);
    }

    private User mustGetUser(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid user id");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "User not found");
        }
        return user;
    }

    private void validateNotSelf(Long operatorUserId, Long targetUserId) {
        if (operatorUserId != null && operatorUserId.equals(targetUserId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "You cannot operate on your own account");
        }
    }

    private Map<String, Object> toUserView(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("avatar", user.getAvatar());
        result.put("bio", user.getBio());
        result.put("status", user.getStatus());
        result.put("muteUntil", user.getMuteUntil());
        result.put("banReason", user.getBanReason());
        result.put("banTime", user.getBanTime());
        result.put("riskLevel", user.getRiskLevel());
        result.put("riskNote", user.getRiskNote());
        result.put("riskMarkBy", user.getRiskMarkBy());
        result.put("riskMarkTime", user.getRiskMarkTime());
        result.put("createTime", user.getCreateTime());
        result.put("updateTime", user.getUpdateTime());
        result.put("roles", userService.getRoleCodesByUserId(user.getId()));
        result.put("permissions", userService.getPermissionCodesByUserId(user.getId()));
        return result;
    }

    private Map<String, Object> toAuditUserSnapshot(User user) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", user.getId());
        snapshot.put("status", user.getStatus());
        snapshot.put("muteUntil", user.getMuteUntil());
        snapshot.put("banReason", user.getBanReason());
        snapshot.put("banTime", user.getBanTime());
        snapshot.put("riskLevel", user.getRiskLevel());
        snapshot.put("riskNote", user.getRiskNote());
        snapshot.put("riskMarkBy", user.getRiskMarkBy());
        snapshot.put("riskMarkTime", user.getRiskMarkTime());
        snapshot.put("updateTime", user.getUpdateTime());
        return snapshot;
    }

    private Map<String, Object> toUserActionResult(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("status", user.getStatus());
        result.put("muteUntil", user.getMuteUntil());
        result.put("banReason", user.getBanReason());
        result.put("banTime", user.getBanTime());
        result.put("riskLevel", user.getRiskLevel());
        result.put("riskNote", user.getRiskNote());
        return result;
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

    private String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null) {
            return null;
        }
        String value = riskLevel.trim().toLowerCase();
        if ("low".equals(value) || "medium".equals(value) || "high".equals(value)) {
            return value;
        }
        return null;
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
