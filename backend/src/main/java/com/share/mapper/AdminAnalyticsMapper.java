package com.share.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
/**
 * 管理端运营分析 SQL 映射。
 * 目标：提供可直接喂给看板的聚合结果，减少服务层重复拼装成本。
 */
public interface AdminAnalyticsMapper {

    /** 内容质量（日）：按内容创建时间汇总赞藏评浏览。 */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label,
                   SUM(IFNULL(like_count, 0)) AS likeCount,
                   SUM(IFNULL(collection_count, 0)) AS collectionCount,
                   SUM(IFNULL(comment_count, 0)) AS commentCount,
                   SUM(IFNULL(view_count, 0)) AS viewCount,
                   COUNT(*) AS contentCount
            FROM contents
            WHERE create_time >= #{fromTime}
              AND status = 1
              AND (review_status = 'approved' OR review_status IS NULL)
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY label
            """)
    List<Map<String, Object>> listContentQualityDay(LocalDateTime fromTime);

    /** 内容质量（月）：按月汇总赞藏评浏览。 */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m') AS label,
                   SUM(IFNULL(like_count, 0)) AS likeCount,
                   SUM(IFNULL(collection_count, 0)) AS collectionCount,
                   SUM(IFNULL(comment_count, 0)) AS commentCount,
                   SUM(IFNULL(view_count, 0)) AS viewCount,
                   COUNT(*) AS contentCount
            FROM contents
            WHERE create_time >= #{fromTime}
              AND status = 1
              AND (review_status = 'approved' OR review_status IS NULL)
            GROUP BY DATE_FORMAT(create_time, '%Y-%m')
            ORDER BY label
            """)
    List<Map<String, Object>> listContentQualityMonth(LocalDateTime fromTime);

    /** 新增用户（日）。 */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label,
                   COUNT(*) AS newUserCount
            FROM users
            WHERE create_time >= #{fromTime}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY label
            """)
    List<Map<String, Object>> listNewUsersDay(LocalDateTime fromTime);

    /** 新增用户（月）。 */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m') AS label,
                   COUNT(*) AS newUserCount
            FROM users
            WHERE create_time >= #{fromTime}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m')
            ORDER BY label
            """)
    List<Map<String, Object>> listNewUsersMonth(LocalDateTime fromTime);

    /** 活跃用户（日）：跨内容/评论/点赞/收藏/私聊去重用户。 */
    @Select("""
            SELECT t.label,
                   COUNT(DISTINCT t.user_id) AS activeUserCount
            FROM (
                SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, user_id
                FROM contents
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, user_id
                FROM comments
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, user_id
                FROM likes
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, user_id
                FROM collections
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS label, sender_id AS user_id
                FROM chat_messages
                WHERE create_time >= #{fromTime} AND sender_id IS NOT NULL
            ) t
            GROUP BY t.label
            ORDER BY t.label
            """)
    List<Map<String, Object>> listActiveUsersDay(LocalDateTime fromTime);

    /** 活跃用户（月）：跨行为表按月去重用户。 */
    @Select("""
            SELECT t.label,
                   COUNT(DISTINCT t.user_id) AS activeUserCount
            FROM (
                SELECT DATE_FORMAT(create_time, '%Y-%m') AS label, user_id
                FROM contents
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m') AS label, user_id
                FROM comments
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m') AS label, user_id
                FROM likes
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m') AS label, user_id
                FROM collections
                WHERE create_time >= #{fromTime} AND user_id IS NOT NULL
                UNION ALL
                SELECT DATE_FORMAT(create_time, '%Y-%m') AS label, sender_id AS user_id
                FROM chat_messages
                WHERE create_time >= #{fromTime} AND sender_id IS NOT NULL
            ) t
            GROUP BY t.label
            ORDER BY t.label
            """)
    List<Map<String, Object>> listActiveUsersMonth(LocalDateTime fromTime);

    /** 审核效率（日）：内容与评论统一按 review_time 聚合。 */
    @Select("""
            SELECT t.label AS label,
                   COUNT(*) AS processedCount,
                   SUM(CASE WHEN t.review_status = 'approved' THEN 1 ELSE 0 END) AS resolvedCount,
                   AVG(t.handleMinutes) AS avgHandleMinutes
            FROM (
                SELECT DATE_FORMAT(review_time, '%Y-%m-%d') AS label,
                       review_status,
                       TIMESTAMPDIFF(MINUTE, create_time, review_time) AS handleMinutes
                FROM contents
                WHERE review_time >= #{fromTime}
                  AND review_status IN ('approved', 'rejected')
                UNION ALL
                SELECT DATE_FORMAT(review_time, '%Y-%m-%d') AS label,
                       review_status,
                       TIMESTAMPDIFF(MINUTE, create_time, review_time) AS handleMinutes
                FROM comments
                WHERE review_time >= #{fromTime}
                  AND review_status IN ('approved', 'rejected')
            ) t
            GROUP BY t.label
            ORDER BY label
            """)
    List<Map<String, Object>> listReportEfficiencyDay(LocalDateTime fromTime);

    /** 审核效率（月）。 */
    @Select("""
            SELECT t.label AS label,
                   COUNT(*) AS processedCount,
                   SUM(CASE WHEN t.review_status = 'approved' THEN 1 ELSE 0 END) AS resolvedCount,
                   AVG(t.handleMinutes) AS avgHandleMinutes
            FROM (
                SELECT DATE_FORMAT(review_time, '%Y-%m') AS label,
                       review_status,
                       TIMESTAMPDIFF(MINUTE, create_time, review_time) AS handleMinutes
                FROM contents
                WHERE review_time >= #{fromTime}
                  AND review_status IN ('approved', 'rejected')
                UNION ALL
                SELECT DATE_FORMAT(review_time, '%Y-%m') AS label,
                       review_status,
                       TIMESTAMPDIFF(MINUTE, create_time, review_time) AS handleMinutes
                FROM comments
                WHERE review_time >= #{fromTime}
                  AND review_status IN ('approved', 'rejected')
            ) t
            GROUP BY t.label
            ORDER BY label
            """)
    List<Map<String, Object>> listReportEfficiencyMonth(LocalDateTime fromTime);

    /** 内容审核通过/驳回汇总。 */
    @Select("""
            SELECT SUM(CASE WHEN review_status = 'approved' THEN 1 ELSE 0 END) AS approvedCount,
                   SUM(CASE WHEN review_status = 'rejected' THEN 1 ELSE 0 END) AS rejectedCount,
                   COUNT(*) AS totalCount
            FROM contents
            WHERE review_time >= #{fromTime}
              AND review_status IN ('approved', 'rejected')
            """)
    Map<String, Object> getContentReviewSummary(LocalDateTime fromTime);

    /** 评论审核通过/驳回汇总。 */
    @Select("""
            SELECT SUM(CASE WHEN review_status = 'approved' THEN 1 ELSE 0 END) AS approvedCount,
                   SUM(CASE WHEN review_status = 'rejected' THEN 1 ELSE 0 END) AS rejectedCount,
                   COUNT(*) AS totalCount
            FROM comments
            WHERE review_time >= #{fromTime}
              AND review_status IN ('approved', 'rejected')
            """)
    Map<String, Object> getCommentReviewSummary(LocalDateTime fromTime);
}
