package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 分类实体类
 * 对应数据库中的categories表，用于内容分类管理
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("categories")  // 指定表名为categories（与数据库表名一致）
public class Category {
    /**
     * 分类ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重
     */
    private Integer sortOrder;  // 修改字段名为sortOrder以匹配数据库字段

    /**
     * 分类状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}