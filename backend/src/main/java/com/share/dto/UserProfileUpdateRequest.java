package com.share.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户资料更新请求
 */
@Data
public class UserProfileUpdateRequest {
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;
}

