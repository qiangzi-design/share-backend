package com.share.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportResolveRequest {

    @Size(max = 64, message = "处理动作不能超过64字符")
    private String action;

    @Size(max = 500, message = "处理备注不能超过500字符")
    private String handleNote;
}