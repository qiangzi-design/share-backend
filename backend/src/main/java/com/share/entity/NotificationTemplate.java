package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification_templates")
public class NotificationTemplate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String titleTemplate;

    private String bodyTemplate;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
