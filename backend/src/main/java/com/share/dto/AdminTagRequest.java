package com.share.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminTagRequest {

    @NotBlank(message = "Tag name is required")
    private String name;
}
