package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.CurrentUserService;
import com.share.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞控制器
 * 处理内容点赞相关功能
 */
@RestController
@RequestMapping({"/like", "/api/like"})
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * 切换内容点赞状态
     * @param contentId 内容ID
     * @return 点赞状态和点赞数
     */
    @PostMapping("/toggle")
    public ApiResponse toggleLike(@RequestParam Long contentId) {
        Long userId = currentUserService.requireCurrentUserId();

        // 切换点赞状态
        boolean isLiked = likeService.toggleLike(userId, contentId);
        Integer likeCount = likeService.getLikeCount(contentId);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);

        return ApiResponse.success(result);
    }

    /**
     * 获取内容点赞状态
     * @param contentId 内容ID
     * @return 点赞状态和点赞数
     */
    @GetMapping("/status")
    public ApiResponse getLikeStatus(@RequestParam Long contentId) {
        Long userId = currentUserService.requireCurrentUserId();

        // 获取点赞状态和数量
        boolean isLiked = likeService.isLiked(userId, contentId);
        Integer likeCount = likeService.getLikeCount(contentId);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);

        return ApiResponse.success(result);
    }

    /**
     * 获取内容点赞数
     * @param contentId 内容ID
     * @return 点赞数
     */
    @GetMapping("/count")
    public ApiResponse getLikeCount(@RequestParam Long contentId) {
        Integer likeCount = likeService.getLikeCount(contentId);
        return ApiResponse.success(likeCount);
    }
}
