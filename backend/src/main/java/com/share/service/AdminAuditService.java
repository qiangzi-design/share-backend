package com.share.service;

import com.share.dto.PageResult;

import java.util.Map;

public interface AdminAuditService {

    void log(Long operatorUserId,
             String action,
             String targetType,
             Long targetId,
             Object detailBefore,
             Object detailAfter,
             String ip,
             String userAgent);

    PageResult<Map<String, Object>> getAuditLogs(Integer page,
                                                 Integer pageSize,
                                                 Long operatorUserId,
                                                 String action,
                                                 String targetType);
}

