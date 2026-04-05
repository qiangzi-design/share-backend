package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.AdminRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：管理角色数据访问，维护角色定义与查询。
 */
public interface AdminRoleMapper extends BaseMapper<AdminRole> {
}

