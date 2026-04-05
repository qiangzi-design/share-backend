package com.share.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.dto.PageResult;
import com.share.entity.Content;
import com.share.entity.Like;
import com.share.mapper.ContentMapper;
import com.share.mapper.LikeMapper;
import com.share.service.LikeService;
import com.share.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
/**
 * 服务职责：内容点赞能力与“我的点赞”列表。
 * 提供点赞切换、点赞状态查询、点赞计数同步及互动通知触发。
 */
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    private final LikeMapper likeMapper;
    private final ContentMapper contentMapper;
    private final NotificationService notificationService;

    public LikeServiceImpl(LikeMapper likeMapper,
                           ContentMapper contentMapper,
                           NotificationService notificationService) {
        this.likeMapper = likeMapper;
        this.contentMapper = contentMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long contentId) {
        Like existingLike = likeMapper.findByUserIdAndContentId(userId, contentId);
        if (existingLike != null) {
            // 已点赞 -> 取消点赞，并刷新内容聚合点赞数。
            removeById(existingLike.getId());
            updateContentLikeCount(contentId, -1);
            return false;
        }

        // 未点赞 -> 新增点赞记录并触发通知。
        Like like = new Like();
        like.setUserId(userId);
        like.setContentId(contentId);
        like.setCreateTime(LocalDateTime.now());
        save(like);
        updateContentLikeCount(contentId, 1);
        notifyContentLike(userId, contentId);
        return true;
    }

    @Override
    public boolean isLiked(Long userId, Long contentId) {
        return likeMapper.findByUserIdAndContentId(userId, contentId) != null;
    }

    @Override
    public Integer getLikeCount(Long contentId) {
        return likeMapper.countByContentId(contentId);
    }

    @Override
    public PageResult<Map<String, Object>> getLikedContents(Long userId, Integer page, Integer pageSize) {
        // 分页归一化：避免无效页码和超大 pageSize 导致慢查询。
        int validPage = page == null || page < 1 ? 1 : page;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (validPage - 1) * validPageSize;

        List<Map<String, Object>> list = likeMapper.findLikedContents(userId, offset, validPageSize);
        long total = likeMapper.countLikedContents(userId);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    /**
     * 以明细表重算点赞数，防止并发导致的计数漂移。
     */
    private void updateContentLikeCount(Long contentId, int delta) {
        Content content = contentMapper.selectById(contentId);
        if (content != null) {
            int latest = Math.max(0, likeMapper.countByContentId(contentId));
            content.setLikeCount(latest);
            contentMapper.updateById(content);
        }
    }

    /**
     * 触发“收到新点赞”通知。
     */
    private void notifyContentLike(Long actorId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || content.getUserId() == null) {
            return;
        }
        notificationService.createInteractionNotification(
                content.getUserId(),
                actorId,
                contentId,
                NotificationService.TYPE_CONTENT_LIKE,
                null
        );
    }
}
