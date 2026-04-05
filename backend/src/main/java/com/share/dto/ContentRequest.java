package com.share.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 内容发布请求类
 * 用于接收前端发布内容的请求参数
 */
@Data
public class ContentRequest {
    /**
     * 内容标题
     * 必填，不能为空
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 内容正文
     * 必填，不能为空
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 分类ID
     * 必填，不能为空，关联categories表
     */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /**
     * 标签列表
     * 可选，多个标签用逗号分隔，例如："生活,美食,旅行"
     */
    private String tags;

    /**
     * 图片URL列表
     * 可选，多个图片URL用逗号分隔
     */
    private String images;

    private String videos;
}
