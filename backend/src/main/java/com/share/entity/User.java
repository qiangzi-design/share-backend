package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("users")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String email;

    private String nickname;

    private String avatar;

    private String bio;

    /**
     * 用户状态:
     * 1 = 正常
     * 2 = 禁言
     * 3 = 封禁
     */
    private Integer status;

    private LocalDateTime muteUntil;

    private String banReason;

    private LocalDateTime banTime;

    private String riskLevel;

    private String riskNote;

    private LocalDateTime riskMarkTime;

    private Long riskMarkBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
