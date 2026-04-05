package com.share.service;

import com.share.dto.PageResult;

import java.util.Map;

public interface AdminModerationService {

    PageResult<Map<String, Object>> getContents(Integer page,
                                                Integer pageSize,
                                                String keyword,
                                                Integer status,
                                                String reviewStatus,
                                                Long userId);

    Map<String, Object> offShelfContent(Long operatorUserId, Long contentId, String reason, String ip, String userAgent);

    Map<String, Object> restoreContent(Long operatorUserId, Long contentId, String reason, String ip, String userAgent);

    PageResult<Map<String, Object>> getComments(Integer page,
                                                Integer pageSize,
                                                Long contentId,
                                                Long userId,
                                                String reviewStatus);

    Map<String, Object> hideComment(Long operatorUserId, Long commentId, String reason, String ip, String userAgent);

    Map<String, Object> restoreComment(Long operatorUserId, Long commentId, String reason, String ip, String userAgent);
}

