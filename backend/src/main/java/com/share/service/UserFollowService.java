package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.dto.PageResult;
import com.share.entity.UserFollow;

import java.util.List;
import java.util.Map;

public interface UserFollowService extends IService<UserFollow> {

    boolean toggleFollow(Long userId, Long targetUserId);

    boolean isFollowing(Long userId, Long targetUserId);

    Integer getFollowerCount(Long userId);

    Integer getFollowingCount(Long userId);

    Integer getUnfollowCount(Long userId);

    Map<String, Object> getFollowSummary(Long userId);

    List<Map<String, Object>> getFollowerGrowth(Long userId, String period, Integer size);

    PageResult<Map<String, Object>> getFollowerList(Long userId, Integer page, Integer pageSize);

    PageResult<Map<String, Object>> getFollowingList(Long userId, Integer page, Integer pageSize);
}
