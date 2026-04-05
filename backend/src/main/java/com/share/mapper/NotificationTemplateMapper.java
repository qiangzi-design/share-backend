package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.NotificationTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：系统消息模板数据访问，支撑治理通知模板配置。
 */
public interface NotificationTemplateMapper extends BaseMapper<NotificationTemplate> {
}
