package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.UserFollow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
/**
 * Mapper职责：关注关系数据访问，支撑粉丝/关注统计与趋势分析。
 */
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Select("SELECT * FROM follows WHERE user_id = #{userId} AND target_user_id = #{targetUserId} LIMIT 1")
    UserFollow findByUserIdAndTargetUserId(Long userId, Long targetUserId);

    @Select("SELECT COUNT(*) FROM follows WHERE target_user_id = #{targetUserId}")
    Integer countFollowers(Long targetUserId);

    @Select("SELECT COUNT(*) FROM follows WHERE user_id = #{userId}")
    Integer countFollowing(Long userId);

    @Insert("INSERT INTO follow_events(user_id, target_user_id, event_type, create_time) VALUES(#{userId}, #{targetUserId}, #{eventType}, NOW())")
    void insertFollowEvent(@Param("userId") Long userId,
                           @Param("targetUserId") Long targetUserId,
                           @Param("eventType") Integer eventType);

    @Select("SELECT COUNT(*) FROM follow_events WHERE target_user_id = #{targetUserId} AND event_type = 2")
    Integer countUnfollowEvents(@Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS bucket, COUNT(*) AS total
            FROM follow_events
            WHERE target_user_id = #{targetUserId}
              AND event_type = #{eventType}
              AND create_time >= #{startTime}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            ORDER BY bucket ASC
            """)
    List<Map<String, Object>> countFollowEventsByDay(@Param("targetUserId") Long targetUserId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("eventType") Integer eventType);

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m') AS bucket, COUNT(*) AS total
            FROM follow_events
            WHERE target_user_id = #{targetUserId}
              AND event_type = #{eventType}
              AND create_time >= #{startTime}
            GROUP BY DATE_FORMAT(create_time, '%Y-%m')
            ORDER BY bucket ASC
            """)
    List<Map<String, Object>> countFollowEventsByMonth(@Param("targetUserId") Long targetUserId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("eventType") Integer eventType);

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y') AS bucket, COUNT(*) AS total
            FROM follow_events
            WHERE target_user_id = #{targetUserId}
              AND event_type = #{eventType}
              AND create_time >= #{startTime}
            GROUP BY DATE_FORMAT(create_time, '%Y')
            ORDER BY bucket ASC
            """)
    List<Map<String, Object>> countFollowEventsByYear(@Param("targetUserId") Long targetUserId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("eventType") Integer eventType);

    @Select("""
            SELECT u.id AS userId,
                   u.username AS username,
                   u.nickname AS nickname,
                   u.avatar AS avatar,
                   u.bio AS bio,
                   f.create_time AS followTime
            FROM follows f
            INNER JOIN users u ON u.id = f.user_id
            WHERE f.target_user_id = #{userId}
              AND u.status NOT IN (0, 3)
            ORDER BY f.create_time DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Map<String, Object>> findFollowers(@Param("userId") Long userId,
                                            @Param("offset") Integer offset,
                                            @Param("pageSize") Integer pageSize);

    @Select("""
            SELECT u.id AS userId,
                   u.username AS username,
                   u.nickname AS nickname,
                   u.avatar AS avatar,
                   u.bio AS bio,
                   f.create_time AS followTime
            FROM follows f
            INNER JOIN users u ON u.id = f.target_user_id
            WHERE f.user_id = #{userId}
              AND u.status NOT IN (0, 3)
            ORDER BY f.create_time DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Map<String, Object>> findFollowingUsers(@Param("userId") Long userId,
                                                 @Param("offset") Integer offset,
                                                 @Param("pageSize") Integer pageSize);

    @Select("""
            SELECT COUNT(*)
            FROM follows f
            INNER JOIN users u ON u.id = f.user_id
            WHERE f.target_user_id = #{userId}
              AND u.status NOT IN (0, 3)
            """)
    Long countFollowersForList(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM follows f
            INNER JOIN users u ON u.id = f.target_user_id
            WHERE f.user_id = #{userId}
              AND u.status NOT IN (0, 3)
            """)
    Long countFollowingForList(@Param("userId") Long userId);
}
