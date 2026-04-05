package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.ContentCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
/**
 * Mapper职责：内容收藏数据访问，维护收藏明细与我的收藏列表。
 */
public interface ContentCollectionMapper extends BaseMapper<ContentCollection> {

    @Select("SELECT * FROM collections WHERE user_id = #{userId} AND content_id = #{contentId} LIMIT 1")
    ContentCollection findByUserIdAndContentId(Long userId, Long contentId);

    @Select("SELECT COUNT(*) FROM collections WHERE content_id = #{contentId}")
    Integer countByContentId(Long contentId);

    @Select("""
            SELECT c.id AS contentId,
                   c.title AS title,
                   c.images AS images,
                   c.like_count AS likeCount,
                   c.collection_count AS collectionCount,
                   c.comment_count AS commentCount,
                   c.view_count AS viewCount,
                   c.create_time AS contentCreateTime,
                   col.create_time AS actionTime,
                   c.user_id AS authorId,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS authorName,
                   u.avatar AS authorAvatar
            FROM collections col
            INNER JOIN contents c ON c.id = col.content_id
            LEFT JOIN users u ON u.id = c.user_id
            WHERE col.user_id = #{userId}
              AND c.status = 1
            ORDER BY col.create_time DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Map<String, Object>> findCollectedContents(@Param("userId") Long userId,
                                                    @Param("offset") Integer offset,
                                                    @Param("pageSize") Integer pageSize);

    @Select("""
            SELECT COUNT(*)
            FROM collections col
            INNER JOIN contents c ON c.id = col.content_id
            WHERE col.user_id = #{userId}
              AND c.status = 1
            """)
    Long countCollectedContents(@Param("userId") Long userId);
}
