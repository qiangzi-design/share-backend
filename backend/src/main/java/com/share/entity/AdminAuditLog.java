package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_audit_logs")
public class AdminAuditLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long operatorUserId;

    private String action;

    private String targetType;

    private Long targetId;

    private String detailBefore;

    private String detailAfter;

    private String ip;

    private String userAgent;

    private LocalDateTime createTime;
}

