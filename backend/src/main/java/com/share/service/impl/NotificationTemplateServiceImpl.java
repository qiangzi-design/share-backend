package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.entity.NotificationTemplate;
import com.share.exception.BusinessException;
import com.share.mapper.NotificationTemplateMapper;
import com.share.service.AdminAuditService;
import com.share.service.NotificationTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 站内通知模板服务：
 * - 管理端维护模板内容与启停状态。
 * - 业务发送通知时按 code 渲染模板，未命中时回退到默认文案。
 */
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateMapper notificationTemplateMapper;
    private final AdminAuditService adminAuditService;

    public NotificationTemplateServiceImpl(NotificationTemplateMapper notificationTemplateMapper,
                                           AdminAuditService adminAuditService) {
        this.notificationTemplateMapper = notificationTemplateMapper;
        this.adminAuditService = adminAuditService;
    }

    @Override
    // 模板列表按 code 稳定排序，便于管理端检索。
    public List<Map<String, Object>> listTemplates() {
        LambdaQueryWrapper<NotificationTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(NotificationTemplate::getCode);
        return notificationTemplateMapper.selectList(queryWrapper).stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    // updateTemplate 同时承担“新增或更新”能力，code 是自然键。
    public Map<String, Object> updateTemplate(Long operatorUserId,
                                              String code,
                                              String name,
                                              String titleTemplate,
                                              String bodyTemplate,
                                              Integer status,
                                              String ip,
                                              String userAgent) {
        String normalizedCode = normalizeRequired(code, 64, "Template code is required").toUpperCase();
        String normalizedName = normalizeRequired(name, 120, "Template name is required");
        String normalizedTitle = normalizeRequired(titleTemplate, 120, "Title template is required");
        String normalizedBody = normalizeRequired(bodyTemplate, 800, "Body template is required");
        int normalizedStatus = (status != null && status == 0) ? 0 : 1;

        LambdaQueryWrapper<NotificationTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NotificationTemplate::getCode, normalizedCode);
        NotificationTemplate existing = notificationTemplateMapper.selectOne(queryWrapper);

        Map<String, Object> before = existing == null ? null : toView(existing);

        if (existing == null) {
            existing = new NotificationTemplate();
            existing.setCode(normalizedCode);
            existing.setCreateTime(LocalDateTime.now());
        }

        existing.setName(normalizedName);
        existing.setTitleTemplate(normalizedTitle);
        existing.setBodyTemplate(normalizedBody);
        existing.setStatus(normalizedStatus);
        existing.setUpdateTime(LocalDateTime.now());

        if (existing.getId() == null) {
            notificationTemplateMapper.insert(existing);
        } else {
            notificationTemplateMapper.updateById(existing);
        }

        Map<String, Object> after = toView(existing);
        adminAuditService.log(operatorUserId,
                "admin.template.update",
                "notification_template",
                existing.getId(),
                before,
                after,
                ip,
                userAgent);

        return after;
    }

    @Override
    // 渲染时仅使用启用状态模板，防止下线模板继续生效。
    public Map<String, String> render(String code, Map<String, Object> context, String fallbackTitle, String fallbackBody) {
        String normalizedCode = code == null ? null : code.trim().toUpperCase();
        NotificationTemplate template = null;
        if (normalizedCode != null && !normalizedCode.isEmpty()) {
            LambdaQueryWrapper<NotificationTemplate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(NotificationTemplate::getCode, normalizedCode)
                    .eq(NotificationTemplate::getStatus, 1)
                    .last("LIMIT 1");
            template = notificationTemplateMapper.selectOne(queryWrapper);
        }

        String title = template == null ? fallbackTitle : template.getTitleTemplate();
        String body = template == null ? fallbackBody : template.getBodyTemplate();

        Map<String, Object> safeContext = context == null ? Map.of() : context;
        String renderedTitle = renderTemplate(title, safeContext);
        String renderedBody = renderTemplate(body, safeContext);

        if (renderedTitle == null || renderedTitle.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Notification title is empty after template rendering");
        }
        if (renderedBody == null || renderedBody.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Notification body is empty after template rendering");
        }

        Map<String, String> result = new HashMap<>();
        result.put("title", renderedTitle.length() > 120 ? renderedTitle.substring(0, 120) : renderedTitle);
        result.put("body", renderedBody.length() > 800 ? renderedBody.substring(0, 800) : renderedBody);
        return result;
    }

    // 简单占位符替换：{{key}} -> value。
    private String renderTemplate(String template, Map<String, Object> context) {
        if (template == null) {
            return "";
        }
        String rendered = template;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace(key, value);
        }
        return rendered;
    }

    private Map<String, Object> toView(NotificationTemplate template) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", template.getId());
        map.put("code", template.getCode());
        map.put("name", template.getName());
        map.put("titleTemplate", template.getTitleTemplate());
        map.put("bodyTemplate", template.getBodyTemplate());
        map.put("status", template.getStatus());
        map.put("createTime", template.getCreateTime());
        map.put("updateTime", template.getUpdateTime());
        return map;
    }

    private String normalizeRequired(String value, int maxLen, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, message);
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }
}
