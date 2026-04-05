package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.ContentViewEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
/**
 * Mapper职责：内容浏览事件数据访问，支撑浏览去重与统计分析。
 */
public interface ContentViewEventMapper extends BaseMapper<ContentViewEvent> {

    @Select("""
            SELECT COUNT(1)
            FROM content_view_events
            WHERE content_id = #{contentId}
              AND user_id = #{userId}
              AND create_time >= #{threshold}
            """)
    int countRecentByUser(@Param("contentId") Long contentId,
                          @Param("userId") Long userId,
                          @Param("threshold") LocalDateTime threshold);

    @Select("""
            SELECT COUNT(1)
            FROM content_view_events
            WHERE content_id = #{contentId}
              AND user_id IS NULL
              AND viewer_key = #{viewerKey}
              AND create_time >= #{threshold}
            """)
    int countRecentByViewerKey(@Param("contentId") Long contentId,
                               @Param("viewerKey") String viewerKey,
                               @Param("threshold") LocalDateTime threshold);
}
