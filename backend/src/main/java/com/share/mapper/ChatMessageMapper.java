package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
/**
 * Mapper职责：私聊消息数据访问，支撑消息分页与未读统计。
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
            SELECT COUNT(*)
            FROM chat_messages
            WHERE conversation_id = #{conversationId}
            """)
    Long countByConversationId(@Param("conversationId") Long conversationId);

    @Select("""
            SELECT m.id,
                   m.conversation_id AS conversationId,
                   m.sender_id AS senderId,
                   m.receiver_id AS receiverId,
                   m.content,
                   m.message_type AS messageType,
                   m.is_read AS isRead,
                   m.read_time AS readTime,
                   m.create_time AS createTime,
                   u.username AS senderUsername,
                   u.nickname AS senderNickname,
                   u.avatar AS senderAvatar
            FROM chat_messages m
            LEFT JOIN users u ON u.id = m.sender_id
            WHERE m.conversation_id = #{conversationId}
            ORDER BY m.id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Map<String, Object>> findMessagePageDesc(@Param("conversationId") Long conversationId,
                                                  @Param("offset") Integer offset,
                                                  @Param("pageSize") Integer pageSize);

    @Select("""
            SELECT COUNT(*)
            FROM chat_messages
            WHERE receiver_id = #{receiverId}
              AND is_read = 0
            """)
    Long countUnreadByReceiverId(@Param("receiverId") Long receiverId);

    @Update("""
            UPDATE chat_messages
            SET is_read = 1,
                read_time = NOW()
            WHERE conversation_id = #{conversationId}
              AND receiver_id = #{receiverId}
              AND is_read = 0
            """)
    int markConversationRead(@Param("conversationId") Long conversationId,
                             @Param("receiverId") Long receiverId);
}
