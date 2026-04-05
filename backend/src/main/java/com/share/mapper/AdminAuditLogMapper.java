package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.AdminAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：管理端审计日志数据访问，支撑治理动作追溯查询。
 */
public interface AdminAuditLogMapper extends BaseMapper<AdminAuditLog> {
}

