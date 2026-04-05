package com.share.service;

import com.share.dto.PageResult;

import java.util.List;
import java.util.Map;

public interface AnnouncementService {

    PageResult<Map<String, Object>> getAdminAnnouncements(Integer page, Integer pageSize, String status, String keyword);

    Map<String, Object> createAnnouncement(Long operatorUserId,
                                           String title,
                                           String body,
                                           Boolean pinned,
                                           java.time.LocalDateTime startTime,
                                           java.time.LocalDateTime endTime,
                                           String ip,
                                           String userAgent);

    Map<String, Object> updateAnnouncement(Long operatorUserId,
                                           Long announcementId,
                                           String title,
                                           String body,
                                           Boolean pinned,
                                           java.time.LocalDateTime startTime,
                                           java.time.LocalDateTime endTime,
                                           String ip,
                                           String userAgent);

    Map<String, Object> publishAnnouncement(Long operatorUserId, Long announcementId, String ip, String userAgent);

    Map<String, Object> offlineAnnouncement(Long operatorUserId, Long announcementId, String ip, String userAgent);

    List<Map<String, Object>> getActiveAnnouncements(Long userId);

    PageResult<Map<String, Object>> getMyAnnouncements(Long userId, Integer page, Integer pageSize);

    Long getUnreadAnnouncementCount(Long userId);

    Map<String, Object> markAnnouncementRead(Long userId, Long announcementId);

    Map<String, Object> markAllAnnouncementsRead(Long userId);
}
