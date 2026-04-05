package com.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportTemplateRequest {

    @NotBlank(message = "template code is required")
    @Size(max = 64, message = "template code max length is 64")
    private String code;

    @NotBlank(message = "template label is required")
    @Size(max = 80, message = "template label max length is 80")
    private String label;

    @Size(max = 255, message = "template description max length is 255")
    private String description;

    private Integer sortOrder;
}
