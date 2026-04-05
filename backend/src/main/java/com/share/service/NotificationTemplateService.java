package com.share.service;

import java.util.Map;

public interface NotificationTemplateService {

    java.util.List<Map<String, Object>> listTemplates();

    Map<String, Object> updateTemplate(Long operatorUserId,
                                       String code,
                                       String name,
                                       String titleTemplate,
                                       String bodyTemplate,
                                       Integer status,
                                       String ip,
                                       String userAgent);

    Map<String, String> render(String code, Map<String, Object> context, String fallbackTitle, String fallbackBody);
}
