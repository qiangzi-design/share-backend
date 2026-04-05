package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.AdminRolePermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：角色-权限关联数据访问，支撑 RBAC 授权关系。
 */
public interface AdminRolePermissionMapper extends BaseMapper<AdminRolePermission> {
}

