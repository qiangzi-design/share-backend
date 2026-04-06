package com.share.service;

import com.share.dto.AdminFeedbackHandleRequest;
import com.share.dto.FeedbackCreateRequest;
import com.share.dto.PageResult;

import java.util.Map;

/**
 * 反馈业务服务：
 * - 用户侧：提交反馈、查看我的反馈；
 * - 管理侧：分页查看、处理闭环。
 */
public interface FeedbackService {

    Map<String, Object> createFeedback(Long userId, FeedbackCreateRequest request);

    PageResult<Map<String, Object>> getMyFeedbacks(Long userId, Integer page, Integer pageSize, String status);

    PageResult<Map<String, Object>> getAdminFeedbacks(Integer page,
                                                      Integer pageSize,
                                                      String status,
                                                      String feedbackType,
                                                      String keyword);

    Map<String, Object> handleFeedback(Long operatorUserId,
                                       Long feedbackId,
                                       AdminFeedbackHandleRequest request,
                                       String ip,
                                       String userAgent);
}

