package com.share.service;

import com.share.dto.PageResult;

import java.util.Map;

public interface AdminUserService {

    PageResult<Map<String, Object>> getUsers(Integer page, Integer pageSize, String keyword, Integer status);

    Map<String, Object> getUserDetail(Long targetUserId);

    Map<String, Object> banUser(Long operatorUserId, Long targetUserId, String reason, String ip, String userAgent);

    Map<String, Object> unbanUser(Long operatorUserId, Long targetUserId, String ip, String userAgent);

    Map<String, Object> muteUser(Long operatorUserId, Long targetUserId, Integer minutes, String reason, String ip, String userAgent);

    Map<String, Object> unmuteUser(Long operatorUserId, Long targetUserId, String ip, String userAgent);

    Map<String, Object> markRisk(Long operatorUserId, Long targetUserId, String riskLevel, String riskNote, String ip, String userAgent);

    Map<String, Object> unmarkRisk(Long operatorUserId, Long targetUserId, String ip, String userAgent);
}
