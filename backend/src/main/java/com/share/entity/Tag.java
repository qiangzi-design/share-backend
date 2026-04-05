package com.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tags")
public class Tag {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer useCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
