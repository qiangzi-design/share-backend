package com.share.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportAssignRequest {

    @NotNull(message = "处理人不能为空")
    private Long assigneeUserId;

    @Size(max = 500, message = "处理备注不能超过500字符")
    private String handleNote;
}