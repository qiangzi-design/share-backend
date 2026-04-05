package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 评论实体类
 * 对应数据库中的comments表，支持嵌套评论功能
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("comments")  // 指定表名为comments（与数据库表名一致）
public class Comment {
    /**
     * 评论ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评论内容
     */
    private String commentContent;  // 修改字段名为commentContent以匹配数据库字段

    /**
     * 评论者ID，关联用户表
     */
    private Long userId;

    /**
     * 内容ID，关联内容表
     */
    private Long contentId;

    /**
     * 父评论ID，用于嵌套评论（null表示顶级评论）
     */
    private Long parentId;

    /**
     * 点赞次数
     */
    private Integer likeCount;

    /**
     * 评论状态：0-删除，1-正常
     */
    private Integer status;

    /**
     * 审核状态：approved-通过，rejected-隐藏
     */
    private String reviewStatus;

    /**
     * 审核原因
     */
    private String reviewReason;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 评论者用户名（查询扩展字段，不入库）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 评论者昵称（查询扩展字段，不入库）
     */
    @TableField(exist = false)
    private String nickname;

    /**
     * 评论者头像（查询扩展字段，不入库）
     */
    @TableField(exist = false)
    private String avatar;
}
