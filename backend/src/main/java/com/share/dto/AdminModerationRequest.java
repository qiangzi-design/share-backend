package com.share.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminModerationRequest {

    @Size(max = 500, message = "处理原因不能超过500字符")
    private String reason;
}

