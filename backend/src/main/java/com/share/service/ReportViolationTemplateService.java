package com.share.service;

import com.share.dto.PageResult;

import java.util.List;
import java.util.Map;

public interface ReportViolationTemplateService {

    List<Map<String, Object>> listActiveTemplates();

    String resolveActiveLabelByCode(String code);

    PageResult<Map<String, Object>> getTemplates(Integer page, Integer pageSize, String keyword, Integer status);

    Map<String, Object> createTemplate(Long operatorUserId,
                                       String code,
                                       String label,
                                       String description,
                                       Integer sortOrder,
                                       String ip,
                                       String userAgent);

    Map<String, Object> updateTemplate(Long operatorUserId,
                                       Long templateId,
                                       String code,
                                       String label,
                                       String description,
                                       Integer sortOrder,
                                       String ip,
                                       String userAgent);

    Map<String, Object> changeTemplateStatus(Long operatorUserId,
                                             Long templateId,
                                             Integer status,
                                             String ip,
                                             String userAgent);
}
