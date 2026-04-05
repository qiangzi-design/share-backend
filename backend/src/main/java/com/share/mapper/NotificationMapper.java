package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
/**
 * Mapper职责：互动通知数据访问，支撑消息中心列表与未读统计。
 */
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM notifications n",
            "WHERE n.receiver_id = #{receiverId}",
            "  AND n.is_read = 0",
            "  <if test='category != null and category != \"\" and category != \"all\"'>",
            "    <choose>",
            "      <when test='category == \"system\"'>",
            "        AND n.type = 'system_notice'",
            "      </when>",
            "      <otherwise>",
            "        AND (n.type IS NULL OR n.type &lt;&gt; 'system_notice')",
            "      </otherwise>",
            "    </choose>",
            "  </if>",
            "</script>"
    })
    Long countUnread(@Param("receiverId") Long receiverId, @Param("category") String category);

    @Select({
            "<script>",
            "SELECT n.id,",
            "       n.receiver_id AS receiverId,",
            "       n.actor_id AS actorId,",
            "       n.content_id AS contentId,",
            "       n.type,",
            "       n.title,",
            "       n.body,",
            "       n.is_read AS isRead,",
            "       n.read_time AS readTime,",
            "       n.create_time AS createTime,",
            "       u.username AS actorUsername,",
            "       u.nickname AS actorNickname,",
            "       u.avatar AS actorAvatar,",
            "       c.title AS contentTitle",
            "FROM notifications n",
            "LEFT JOIN users u ON u.id = n.actor_id",
            "LEFT JOIN contents c ON c.id = n.content_id",
            "WHERE n.receiver_id = #{receiverId}",
            "  <if test='category != null and category != \"\" and category != \"all\"'>",
            "    <choose>",
            "      <when test='category == \"system\"'>",
            "        AND n.type = 'system_notice'",
            "      </when>",
            "      <otherwise>",
            "        AND (n.type IS NULL OR n.type &lt;&gt; 'system_notice')",
            "      </otherwise>",
            "    </choose>",
            "  </if>",
            "ORDER BY n.id DESC",
            "LIMIT #{pageSize} OFFSET #{offset}",
            "</script>"
    })
    List<Map<String, Object>> findPage(@Param("receiverId") Long receiverId,
                                       @Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize,
                                       @Param("category") String category);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM notifications n",
            "WHERE n.receiver_id = #{receiverId}",
            "  <if test='category != null and category != \"\" and category != \"all\"'>",
            "    <choose>",
            "      <when test='category == \"system\"'>",
            "        AND n.type = 'system_notice'",
            "      </when>",
            "      <otherwise>",
            "        AND (n.type IS NULL OR n.type &lt;&gt; 'system_notice')",
            "      </otherwise>",
            "    </choose>",
            "  </if>",
            "</script>"
    })
    Long countAll(@Param("receiverId") Long receiverId, @Param("category") String category);

    @Update("""
            UPDATE notifications
            SET is_read = 1,
                read_time = NOW()
            WHERE receiver_id = #{receiverId}
              AND is_read = 0
            """)
    int markAllRead(@Param("receiverId") Long receiverId);

    @Update("""
            UPDATE notifications
            SET is_read = 1,
                read_time = NOW()
            WHERE receiver_id = #{receiverId}
              AND id = #{id}
              AND is_read = 0
            """)
    int markRead(@Param("receiverId") Long receiverId, @Param("id") Long id);
}
