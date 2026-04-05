package com.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportCreateRequest {

    @NotBlank(message = "targetType is required")
    private String targetType;

    @NotNull(message = "targetId is required")
    private Long targetId;

    @Size(max = 64, message = "templateCode max length is 64")
    private String templateCode;

    @Size(max = 80, message = "templateLabel max length is 80")
    private String templateLabel;

    @Size(max = 500, message = "reason max length is 500")
    private String reason;
}
