package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：站内公告数据访问，支撑公告发布与可见性查询。
 */
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
