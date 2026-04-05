package com.share.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatSendRequest {

    @NotNull(message = "Target user is required")
    private Long targetUserId;

    private String content;

    /**
     * 1 = text, 2 = image
     */
    private Integer messageType;
}
