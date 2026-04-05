package com.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportHandleRequest {

    @NotBlank(message = "decision is required")
    @Size(max = 32, message = "decision max length is 32")
    private String decision;

    @Size(max = 64, message = "action max length is 64")
    private String action;

    @Size(max = 64, message = "violationTemplateCode max length is 64")
    private String violationTemplateCode;

    @Size(max = 80, message = "violationTemplateLabel max length is 80")
    private String violationTemplateLabel;

    @Size(max = 500, message = "violationReason max length is 500")
    private String violationReason;

    @Size(max = 500, message = "handleNote max length is 500")
    private String handleNote;
}
