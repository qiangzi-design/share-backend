package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcements")
public class Announcement {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String body;

    private String status;

    private Boolean isPinned;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime publishTime;

    private Long creatorId;

    private Long updaterId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
