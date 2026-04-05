package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("comment_likes")
public class CommentLike {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long commentId;

    private LocalDateTime createTime;
}
