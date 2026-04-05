package com.share.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.dto.PageResult;
import com.share.entity.Content;
import com.share.entity.ContentCollection;
import com.share.exception.BusinessException;
import com.share.mapper.ContentCollectionMapper;
import com.share.mapper.ContentMapper;
import com.share.service.ContentCollectionService;
import com.share.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
/**
 * 服务职责：内容收藏能力与“我的收藏”列表。
 * 负责收藏切换、收藏计数同步及收藏互动通知触发。
 */
public class ContentCollectionServiceImpl extends ServiceImpl<ContentCollectionMapper, ContentCollection> implements ContentCollectionService {

    private final ContentCollectionMapper contentCollectionMapper;
    private final ContentMapper contentMapper;
    private final NotificationService notificationService;

    public ContentCollectionServiceImpl(ContentCollectionMapper contentCollectionMapper,
                                        ContentMapper contentMapper,
                                        NotificationService notificationService) {
        this.contentCollectionMapper = contentCollectionMapper;
        this.contentMapper = contentMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public boolean toggleCollection(Long userId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        // 仅允许对“存在且在线”的内容进行收藏。
        if (content == null || content.getStatus() == null || content.getStatus() != 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "内容不存在或已下线");
        }

        ContentCollection existing = contentCollectionMapper.findByUserIdAndContentId(userId, contentId);
        if (existing != null) {
            // 已收藏 -> 取消收藏并同步聚合计数。
            removeById(existing.getId());
            updateContentCollectionCount(contentId, -1);
            return false;
        }

        // 未收藏 -> 新增收藏并触发通知。
        ContentCollection collection = new ContentCollection();
        collection.setUserId(userId);
        collection.setContentId(contentId);
        collection.setCreateTime(LocalDateTime.now());
        save(collection);
        updateContentCollectionCount(contentId, 1);
        notifyContentCollection(content, userId, contentId);
        return true;
    }

    @Override
    public boolean isCollected(Long userId, Long contentId) {
        return contentCollectionMapper.findByUserIdAndContentId(userId, contentId) != null;
    }

    @Override
    public Integer getCollectionCount(Long contentId) {
        return contentCollectionMapper.countByContentId(contentId);
    }

    @Override
    public PageResult<Map<String, Object>> getCollectedContents(Long userId, Integer page, Integer pageSize) {
        // 分页参数归一化，避免异常参数放大查询压力。
        int validPage = page == null || page < 1 ? 1 : page;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (validPage - 1) * validPageSize;

        List<Map<String, Object>> list = contentCollectionMapper.findCollectedContents(userId, offset, validPageSize);
        long total = contentCollectionMapper.countCollectedContents(userId);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    /**
     * 重新计算收藏数，确保 contents.collection_count 与明细表一致。
     */
    private void updateContentCollectionCount(Long contentId, int delta) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            return;
        }
        int latest = Math.max(0, contentCollectionMapper.countByContentId(contentId));
        content.setCollectionCount(latest);
        contentMapper.updateById(content);
    }

    /**
     * 触发“收到新收藏”通知。
     */
    private void notifyContentCollection(Content content, Long actorId, Long contentId) {
        if (content == null || content.getUserId() == null) {
            return;
        }
        notificationService.createInteractionNotification(
                content.getUserId(),
                actorId,
                contentId,
                NotificationService.TYPE_CONTENT_COLLECTION,
                null
        );
    }
}
