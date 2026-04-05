package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.entity.CommentLike;

public interface CommentLikeService extends IService<CommentLike> {
    boolean toggleLike(Long userId, Long commentId);
    
    boolean isLiked(Long userId, Long commentId);
    
    Integer getLikeCount(Long commentId);
}
