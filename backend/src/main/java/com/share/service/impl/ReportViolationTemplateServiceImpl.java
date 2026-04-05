package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.dto.PageResult;
import com.share.entity.ReportViolationTemplate;
import com.share.exception.BusinessException;
import com.share.mapper.ReportViolationTemplateMapper;
import com.share.service.AdminAuditService;
import com.share.service.ReportViolationTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 举报违规模板服务：
 * - 用户侧读取启用模板用于快捷举报；
 * - 管理侧维护模板（增改启停），并记录审计日志。
 */
public class ReportViolationTemplateServiceImpl implements ReportViolationTemplateService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportViolationTemplateMapper reportViolationTemplateMapper;
    private final AdminAuditService adminAuditService;

    public ReportViolationTemplateServiceImpl(ReportViolationTemplateMapper reportViolationTemplateMapper,
                                              AdminAuditService adminAuditService) {
        this.reportViolationTemplateMapper = reportViolationTemplateMapper;
        this.adminAuditService = adminAuditService;
    }

    @Override
    // 用户侧仅返回启用模板，按 sortOrder 保持稳定顺序。
    public List<Map<String, Object>> listActiveTemplates() {
        LambdaQueryWrapper<ReportViolationTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportViolationTemplate::getStatus, 1)
                .orderByAsc(ReportViolationTemplate::getSortOrder)
                .orderByAsc(ReportViolationTemplate::getId);
        return reportViolationTemplateMapper.selectList(queryWrapper).stream()
                .map(this::toPublicView)
                .toList();
    }

    @Override
    // 解析 code 时只接受启用模板，禁用模板不再用于新举报。
    public String resolveActiveLabelByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return null;
        }
        LambdaQueryWrapper<ReportViolationTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportViolationTemplate::getCode, normalizedCode)
                .eq(ReportViolationTemplate::getStatus, 1)
                .last("LIMIT 1");
        ReportViolationTemplate template = reportViolationTemplateMapper.selectOne(queryWrapper);
        return template == null ? null : template.getLabel();
    }

    @Override
    // 管理端模板列表支持关键字和状态筛选。
    public PageResult<Map<String, Object>> getTemplates(Integer page, Integer pageSize, String keyword, Integer status) {
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<ReportViolationTemplate> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(ReportViolationTemplate::getCode, kw)
                    .or()
                    .like(ReportViolationTemplate::getLabel, kw)
                    .or()
                    .like(ReportViolationTemplate::getDescription, kw));
        }
        if (status != null) {
            queryWrapper.eq(ReportViolationTemplate::getStatus, status);
        }
        queryWrapper.orderByAsc(ReportViolationTemplate::getSortOrder)
                .orderByAsc(ReportViolationTemplate::getId);

        Page<ReportViolationTemplate> pageData = reportViolationTemplateMapper.selectPage(
                new Page<>(validPage, validPageSize),
                queryWrapper
        );

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(pageData.getRecords().stream().map(this::toAdminView).toList());
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    @Transactional
    // 新建模板需保证 code/label 唯一，避免用户侧出现重复项。
    public Map<String, Object> createTemplate(Long operatorUserId,
                                              String code,
                                              String label,
                                              String description,
                                              Integer sortOrder,
                                              String ip,
                                              String userAgent) {
        String normalizedCode = normalizeRequiredCode(code);
        String normalizedLabel = normalizeRequired(label, 80, "template label is required");
        ensureCodeUnique(normalizedCode, null);
        ensureLabelUnique(normalizedLabel, null);

        ReportViolationTemplate template = new ReportViolationTemplate();
        template.setCode(normalizedCode);
        template.setLabel(normalizedLabel);
        template.setDescription(normalizeOptional(description, 255));
        template.setStatus(1);
        template.setSortOrder(sortOrder == null ? 0 : sortOrder);
        template.setIsSystem(0);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        reportViolationTemplateMapper.insert(template);

        Map<String, Object> after = toAdminView(template);
        adminAuditService.log(operatorUserId, "admin.report.template.create", "report_template", template.getId(), null, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    // 更新模板允许调整排序与描述，但仍保持 code/label 全局唯一。
    public Map<String, Object> updateTemplate(Long operatorUserId,
                                              Long templateId,
                                              String code,
                                              String label,
                                              String description,
                                              Integer sortOrder,
                                              String ip,
                                              String userAgent) {
        ReportViolationTemplate template = mustGetTemplate(templateId);
        Map<String, Object> before = toAdminView(template);

        String normalizedCode = normalizeRequiredCode(code);
        String normalizedLabel = normalizeRequired(label, 80, "template label is required");
        ensureCodeUnique(normalizedCode, templateId);
        ensureLabelUnique(normalizedLabel, templateId);

        template.setCode(normalizedCode);
        template.setLabel(normalizedLabel);
        template.setDescription(normalizeOptional(description, 255));
        template.setSortOrder(sortOrder == null ? template.getSortOrder() : sortOrder);
        template.setUpdateTime(LocalDateTime.now());
        reportViolationTemplateMapper.updateById(template);

        Map<String, Object> after = toAdminView(template);
        adminAuditService.log(operatorUserId, "admin.report.template.update", "report_template", templateId, before, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    // 启停模板不删历史记录，便于后续复用与追溯。
    public Map<String, Object> changeTemplateStatus(Long operatorUserId,
                                                    Long templateId,
                                                    Integer status,
                                                    String ip,
                                                    String userAgent) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "template status must be 0 or 1");
        }
        ReportViolationTemplate template = mustGetTemplate(templateId);
        Map<String, Object> before = toAdminView(template);

        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        reportViolationTemplateMapper.updateById(template);

        Map<String, Object> after = toAdminView(template);
        adminAuditService.log(
                operatorUserId,
                status == 1 ? "admin.report.template.enable" : "admin.report.template.disable",
                "report_template",
                templateId,
                before,
                after,
                ip,
                userAgent
        );
        return after;
    }

    private ReportViolationTemplate mustGetTemplate(Long templateId) {
        if (templateId == null || templateId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "invalid report template id");
        }
        ReportViolationTemplate template = reportViolationTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "report template not found");
        }
        return template;
    }

    private void ensureCodeUnique(String code, Long ignoreId) {
        LambdaQueryWrapper<ReportViolationTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportViolationTemplate::getCode, code);
        if (ignoreId != null) {
            queryWrapper.ne(ReportViolationTemplate::getId, ignoreId);
        }
        if (reportViolationTemplateMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "template code already exists");
        }
    }

    private void ensureLabelUnique(String label, Long ignoreId) {
        LambdaQueryWrapper<ReportViolationTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReportViolationTemplate::getLabel, label);
        if (ignoreId != null) {
            queryWrapper.ne(ReportViolationTemplate::getId, ignoreId);
        }
        if (reportViolationTemplateMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "template label already exists");
        }
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9_\\-]", "");
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private String normalizeRequiredCode(String value) {
        String normalized = normalizeCode(value);
        if (normalized == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "template code is required");
        }
        return normalized;
    }

    private String normalizeRequired(String value, int maxLength, String message) {
        String normalized = normalizeOptional(value, maxLength);
        if (normalized == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, message);
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
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

    private Map<String, Object> toPublicView(ReportViolationTemplate template) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", template.getCode());
        map.put("label", template.getLabel());
        map.put("description", template.getDescription());
        return map;
    }

    private Map<String, Object> toAdminView(ReportViolationTemplate template) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", template.getId());
        map.put("code", template.getCode());
        map.put("label", template.getLabel());
        map.put("description", template.getDescription());
        map.put("status", template.getStatus());
        map.put("sortOrder", template.getSortOrder());
        map.put("isSystem", template.getIsSystem());
        map.put("createTime", template.getCreateTime());
        map.put("updateTime", template.getUpdateTime());
        return map;
    }
}
