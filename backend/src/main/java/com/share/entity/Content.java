package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 内容实体类
 * 对应数据库中的contents表，存储用户发布的分享内容
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("contents")  // 指定表名为contents（与数据库表名一致）
public class Content {
    /**
     * 内容ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 内容标题
     */
    private String title;

    /**
     * 内容正文
     */
    private String content;

    /**
     * 发布者ID，关联用户表
     */
    private Long userId;

    /**
     * 分类ID，关联分类表
     */
    private Long categoryId;

    /**
     * 标签列表，逗号分隔
     */
    private String tags;

    /**
     * 图片列表，逗号分隔的URL
     */
    private String images;

    private String videos;

    /**
     * 图片总大小（字节）
     */
    private Long imageSize;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞次数
     */
    private Integer likeCount;

    /**
     * 评论次数
     */
    private Integer commentCount;

    /**
     * 收藏次数
     */
    private Integer collectionCount;

    /**
     * 内容状态：0-草稿，1-发布，2-删除
     */
    private Integer status;

    /**
     * 审核状态：approved-通过，rejected-下架
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
     * 发布者展示名（非表字段）
     */
    @TableField(exist = false)
    private String authorName;

    /**
     * 发布者头像（非表字段）
     */
    @TableField(exist = false)
    private String authorAvatar;
}
