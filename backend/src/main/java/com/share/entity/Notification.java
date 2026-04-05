package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("notifications")
public class Notification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long receiverId;

    private Long actorId;

    private Long contentId;

    private String type;

    private String title;

    private String body;

    private Boolean isRead;

    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
