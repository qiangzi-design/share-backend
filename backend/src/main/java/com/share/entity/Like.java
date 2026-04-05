package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 点赞实体类
 * 对应数据库中的likes表，记录用户对内容的点赞记录
 * like是SQL关键字，所以表名需要用反引号转义
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("likes")  // 指定表名为likes（与数据库表名一致）
public class Like {
    /**
     * 点赞记录ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联用户表
     */
    private Long userId;

    /**
     * 内容ID，关联内容表
     */
    private Long contentId;

    /**
     * 点赞时间
     */
    private LocalDateTime createTime;
}
