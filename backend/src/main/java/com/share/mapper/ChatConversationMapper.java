package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.ChatConversation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
/**
 * Mapper职责：私聊会话数据访问，支撑会话列表与排序。
 */
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    @Select("""
            SELECT * FROM chat_conversations
            WHERE user_low_id = #{userLowId} AND user_high_id = #{userHighId}
            LIMIT 1
            """)
    ChatConversation findByPair(@Param("userLowId") Long userLowId,
                                @Param("userHighId") Long userHighId);

    @Insert("""
            INSERT IGNORE INTO chat_conversations
            (user_low_id, user_high_id, create_time, update_time)
            VALUES (#{userLowId}, #{userHighId}, NOW(), NOW())
            """)
    int insertIgnore(@Param("userLowId") Long userLowId,
                     @Param("userHighId") Long userHighId);

    @Update("""
            UPDATE chat_conversations
            SET last_message_id = #{lastMessageId},
                last_message_time = #{lastMessageTime},
                update_time = NOW()
            WHERE id = #{conversationId}
            """)
    int updateLastMessage(@Param("conversationId") Long conversationId,
                          @Param("lastMessageId") Long lastMessageId,
                          @Param("lastMessageTime") LocalDateTime lastMessageTime);

    @Select("""
            SELECT c.id AS conversationId,
                   CASE WHEN c.user_low_id = #{currentUserId} THEN c.user_high_id ELSE c.user_low_id END AS peerUserId,
                   u.username AS peerUsername,
                   u.nickname AS peerNickname,
                   u.avatar AS peerAvatar,
                   u.bio AS peerBio,
                   COALESCE(unread.unread_count, 0) AS unreadCount,
                   c.last_message_id AS lastMessageId,
                   c.last_message_time AS lastMessageTime,
                   c.create_time AS createTime,
                   m.sender_id AS lastSenderId,
                   m.message_type AS lastMessageType,
                   CASE WHEN m.message_type = 2 THEN '[图片]' ELSE m.content END AS lastMessage
            FROM chat_conversations c
            INNER JOIN users u
                ON u.id = CASE WHEN c.user_low_id = #{currentUserId} THEN c.user_high_id ELSE c.user_low_id END
            LEFT JOIN chat_messages m ON m.id = c.last_message_id
            LEFT JOIN (
                SELECT conversation_id, COUNT(*) AS unread_count
                FROM chat_messages
                WHERE receiver_id = #{currentUserId}
                  AND is_read = 0
                GROUP BY conversation_id
            ) unread ON unread.conversation_id = c.id
            WHERE (c.user_low_id = #{currentUserId} OR c.user_high_id = #{currentUserId})
              AND u.status NOT IN (0, 3)
            ORDER BY COALESCE(c.last_message_time, c.create_time) DESC, c.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Map<String, Object>> findConversationSummaries(@Param("currentUserId") Long currentUserId,
                                                        @Param("offset") Integer offset,
                                                        @Param("pageSize") Integer pageSize);

    @Select("""
            SELECT COUNT(*)
            FROM chat_conversations c
            INNER JOIN users u
                ON u.id = CASE WHEN c.user_low_id = #{currentUserId} THEN c.user_high_id ELSE c.user_low_id END
            WHERE (c.user_low_id = #{currentUserId} OR c.user_high_id = #{currentUserId})
              AND u.status NOT IN (0, 3)
            """)
    Long countConversations(@Param("currentUserId") Long currentUserId);
}
