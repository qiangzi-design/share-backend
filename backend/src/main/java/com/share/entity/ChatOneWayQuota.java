package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("chat_oneway_quota")
public class ChatOneWayQuota {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long senderId;

    private Long receiverId;

    private LocalDateTime usedAt;
}
