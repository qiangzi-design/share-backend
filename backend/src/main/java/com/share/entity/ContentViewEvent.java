package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("content_view_events")
public class ContentViewEvent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long contentId;

    private Long userId;

    private String viewerKey;

    private String ip;

    private String userAgent;

    private LocalDateTime createTime;
}
