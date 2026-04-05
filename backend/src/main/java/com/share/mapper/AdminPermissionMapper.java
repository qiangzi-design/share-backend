package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.AdminPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：管理权限字典数据访问，维护权限码配置。
 */
public interface AdminPermissionMapper extends BaseMapper<AdminPermission> {
}

