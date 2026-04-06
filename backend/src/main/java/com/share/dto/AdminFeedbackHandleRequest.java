package com.share.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端处理反馈请求：
 * - status 用于表达处理结论；
 * - reply 作为对用户可见的处理反馈。
 */
@Data
public class AdminFeedbackHandleRequest {

    /** 处理状态（resolved/rejected） */
    @NotBlank(message = "status is required")
    @Size(max = 32, message = "status max length is 32")
    private String status;

    /** 处理回复（建议必填，便于和用户形成闭环） */
    @NotBlank(message = "reply is required")
    @Size(max = 1000, message = "reply max length is 1000")
    private String reply;
}

