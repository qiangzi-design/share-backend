package com.share.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminRiskMarkRequest {

    @Pattern(regexp = "^(low|medium|high)$", message = "riskLevel must be low, medium or high")
    private String riskLevel;

    private String riskNote;
}
