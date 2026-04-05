package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.AnnouncementRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

@Mapper
/**
 * Mapper职责：公告已读回执数据访问，支撑公告未读统计。
 */
public interface AnnouncementReadMapper extends BaseMapper<AnnouncementRead> {

    @Select("""
            SELECT COUNT(1)
            FROM announcements a
            LEFT JOIN announcement_reads ar
              ON ar.announcement_id = a.id
             AND ar.user_id = #{userId}
            WHERE a.status = 'published'
              AND (a.start_time IS NULL OR a.start_time <= NOW())
              AND (
                    a.end_time IS NULL
                    OR a.end_time >= NOW()
                    OR (DATE(a.end_time) = CURDATE() AND TIME(a.end_time) = '00:00:00')
              )
              AND ar.id IS NULL
            """)
    Long countUnreadAnnouncements(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO announcement_reads(announcement_id, user_id, read_time, create_time)
            SELECT a.id, #{userId}, NOW(), NOW()
            FROM announcements a
            LEFT JOIN announcement_reads ar
              ON ar.announcement_id = a.id
             AND ar.user_id = #{userId}
            WHERE a.status = 'published'
              AND (a.start_time IS NULL OR a.start_time <= NOW())
              AND (
                    a.end_time IS NULL
                    OR a.end_time >= NOW()
                    OR (DATE(a.end_time) = CURDATE() AND TIME(a.end_time) = '00:00:00')
              )
              AND ar.id IS NULL
            """)
    int markAllUnreadAsRead(@Param("userId") Long userId);
}
