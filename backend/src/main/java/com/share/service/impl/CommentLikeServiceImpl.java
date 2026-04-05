package com.share.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.entity.Comment;
import com.share.entity.CommentLike;
import com.share.mapper.CommentLikeMapper;
import com.share.mapper.CommentMapper;
import com.share.service.CommentLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
/**
 * 服务职责：评论点赞开关与计数维护。
 * 采用“用户-评论”唯一关系，重复点击即执行点赞/取消点赞切换。
 */
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements CommentLikeService {

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long commentId) {
        CommentLike existingLike = commentLikeMapper.findByUserIdAndCommentId(userId, commentId);
        if (existingLike != null) {
            // 已点赞 -> 取消点赞，并同步评论 like_count。
            removeById(existingLike.getId());
            updateCommentLikeCount(commentId, -1);
            return false;
        } else {
            // 未点赞 -> 新增点赞记录，并同步评论 like_count。
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            like.setCreateTime(LocalDateTime.now());
            save(like);
            updateCommentLikeCount(commentId, 1);
            return true;
        }
    }

    @Override
    public boolean isLiked(Long userId, Long commentId) {
        CommentLike like = commentLikeMapper.findByUserIdAndCommentId(userId, commentId);
        return like != null;
    }

    @Override
    public Integer getLikeCount(Long commentId) {
        return commentLikeMapper.countByCommentId(commentId);
    }

    /**
     * 兜底同步评论点赞数，保证聚合字段与明细表一致。
     */
    private void updateCommentLikeCount(Long commentId, int delta) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            int current = comment.getLikeCount() == null ? 0 : comment.getLikeCount();
            comment.setLikeCount(Math.max(0, current + delta));
            commentMapper.updateById(comment);
        }
    }
}
