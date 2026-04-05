package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.dto.PageResult;
import com.share.entity.Announcement;
import com.share.entity.AnnouncementRead;
import com.share.exception.BusinessException;
import com.share.realtime.NotificationWebSocketSessionManager;
import com.share.service.AdminAuditService;
import com.share.service.AnnouncementService;
import com.share.service.RealtimeMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.share.mapper.AnnouncementMapper;
import com.share.mapper.AnnouncementReadMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * 公告服务：
 * - 管理端：草稿/发布/下线全流程管理，并写入审计日志。
 * - 用户端：读取当前有效公告与已读状态，支持单条/全部已读。
 */
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_OFFLINE = "offline";

    private final AnnouncementMapper announcementMapper;
    private final AnnouncementReadMapper announcementReadMapper;
    private final AdminAuditService adminAuditService;
    private final NotificationWebSocketSessionManager notificationWebSocketSessionManager;
    private final RealtimeMessageService realtimeMessageService;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper,
                                   AnnouncementReadMapper announcementReadMapper,
                                   AdminAuditService adminAuditService,
                                   NotificationWebSocketSessionManager notificationWebSocketSessionManager,
                                   RealtimeMessageService realtimeMessageService) {
        this.announcementMapper = announcementMapper;
        this.announcementReadMapper = announcementReadMapper;
        this.adminAuditService = adminAuditService;
        this.notificationWebSocketSessionManager = notificationWebSocketSessionManager;
        this.realtimeMessageService = realtimeMessageService;
    }

    @Override
    // 管理端公告列表：按置顶、发布时间排序，支持状态和关键词筛选。
    public PageResult<Map<String, Object>> getAdminAnnouncements(Integer page, Integer pageSize, String status, String keyword) {
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<Announcement> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(Announcement::getStatus, status.trim().toLowerCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            queryWrapper.and(wrapper -> wrapper.like(Announcement::getTitle, kw).or().like(Announcement::getBody, kw));
        }
        queryWrapper.orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getId);

        Page<Announcement> pageData = announcementMapper.selectPage(new Page<>(validPage, validPageSize), queryWrapper);
        List<Map<String, Object>> list = pageData.getRecords().stream().map(this::toAnnouncementView).toList();

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    @Transactional
    // 新建公告默认草稿，避免创建即触达用户造成运营误发。
    public Map<String, Object> createAnnouncement(Long operatorUserId,
                                                   String title,
                                                   String body,
                                                   Boolean pinned,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime,
                                                   String ip,
                                                   String userAgent) {
        validateTimeRange(startTime, endTime);

        Announcement announcement = new Announcement();
        announcement.setTitle(normalizeRequired(title, 120, "Announcement title is required"));
        announcement.setBody(normalizeRequired(body, 2000, "Announcement body is required"));
        announcement.setStatus(STATUS_DRAFT);
        announcement.setIsPinned(Boolean.TRUE.equals(pinned));
        announcement.setStartTime(startTime);
        announcement.setEndTime(endTime);
        announcement.setPublishTime(null);
        announcement.setCreatorId(operatorUserId);
        announcement.setUpdaterId(operatorUserId);
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.insert(announcement);

        Map<String, Object> after = toAnnouncementView(announcement);
        adminAuditService.log(operatorUserId, "admin.announcement.create", "announcement", announcement.getId(), null, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    // 编辑公告仅更新内容，不改变当前发布状态。
    public Map<String, Object> updateAnnouncement(Long operatorUserId,
                                                   Long announcementId,
                                                   String title,
                                                   String body,
                                                   Boolean pinned,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime,
                                                   String ip,
                                                   String userAgent) {
        validateTimeRange(startTime, endTime);
        Announcement announcement = mustGetAnnouncement(announcementId);
        Map<String, Object> before = toAnnouncementView(announcement);

        announcement.setTitle(normalizeRequired(title, 120, "Announcement title is required"));
        announcement.setBody(normalizeRequired(body, 2000, "Announcement body is required"));
        announcement.setIsPinned(Boolean.TRUE.equals(pinned));
        announcement.setStartTime(startTime);
        announcement.setEndTime(endTime);
        announcement.setUpdaterId(operatorUserId);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);

        Map<String, Object> after = toAnnouncementView(announcement);
        adminAuditService.log(operatorUserId, "admin.announcement.update", "announcement", announcementId, before, after, ip, userAgent);
        // 仅对“已发布且当前可见”的公告发送实时更新事件，避免草稿变更打扰在线用户。
        pushAnnouncementRealtimeEvent("update", announcement);
        return after;
    }

    @Override
    @Transactional
    // 发布公告后才进入用户可见范围。
    public Map<String, Object> publishAnnouncement(Long operatorUserId, Long announcementId, String ip, String userAgent) {
        Announcement announcement = mustGetAnnouncement(announcementId);
        Map<String, Object> before = toAnnouncementView(announcement);

        announcement.setStatus(STATUS_PUBLISHED);
        if (announcement.getPublishTime() == null) {
            announcement.setPublishTime(LocalDateTime.now());
        }
        announcement.setUpdaterId(operatorUserId);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);

        Map<String, Object> after = toAnnouncementView(announcement);
        adminAuditService.log(operatorUserId, "admin.announcement.publish", "announcement", announcementId, before, after, ip, userAgent);
        // 发布后立即向在线用户推送公告事件，确保“公告/消息角标/消息中心”无需等待轮询即可同步。
        pushAnnouncementRealtimeEvent("publish", announcement);
        return after;
    }

    @Override
    @Transactional
    // 下线公告做软状态切换，不删除记录，保留审计链路。
    public Map<String, Object> offlineAnnouncement(Long operatorUserId, Long announcementId, String ip, String userAgent) {
        Announcement announcement = mustGetAnnouncement(announcementId);
        Map<String, Object> before = toAnnouncementView(announcement);

        announcement.setStatus(STATUS_OFFLINE);
        announcement.setUpdaterId(operatorUserId);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);

        Map<String, Object> after = toAnnouncementView(announcement);
        adminAuditService.log(operatorUserId, "admin.announcement.offline", "announcement", announcementId, before, after, ip, userAgent);
        // 下线属于公告可见性变化，通知在线用户刷新公告列表与角标。
        pushAnnouncementRealtimeEvent("offline", announcement);
        return after;
    }

    @Override
    // 首页仅取当前有效公告，最多 5 条，按置顶优先返回。
    public List<Map<String, Object>> getActiveAnnouncements(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Announcement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Announcement::getStatus, STATUS_PUBLISHED);
        appendActiveTimeWindow(queryWrapper, now);
        queryWrapper.orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishTime)
                .last("LIMIT 5");

        List<Announcement> list = announcementMapper.selectList(queryWrapper);
        Set<Long> readIds = collectReadAnnouncementIds(userId, list.stream().map(Announcement::getId).collect(Collectors.toSet()));
        return list.stream().map(item -> toAnnouncementViewWithRead(item, readIds.contains(item.getId()))).toList();
    }

    @Override
    // 消息中心公告列表分页展示，并回填当前用户是否已读。
    public PageResult<Map<String, Object>> getMyAnnouncements(Long userId, Integer page, Integer pageSize) {
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Announcement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Announcement::getStatus, STATUS_PUBLISHED);
        appendActiveTimeWindow(queryWrapper, now);
        queryWrapper.orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getId);

        Page<Announcement> pageData = announcementMapper.selectPage(new Page<>(validPage, validPageSize), queryWrapper);
        Set<Long> announcementIds = pageData.getRecords().stream().map(Announcement::getId).collect(Collectors.toSet());
        Set<Long> readIds = collectReadAnnouncementIds(userId, announcementIds);

        List<Map<String, Object>> views = pageData.getRecords().stream()
                .map(item -> toAnnouncementViewWithRead(item, readIds.contains(item.getId())))
                .toList();

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(views);
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    // 公告未读总数用于顶部角标汇总。
    public Long getUnreadAnnouncementCount(Long userId) {
        if (userId == null || userId < 1) {
            return 0L;
        }
        Long count = announcementReadMapper.countUnreadAnnouncements(userId);
        return count == null ? 0L : count;
    }

    @Override
    @Transactional
    // 单条公告已读：幂等写入 read 表，不重复插入。
    public Map<String, Object> markAnnouncementRead(Long userId, Long announcementId) {
        if (announcementId == null || announcementId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid announcement id");
        }
        Announcement announcement = mustGetAnnouncement(announcementId);
        if (!STATUS_PUBLISHED.equalsIgnoreCase(announcement.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Announcement is not published");
        }

        LambdaQueryWrapper<AnnouncementRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AnnouncementRead::getAnnouncementId, announcementId)
                .eq(AnnouncementRead::getUserId, userId);
        AnnouncementRead existing = announcementReadMapper.selectOne(queryWrapper);
        if (existing == null) {
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncementId(announcementId);
            read.setUserId(userId);
            read.setReadTime(LocalDateTime.now());
            read.setCreateTime(LocalDateTime.now());
            announcementReadMapper.insert(read);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("announcementId", announcementId);
        result.put("read", true);
        result.put("unreadCount", getUnreadAnnouncementCount(userId));
        return result;
    }

    @Override
    @Transactional
    // 一键已读：批量把当前用户的公告未读写为已读。
    public Map<String, Object> markAllAnnouncementsRead(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid user id");
        }
        int updated = announcementReadMapper.markAllUnreadAsRead(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("unreadCount", getUnreadAnnouncementCount(userId));
        return result;
    }

    private Announcement mustGetAnnouncement(Long announcementId) {
        if (announcementId == null || announcementId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid announcement id");
        }
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Announcement not found");
        }
        return announcement;
    }

    private Set<Long> collectReadAnnouncementIds(Long userId, Set<Long> announcementIds) {
        if (userId == null || userId < 1 || announcementIds == null || announcementIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<AnnouncementRead> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AnnouncementRead::getUserId, userId)
                .in(AnnouncementRead::getAnnouncementId, announcementIds);
        return announcementReadMapper.selectList(queryWrapper).stream()
                .map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> toAnnouncementView(Announcement announcement) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", announcement.getId());
        view.put("title", announcement.getTitle());
        view.put("body", announcement.getBody());
        view.put("status", announcement.getStatus());
        view.put("isPinned", Boolean.TRUE.equals(announcement.getIsPinned()));
        view.put("startTime", announcement.getStartTime());
        view.put("endTime", announcement.getEndTime());
        view.put("publishTime", announcement.getPublishTime());
        view.put("creatorId", announcement.getCreatorId());
        view.put("updaterId", announcement.getUpdaterId());
        view.put("createTime", announcement.getCreateTime());
        view.put("updateTime", announcement.getUpdateTime());
        return view;
    }

    private Map<String, Object> toAnnouncementViewWithRead(Announcement announcement, boolean read) {
        Map<String, Object> view = toAnnouncementView(announcement);
        view.put("isRead", read);
        return view;
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "endTime must be greater than startTime");
        }
    }

    private void appendActiveTimeWindow(LambdaQueryWrapper<Announcement> queryWrapper, LocalDateTime now) {
        queryWrapper.and(wrapper -> wrapper.isNull(Announcement::getStartTime).or().le(Announcement::getStartTime, now))
                .and(wrapper -> wrapper
                        .isNull(Announcement::getEndTime)
                        .or()
                        .ge(Announcement::getEndTime, now)
                        // Compatibility: treat "yyyy-MM-dd 00:00:00" as valid for the whole same day.
                        .or()
                        .apply("DATE(end_time) = CURDATE() AND TIME(end_time) = '00:00:00'")
                );
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

    private String normalizeRequired(String value, int max, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, message);
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    /**
     * 公告实时事件推送。
     * 规则：
     * 1. 仅对在线用户推送，离线用户由常规接口在下次进入时获取最新状态；
     * 2. 草稿公告不推送；
     * 3. 通过 payload.action 让前端决定是“弹窗提醒”还是“静默刷新列表”。
     */
    private void pushAnnouncementRealtimeEvent(String action, Announcement announcement) {
        if (announcement == null || announcement.getId() == null) {
            return;
        }
        if (!STATUS_PUBLISHED.equalsIgnoreCase(String.valueOf(announcement.getStatus())) && !"offline".equalsIgnoreCase(action)) {
            return;
        }

        Set<Long> onlineUserIds = notificationWebSocketSessionManager.getOnlineUserIds();
        if (onlineUserIds.isEmpty()) {
            return;
        }

        boolean activeNow = isAnnouncementActiveNow(announcement, LocalDateTime.now());
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("announcementId", announcement.getId());
        payload.put("title", announcement.getTitle());
        payload.put("body", announcement.getBody());
        payload.put("isPinned", Boolean.TRUE.equals(announcement.getIsPinned()));
        payload.put("publishTime", announcement.getPublishTime());
        payload.put("startTime", announcement.getStartTime());
        payload.put("endTime", announcement.getEndTime());
        payload.put("activeNow", activeNow);
        payload.put("createTime", announcement.getCreateTime());
        payload.put("updateTime", announcement.getUpdateTime());
        realtimeMessageService.pushAnnouncementEvent(onlineUserIds, payload);
    }

    /**
     * 判断公告在“当前时刻”是否处于可见窗口。
     * 兼容规则：当结束时间为当天00:00:00时，视为该自然日全天有效。
     */
    private boolean isAnnouncementActiveNow(Announcement announcement, LocalDateTime now) {
        if (!STATUS_PUBLISHED.equalsIgnoreCase(String.valueOf(announcement.getStatus()))) {
            return false;
        }
        LocalDateTime start = announcement.getStartTime();
        if (start != null && start.isAfter(now)) {
            return false;
        }
        LocalDateTime end = announcement.getEndTime();
        if (end == null) {
            return true;
        }
        if (!end.isBefore(now)) {
            return true;
        }
        return end.toLocalDate().isEqual(now.toLocalDate())
                && end.toLocalTime().getHour() == 0
                && end.toLocalTime().getMinute() == 0
                && end.toLocalTime().getSecond() == 0;
    }
}
