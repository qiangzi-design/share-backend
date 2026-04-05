package com.share.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminMuteRequest {

    @NotNull(message = "禁言分钟数不能为空")
    @Min(value = 1, message = "禁言时长最少1分钟")
    @Max(value = 60 * 24 * 365, message = "禁言时长不能超过365天")
    private Integer minutes;

    @Size(max = 500, message = "禁言原因不能超过500字符")
    private String reason;
}

