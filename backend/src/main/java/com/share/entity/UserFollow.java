package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("follows")
public class UserFollow {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关注者（粉丝）ID
     */
    private Long userId;

    /**
     * 被关注用户ID
     */
    private Long targetUserId;

    private LocalDateTime createTime;
}
