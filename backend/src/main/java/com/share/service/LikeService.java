package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.dto.PageResult;
import com.share.entity.Like;

import java.util.Map;

public interface LikeService extends IService<Like> {

    boolean toggleLike(Long userId, Long contentId);

    boolean isLiked(Long userId, Long contentId);

    Integer getLikeCount(Long contentId);

    PageResult<Map<String, Object>> getLikedContents(Long userId, Integer page, Integer pageSize);
}
