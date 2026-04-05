package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日AI快讯条目实体：
 * - 每条记录对应一条热点事件；
 * - 通过 rankOrder 保证展示顺序稳定；
 * - sourceUrl 用于前端“查看来源”跳转。
 */
@Data
@TableName("daily_ai_brief_items")
public class DailyAiBriefItem {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属快讯ID */
    private Long briefId;

    /** 快讯日期（冗余便于聚合） */
    private LocalDate briefDate;

    /** 排序序号（1开始） */
    private Integer rankOrder;

    /** 热度分 */
    private BigDecimal hotScore;

    /** 热点标题 */
    private String title;

    /** 热点摘要 */
    private String summary;

    /** 来源名称 */
    private String sourceName;

    /** 来源链接 */
    private String sourceUrl;

    /** 事件时间 */
    private LocalDateTime eventTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
