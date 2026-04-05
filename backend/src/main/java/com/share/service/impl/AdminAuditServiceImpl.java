package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.dto.PageResult;
import com.share.entity.AdminAuditLog;
import com.share.entity.User;
import com.share.mapper.AdminAuditLogMapper;
import com.share.service.AdminAuditService;
import com.share.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * 服务职责：管理端审计日志写入与查询。
 * 设计目标：所有治理动作都可追溯“谁在何时对什么对象做了什么操作”。
 */
public class AdminAuditServiceImpl implements AdminAuditService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAuditLogMapper auditLogMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AdminAuditServiceImpl(AdminAuditLogMapper auditLogMapper, UserService userService, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    /**
     * 写入审计日志。
     * 副作用：落库 admin_audit_logs，记录前后快照、IP、UA。
     */
    @Override
    public void log(Long operatorUserId,
                    String action,
                    String targetType,
                    Long targetId,
                    Object detailBefore,
                    Object detailAfter,
                    String ip,
                    String userAgent) {
        AdminAuditLog log = new AdminAuditLog();
        log.setOperatorUserId(operatorUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetailBefore(toJson(detailBefore));
        log.setDetailAfter(toJson(detailAfter));
        log.setIp(truncate(ip, 64));
        log.setUserAgent(truncate(userAgent, 255));
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    /**
     * 分页查询审计日志并补齐操作者展示信息。
     */
    @Override
    public PageResult<Map<String, Object>> getAuditLogs(Integer page,
                                                        Integer pageSize,
                                                        Long operatorUserId,
                                                        String action,
                                                        String targetType) {
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<AdminAuditLog> queryWrapper = new LambdaQueryWrapper<>();
        // 仅在有值时应用筛选条件，避免无效参数影响查询计划。
        if (operatorUserId != null && operatorUserId > 0) {
            queryWrapper.eq(AdminAuditLog::getOperatorUserId, operatorUserId);
        }
        if (action != null && !action.isBlank()) {
            queryWrapper.like(AdminAuditLog::getAction, action.trim());
        }
        if (targetType != null && !targetType.isBlank()) {
            queryWrapper.eq(AdminAuditLog::getTargetType, targetType.trim());
        }
        queryWrapper.orderByDesc(AdminAuditLog::getId);

        Page<AdminAuditLog> pageData = auditLogMapper.selectPage(new Page<>(validPage, validPageSize), queryWrapper);
        List<AdminAuditLog> records = pageData.getRecords();

        // 二次批量查用户信息，避免列表渲染时逐条 N+1 查询。
        Set<Long> operatorIds = records.stream()
                .map(AdminAuditLog::getOperatorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(operatorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        List<Map<String, Object>> list = records.stream().map(item -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", item.getId());
            result.put("operatorUserId", item.getOperatorUserId());
            User operator = item.getOperatorUserId() == null ? null : userMap.get(item.getOperatorUserId());
            result.put("operatorUsername", operator == null ? null : operator.getUsername());
            result.put("operatorNickname", operator == null ? null : operator.getNickname());
            result.put("action", item.getAction());
            result.put("targetType", item.getTargetType());
            result.put("targetId", item.getTargetId());
            result.put("detailBefore", item.getDetailBefore());
            result.put("detailAfter", item.getDetailAfter());
            result.put("ip", item.getIp());
            result.put("userAgent", item.getUserAgent());
            result.put("createTime", item.getCreateTime());
            return result;
        }).toList();

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(pageData.getTotal());
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    /**
     * 将对象转为 JSON 快照，序列化失败时回退为字符串，避免影响主流程。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return String.valueOf(value);
        }
    }

    /**
     * 安全截断：防止 IP/UA 等外部字段异常过长。
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 统一页码归一化。
     */
    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    /**
     * 统一分页大小归一化并限制最大值。
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
