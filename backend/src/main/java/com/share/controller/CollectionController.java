package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.CurrentUserService;
import com.share.service.ContentCollectionService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/collection", "/api/collection"})
/**
 * 控制器职责：内容收藏入口。
 * 负责收藏开关、收藏状态与收藏计数查询，前端可据此实现“收藏不迷路”。
 */
public class CollectionController {

    private final ContentCollectionService contentCollectionService;
    private final CurrentUserService currentUserService;

    public CollectionController(ContentCollectionService contentCollectionService, CurrentUserService currentUserService) {
        this.contentCollectionService = contentCollectionService;
        this.currentUserService = currentUserService;
    }

    /**
     * 收藏开关：已收藏则取消，未收藏则新增。
     * 返回值同时包含“当前是否收藏 + 最新收藏数”，减少前端二次请求。
     */
    @PostMapping("/toggle")
    public ApiResponse toggleCollection(@RequestParam Long contentId) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean isCollected = contentCollectionService.toggleCollection(userId, contentId);
        Integer collectionCount = contentCollectionService.getCollectionCount(contentId);

        Map<String, Object> result = new HashMap<>();
        result.put("isCollected", isCollected);
        result.put("collectionCount", collectionCount);
        return ApiResponse.success(result);
    }

    /**
     * 获取当前用户对某内容的收藏状态与收藏总数。
     */
    @GetMapping("/status")
    public ApiResponse getCollectionStatus(@RequestParam Long contentId) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean isCollected = contentCollectionService.isCollected(userId, contentId);
        Integer collectionCount = contentCollectionService.getCollectionCount(contentId);

        Map<String, Object> result = new HashMap<>();
        result.put("isCollected", isCollected);
        result.put("collectionCount", collectionCount);
        return ApiResponse.success(result);
    }

    /**
     * 公开收藏总数接口（匿名可读）。
     */
    @GetMapping("/count")
    public ApiResponse getCollectionCount(@RequestParam Long contentId) {
        return ApiResponse.success(contentCollectionService.getCollectionCount(contentId));
    }
}
