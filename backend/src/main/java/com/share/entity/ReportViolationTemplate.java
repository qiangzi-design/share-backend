package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_violation_templates")
public class ReportViolationTemplate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String code;

    private String label;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private Integer isSystem;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
