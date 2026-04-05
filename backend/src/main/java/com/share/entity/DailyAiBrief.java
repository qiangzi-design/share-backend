package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日AI快讯主表实体：
 * - 一天只保留一份快讯（briefDate 唯一）；
 * - 由定时任务或管理员手动触发生成；
 * - 只存“聚合结果”，不保存抓取过程中的原始网页内容。
 */
@Data
@TableName("daily_ai_briefs")
public class DailyAiBrief {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 快讯日期（按天唯一） */
    private LocalDate briefDate;

    /** 快讯标题 */
    private String title;

    /** 快讯摘要 */
    private String summary;

    /** 状态：ready/failed */
    private String status;

    /** 采集来源数量 */
    private Integer sourceCount;

    /** 热点条目数量 */
    private Integer itemCount;

    /** 生成时间 */
    private LocalDateTime generatedAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
