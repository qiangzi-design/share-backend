package com.share.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
/**
 * 用户侧分析 SQL 映射。
 * 约束：所有统计都基于“本人内容”且按统一可见性规则过滤，避免跨模块口径不一致。
 */
public interface UserAnalyticsMapper {

    /** 浏览趋势：按粒度聚合内容浏览事件。 */
    @Select("""
            SELECT CASE
                     WHEN #{granularity} = 'month' THEN DATE_FORMAT(e.create_time, '%Y-%m')
                     WHEN #{granularity} = 'week' THEN DATE_FORMAT(e.create_time, '%x-W%v')
                     ELSE DATE_FORMAT(e.create_time, '%Y-%m-%d')
                   END AS label,
                   COUNT(*) AS total
            FROM content_view_events e
            INNER JOIN contents c ON c.id = e.content_id
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
              AND e.create_time >= #{fromTime}
            GROUP BY label
            ORDER BY label
            """)
    List<Map<String, Object>> listViewTrend(@Param("userId") Long userId,
                                            @Param("fromTime") LocalDateTime fromTime,
                                            @Param("granularity") String granularity);

    /** 点赞趋势：统计作用于本人内容的点赞事件。 */
    @Select("""
            SELECT CASE
                     WHEN #{granularity} = 'month' THEN DATE_FORMAT(l.create_time, '%Y-%m')
                     WHEN #{granularity} = 'week' THEN DATE_FORMAT(l.create_time, '%x-W%v')
                     ELSE DATE_FORMAT(l.create_time, '%Y-%m-%d')
                   END AS label,
                   COUNT(*) AS total
            FROM likes l
            INNER JOIN contents c ON c.id = l.content_id
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
              AND l.create_time >= #{fromTime}
            GROUP BY label
            ORDER BY label
            """)
    List<Map<String, Object>> listLikeTrend(@Param("userId") Long userId,
                                            @Param("fromTime") LocalDateTime fromTime,
                                            @Param("granularity") String granularity);

    /** 收藏趋势：统计作用于本人内容的收藏事件。 */
    @Select("""
            SELECT CASE
                     WHEN #{granularity} = 'month' THEN DATE_FORMAT(cl.create_time, '%Y-%m')
                     WHEN #{granularity} = 'week' THEN DATE_FORMAT(cl.create_time, '%x-W%v')
                     ELSE DATE_FORMAT(cl.create_time, '%Y-%m-%d')
                   END AS label,
                   COUNT(*) AS total
            FROM collections cl
            INNER JOIN contents c ON c.id = cl.content_id
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
              AND cl.create_time >= #{fromTime}
            GROUP BY label
            ORDER BY label
            """)
    List<Map<String, Object>> listCollectionTrend(@Param("userId") Long userId,
                                                  @Param("fromTime") LocalDateTime fromTime,
                                                  @Param("granularity") String granularity);

    /** 评论趋势：只计顶级且可见评论，避免回复链噪音。 */
    @Select("""
            SELECT CASE
                     WHEN #{granularity} = 'month' THEN DATE_FORMAT(cm.create_time, '%Y-%m')
                     WHEN #{granularity} = 'week' THEN DATE_FORMAT(cm.create_time, '%x-W%v')
                     ELSE DATE_FORMAT(cm.create_time, '%Y-%m-%d')
                   END AS label,
                   COUNT(*) AS total
            FROM comments cm
            INNER JOIN contents c ON c.id = cm.content_id
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
              AND cm.parent_id IS NULL
              AND cm.status = 1
              AND (cm.review_status = 'approved' OR cm.review_status IS NULL)
              AND cm.create_time >= #{fromTime}
            GROUP BY label
            ORDER BY label
            """)
    List<Map<String, Object>> listCommentTrend(@Param("userId") Long userId,
                                               @Param("fromTime") LocalDateTime fromTime,
                                               @Param("granularity") String granularity);

    /** 粉丝趋势：仅统计关注事件（event_type=1）。 */
    @Select("""
            SELECT CASE
                     WHEN #{granularity} = 'month' THEN DATE_FORMAT(fe.create_time, '%Y-%m')
                     WHEN #{granularity} = 'week' THEN DATE_FORMAT(fe.create_time, '%x-W%v')
                     ELSE DATE_FORMAT(fe.create_time, '%Y-%m-%d')
                   END AS label,
                   COUNT(*) AS total
            FROM follow_events fe
            WHERE fe.target_user_id = #{userId}
              AND fe.event_type = 1
              AND fe.create_time >= #{fromTime}
            GROUP BY label
            ORDER BY label
            """)
    List<Map<String, Object>> listFollowerTrend(@Param("userId") Long userId,
                                                @Param("fromTime") LocalDateTime fromTime,
                                                @Param("granularity") String granularity);

    /** 筛选周期内可见作品集，供标签/分类与发布时间段分析复用。 */
    @Select("""
            SELECT c.id AS contentId,
                   c.title AS title,
                   c.category_id AS categoryId,
                   COALESCE(cat.name, '未分类') AS categoryName,
                   c.tags AS tags,
                   c.create_time AS createTime,
                   IFNULL(c.view_count, 0) AS viewCount,
                   IFNULL(c.like_count, 0) AS likeCount,
                   IFNULL(c.collection_count, 0) AS collectionCount,
                   IFNULL(c.comment_count, 0) AS commentCount
            FROM contents c
            LEFT JOIN categories cat ON cat.id = c.category_id
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
              AND c.create_time >= #{fromTime}
            ORDER BY c.create_time DESC
            """)
    List<Map<String, Object>> listVisibleContentsInRange(@Param("userId") Long userId,
                                                         @Param("fromTime") LocalDateTime fromTime);

    /** Top 作品：按累计浏览量降序（并列按发布时间降序）。 */
    @Select("""
            SELECT c.id AS contentId,
                   c.title AS title,
                   c.category_id AS categoryId,
                   COALESCE(cat.name, '未分类') AS categoryName,
                   c.create_time AS createTime,
                   IFNULL(c.view_count, 0) AS viewCount,
                   IFNULL(c.like_count, 0) AS likeCount,
                   IFNULL(c.collection_count, 0) AS collectionCount,
                   IFNULL(c.comment_count, 0) AS commentCount
            FROM contents c
            LEFT JOIN categories cat ON cat.id = c.category_id
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
            ORDER BY c.view_count DESC, c.create_time DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listTopVisibleContents(@Param("userId") Long userId,
                                                     @Param("limit") Integer limit);

    /** 治理提醒：待审核内容数。 */
    @Select("""
            SELECT COUNT(*)
            FROM contents c
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND (c.review_status = 'pending' OR c.review_status IS NULL)
            """)
    Long countPendingContents(@Param("userId") Long userId);

    /** 治理提醒：已下架内容数（review_status=rejected）。 */
    @Select("""
            SELECT COUNT(*)
            FROM contents c
            WHERE c.user_id = #{userId}
              AND c.status = 1
              AND c.review_status = 'rejected'
            """)
    Long countOffShelfContents(@Param("userId") Long userId);

    /** 治理提醒：全部举报量（按用户内容被举报统计）。 */
    @Select("""
            SELECT COUNT(*)
            FROM reports r
            INNER JOIN contents c ON c.id = r.target_id
            WHERE r.target_type = 'content'
              AND c.user_id = #{userId}
            """)
    Long countReportedContentsAll(@Param("userId") Long userId);

    /** 治理提醒：待处理举报量（pending/assigned）。 */
    @Select("""
            SELECT COUNT(*)
            FROM reports r
            INNER JOIN contents c ON c.id = r.target_id
            WHERE r.target_type = 'content'
              AND c.user_id = #{userId}
              AND r.status IN ('pending', 'assigned')
            """)
    Long countReportedContentsPending(@Param("userId") Long userId);
}
