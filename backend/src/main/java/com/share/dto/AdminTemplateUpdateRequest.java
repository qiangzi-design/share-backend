package com.share.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminTemplateUpdateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @NotBlank(message = "Title template is required")
    private String titleTemplate;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    private Integer status;
}
