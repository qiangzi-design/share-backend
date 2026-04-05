package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.AdminUserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：用户-角色关联数据访问，支撑管理员角色绑定。
 */
public interface AdminUserRoleMapper extends BaseMapper<AdminUserRole> {
}

