package com.share.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.entity.Comment;
import com.share.entity.Content;
import com.share.mapper.CommentMapper;
import com.share.mapper.ContentMapper;
import com.share.service.CommentService;
import com.share.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * 服务职责：评论主流程编排。
 * 负责评论创建/删除、回复读取、评论计数同步以及评论互动通知触发。
 */
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;
    private final NotificationService notificationService;

    public CommentServiceImpl(CommentMapper commentMapper,
                              ContentMapper contentMapper,
                              NotificationService notificationService) {
        this.commentMapper = commentMapper;
        this.contentMapper = contentMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public boolean createComment(Comment comment) {
        // 评论创建默认可见；后续若引入评论先审后发，只需在此调整默认 review_status。
        comment.setLikeCount(0);
        comment.setStatus(1);
        if (comment.getReviewStatus() == null || comment.getReviewStatus().isBlank()) {
            comment.setReviewStatus("approved");
        }
        comment.setReviewReason(null);
        comment.setReviewerId(null);
        comment.setReviewTime(null);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        boolean success = save(comment);
        if (!success) {
            return false;
        }

        // 仅顶级评论计入内容 comment_count，回复不重复累加主计数。
        if (comment.getParentId() == null) {
            updateContentCommentCount(comment.getContentId(), 1);
        }
        // 评论成功后给内容作者发互动通知（自己评自己时由通知层去重）。
        notifyContentComment(comment);
        return true;
    }

    @Override
    public List<Comment> getCommentsByContentId(Long contentId, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        return commentMapper.findByContentId(contentId, offset, pageSize);
    }

    @Override
    public List<Comment> getRepliesByCommentId(Long commentId) {
        return commentMapper.findByParentId(commentId);
    }

    @Override
    public Integer getCommentCountByContentId(Long contentId) {
        return commentMapper.countByContentId(contentId);
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null) {
            return false;
        }

        // 逻辑删除保留审计与关联数据，前台按 status 过滤不可见。
        comment.setStatus(0);
        boolean success = updateById(comment);
        if (success && comment.getParentId() == null) {
            updateContentCommentCount(comment.getContentId(), -1);
        }
        return success;
    }

    /**
     * 重新计算内容评论数，避免并发增减导致聚合字段漂移。
     */
    private void updateContentCommentCount(Long contentId, int delta) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            return;
        }
        int latest = Math.max(0, commentMapper.countByContentId(contentId));
        content.setCommentCount(latest);
        contentMapper.updateById(content);
    }

    /**
     * 触发“收到新评论”通知。
     */
    private void notifyContentComment(Comment comment) {
        if (comment == null || comment.getContentId() == null || comment.getUserId() == null) {
            return;
        }
        Content content = contentMapper.selectById(comment.getContentId());
        if (content == null || content.getUserId() == null) {
            return;
        }
        notificationService.createInteractionNotification(
                content.getUserId(),
                comment.getUserId(),
                comment.getContentId(),
                NotificationService.TYPE_CONTENT_COMMENT,
                comment.getCommentContent()
        );
    }
}
