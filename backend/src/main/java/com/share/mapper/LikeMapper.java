package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
/**
 * Mapper职责：内容点赞数据访问，维护点赞明细与点赞列表。
 */
public interface LikeMapper extends BaseMapper<Like> {

    @Select("SELECT * FROM likes WHERE user_id = #{userId} AND content_id = #{contentId} LIMIT 1")
    Like findByUserIdAndContentId(Long userId, Long contentId);

    @Select("SELECT COUNT(*) FROM likes WHERE content_id = #{contentId}")
    Integer countByContentId(Long contentId);

    @Select("SELECT * FROM likes WHERE user_id = #{userId}")
    List<Like> findByUserId(Long userId);

    @Select("""
            SELECT c.id AS contentId,
                   c.title AS title,
                   c.images AS images,
                   c.like_count AS likeCount,
                   c.collection_count AS collectionCount,
                   c.comment_count AS commentCount,
                   c.view_count AS viewCount,
                   c.create_time AS contentCreateTime,
                   l.create_time AS actionTime,
                   c.user_id AS authorId,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS authorName,
                   u.avatar AS authorAvatar
            FROM likes l
            INNER JOIN contents c ON c.id = l.content_id
            LEFT JOIN users u ON u.id = c.user_id
            WHERE l.user_id = #{userId}
              AND c.status = 1
            ORDER BY l.create_time DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Map<String, Object>> findLikedContents(@Param("userId") Long userId,
                                                @Param("offset") Integer offset,
                                                @Param("pageSize") Integer pageSize);

    @Select("""
            SELECT COUNT(*)
            FROM likes l
            INNER JOIN contents c ON c.id = l.content_id
            WHERE l.user_id = #{userId}
              AND c.status = 1
            """)
    Long countLikedContents(@Param("userId") Long userId);
}
