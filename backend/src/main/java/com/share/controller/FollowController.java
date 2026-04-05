package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.dto.PageResult;
import com.share.security.CurrentUserService;
import com.share.service.UserFollowService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/follow", "/api/follow"})
/**
 * 控制器职责：关注关系与粉丝统计入口。
 * 覆盖关注开关、关注状态、粉丝/关注列表、增长趋势等用户社交核心接口。
 */
public class FollowController {

    private final UserFollowService userFollowService;
    private final CurrentUserService currentUserService;

    public FollowController(UserFollowService userFollowService, CurrentUserService currentUserService) {
        this.userFollowService = userFollowService;
        this.currentUserService = currentUserService;
    }

    /**
     * 关注开关：关注与取关共用同一接口。
     */
    @PostMapping("/toggle")
    public ApiResponse toggleFollow(@RequestParam Long targetUserId) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean isFollowing = userFollowService.toggleFollow(userId, targetUserId);
        Integer followerCount = userFollowService.getFollowerCount(targetUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("followerCount", followerCount);
        return ApiResponse.success(result);
    }

    /**
     * 获取当前用户对目标作者的关注状态，同时返回目标粉丝数。
     */
    @GetMapping("/status")
    public ApiResponse getFollowStatus(@RequestParam Long targetUserId) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean isFollowing = userFollowService.isFollowing(userId, targetUserId);
        Integer followerCount = userFollowService.getFollowerCount(targetUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("followerCount", followerCount);
        return ApiResponse.success(result);
    }

    /**
     * 公开粉丝数查询。
     */
    @GetMapping("/follower-count")
    public ApiResponse getFollowerCount(@RequestParam Long userId) {
        return ApiResponse.success(userFollowService.getFollowerCount(userId));
    }

    /**
     * 公开关注数查询。
     */
    @GetMapping("/following-count")
    public ApiResponse getFollowingCount(@RequestParam Long userId) {
        return ApiResponse.success(userFollowService.getFollowingCount(userId));
    }

    /**
     * 当前登录用户的关注摘要（粉丝总量、新增/取关趋势概览）。
     */
    @GetMapping("/my/summary")
    public ApiResponse getMyFollowSummary() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(userFollowService.getFollowSummary(userId));
    }

    /**
     * 当前登录用户粉丝增长趋势。
     * period 用于切换按天/按月/按年统计口径。
     */
    @GetMapping("/my/follower-growth")
    public ApiResponse getMyFollowerGrowth(@RequestParam(defaultValue = "day") String period,
                                           @RequestParam(required = false) Integer size) {
        Long userId = currentUserService.requireCurrentUserId();
        List<Map<String, Object>> growth = userFollowService.getFollowerGrowth(userId, period, size);
        return ApiResponse.success(growth);
    }

    /**
     * 我的粉丝列表。
     */
    @GetMapping("/my/followers")
    public ApiResponse getMyFollowers(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        PageResult<Map<String, Object>> result = userFollowService.getFollowerList(userId, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 我的关注列表。
     */
    @GetMapping("/my/following")
    public ApiResponse getMyFollowing(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        PageResult<Map<String, Object>> result = userFollowService.getFollowingList(userId, page, pageSize);
        return ApiResponse.success(result);
    }
}
