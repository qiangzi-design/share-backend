package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.ChatOneWayQuota;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
/**
 * Mapper职责：单向关注发信额度数据访问，约束私聊单向消息次数。
 */
public interface ChatOneWayQuotaMapper extends BaseMapper<ChatOneWayQuota> {

    @Insert("""
            INSERT IGNORE INTO chat_oneway_quota(sender_id, receiver_id, used_at)
            VALUES(#{senderId}, #{receiverId}, NOW())
            """)
    int insertIgnore(@Param("senderId") Long senderId,
                     @Param("receiverId") Long receiverId);

    @Select("""
            SELECT COUNT(*)
            FROM chat_oneway_quota
            WHERE sender_id = #{senderId} AND receiver_id = #{receiverId}
            """)
    int countUsed(@Param("senderId") Long senderId,
                  @Param("receiverId") Long receiverId);
}
