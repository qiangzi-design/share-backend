package com.share.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.dto.ContentUpdateRequest;
import com.share.dto.PageResult;
import com.share.entity.Content;
import com.share.entity.ContentViewEvent;
import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.mapper.CommentMapper;
import com.share.mapper.ContentCollectionMapper;
import com.share.mapper.ContentMapper;
import com.share.mapper.ContentViewEventMapper;
import com.share.mapper.LikeMapper;
import com.share.service.ContentService;
import com.share.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内容服务实现类
 * 实现内容相关的业务逻辑
 */
@Service
public class ContentServiceImpl extends ServiceImpl<ContentMapper, Content> implements ContentService {

    private static final int VIEW_DEDUP_WINDOW_MINUTES = 30;
    private static final String REVIEW_PENDING = "pending";
    private static final String REVIEW_APPROVED = "approved";

    private final UserService userService;
    private final ContentViewEventMapper contentViewEventMapper;
    private final LikeMapper likeMapper;
    private final ContentCollectionMapper contentCollectionMapper;
    private final CommentMapper commentMapper;

    public ContentServiceImpl(UserService userService,
                              ContentViewEventMapper contentViewEventMapper,
                              LikeMapper likeMapper,
                              ContentCollectionMapper contentCollectionMapper,
                              CommentMapper commentMapper) {
        this.userService = userService;
        this.contentViewEventMapper = contentViewEventMapper;
        this.likeMapper = likeMapper;
        this.contentCollectionMapper = contentCollectionMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public boolean publish(Content content) {
        // 发布后默认进入待审核，避免未审核内容直接可见。
        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setCollectionCount(0);
        content.setImageSize(calculateImageSizeBytes(content.getImages()));
        content.setStatus(1);
        content.setReviewStatus(REVIEW_PENDING);
        content.setReviewReason(null);
        content.setReviewerId(null);
        content.setReviewTime(null);
        content.setCreateTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        return save(content);
    }

    @Override
    public PageResult<Content> getPublishedContents(Integer page, Integer pageSize, Long categoryId, String keyword, String sort) {
        // 前台列表只返回“已发布且审核通过（或历史空值）”的内容。
        int validPage = page == null || page < 1 ? 1 : page;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (validPage - 1) * validPageSize;

        LambdaQueryWrapper<Content> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Content::getStatus, 1);
        queryWrapper.and(wrapper -> wrapper
                .eq(Content::getReviewStatus, REVIEW_APPROVED)
                .or()
                .isNull(Content::getReviewStatus));
        if (categoryId != null) {
            queryWrapper.eq(Content::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(Content::getTitle, trimmedKeyword)
                    .or()
                    .like(Content::getContent, trimmedKeyword));
        }
        applySort(queryWrapper, sort);

        long total = count(queryWrapper);
        queryWrapper.last("LIMIT " + offset + "," + validPageSize);
        List<Content> list = list(queryWrapper);
        syncInteractionCounters(list);
        enrichAuthorInfo(list);

        PageResult<Content> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    public Content getPublishedContentById(Long id) {
        // 详情页公开访问口径与列表一致，保证可见性规则统一。
        LambdaQueryWrapper<Content> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Content::getId, id)
                .eq(Content::getStatus, 1)
                .and(wrapper -> wrapper
                        .eq(Content::getReviewStatus, REVIEW_APPROVED)
                        .or()
                        .isNull(Content::getReviewStatus));
        Content content = getOne(queryWrapper, false);
        syncInteractionCounters(content);
        enrichAuthorInfo(content);
        return content;
    }

    @Override
    public Content getContentDetailByViewer(Long id, Long viewerUserId) {
        // 作者本人可查看自己待审核内容；其他用户仅可查看通过内容。
        Content content = getById(id);
        if (content == null || content.getStatus() != 1) {
            return null;
        }

        boolean approved = content.getReviewStatus() == null || REVIEW_APPROVED.equalsIgnoreCase(content.getReviewStatus());
        boolean isOwner = viewerUserId != null && viewerUserId.equals(content.getUserId());
        if (!approved && !isOwner) {
            return null;
        }

        syncInteractionCounters(content);
        enrichAuthorInfo(content);
        return content;
    }

    @Override
    public boolean updateContent(Long contentId, Long userId, ContentUpdateRequest request) {
        // 编辑后重新进入待审核，避免绕过审核直接覆盖线上内容。
        Content existed = getById(contentId);
        if (existed == null || existed.getStatus() != 1 || !existed.getUserId().equals(userId)) {
            return false;
        }

        existed.setTitle(request.getTitle());
        existed.setContent(request.getContent());
        existed.setCategoryId(request.getCategoryId());
        existed.setTags(request.getTags());
        existed.setImages(request.getImages());
        existed.setVideos(request.getVideos());
        existed.setImageSize(calculateImageSizeBytes(request.getImages()));
        existed.setReviewStatus(REVIEW_PENDING);
        existed.setReviewReason(null);
        existed.setReviewerId(null);
        existed.setReviewTime(null);
        existed.setUpdateTime(LocalDateTime.now());
        return updateById(existed);
    }

    @Override
    public boolean deleteContent(Long contentId, Long userId) {
        // 删除采用状态位软删，保留审计与统计可追溯能力。
        Content existed = getById(contentId);
        if (existed == null || existed.getStatus() != 1 || !existed.getUserId().equals(userId)) {
            return false;
        }

        existed.setStatus(2);
        existed.setUpdateTime(LocalDateTime.now());
        return updateById(existed);
    }

    @Override
    public PageResult<Content> getPublishedContentsByUserId(Long userId, Integer page, Integer pageSize, String sort) {
        // 公开主页：仅展示该用户已发布且通过审核的作品。
        int validPage = page == null || page < 1 ? 1 : page;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (validPage - 1) * validPageSize;

        LambdaQueryWrapper<Content> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Content::getUserId, userId)
                .eq(Content::getStatus, 1)
                .and(wrapper -> wrapper
                        .eq(Content::getReviewStatus, REVIEW_APPROVED)
                        .or()
                        .isNull(Content::getReviewStatus));

        applyUserContentSort(queryWrapper, sort);

        long total = count(queryWrapper);
        queryWrapper.last("LIMIT " + offset + "," + validPageSize);
        List<Content> list = list(queryWrapper);
        syncInteractionCounters(list);
        enrichAuthorInfo(list);

        PageResult<Content> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    public PageResult<Content> getContentsByOwner(Long userId, Integer page, Integer pageSize, String sort) {
        // 个人中心：作者可看到自己全部发布状态内容（含待审核）。
        int validPage = page == null || page < 1 ? 1 : page;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (validPage - 1) * validPageSize;

        LambdaQueryWrapper<Content> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Content::getUserId, userId)
                .eq(Content::getStatus, 1);

        applyUserContentSort(queryWrapper, sort);

        long total = count(queryWrapper);
        queryWrapper.last("LIMIT " + offset + "," + validPageSize);
        List<Content> list = list(queryWrapper);
        syncInteractionCounters(list);
        enrichAuthorInfo(list);

        PageResult<Content> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> recordContentView(Long contentId, Long viewerUserId, String viewerKey, String ip, String userAgent) {
        // 浏览去重窗口：同一用户（或同一匿名标识）30分钟内重复进入不重复计数。
        Content content = getPublishedContentById(contentId);
        if (content == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "内容不存在");
        }

        Integer currentViewCount = content.getViewCount() == null ? 0 : content.getViewCount();

        if (viewerUserId != null && viewerUserId.equals(content.getUserId())) {
            // 作者自己浏览不计入浏览量，避免统计失真。
            return buildViewResult(false, currentViewCount);
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(VIEW_DEDUP_WINDOW_MINUTES);
        boolean viewedRecently;
        if (viewerUserId != null) {
            viewedRecently = contentViewEventMapper.countRecentByUser(contentId, viewerUserId, threshold) > 0;
        } else {
            viewedRecently = contentViewEventMapper.countRecentByViewerKey(contentId, viewerKey, threshold) > 0;
        }

        if (!viewedRecently) {
            ContentViewEvent event = new ContentViewEvent();
            event.setContentId(contentId);
            event.setUserId(viewerUserId);
            event.setViewerKey(viewerKey);
            event.setIp(ip);
            event.setUserAgent(userAgent);
            event.setCreateTime(LocalDateTime.now());
            contentViewEventMapper.insert(event);
            baseMapper.incrementViewCount(contentId);
        }

        Integer latestViewCount = baseMapper.getViewCountById(contentId);
        return buildViewResult(!viewedRecently, latestViewCount == null ? currentViewCount : latestViewCount);
    }

    private void applySort(LambdaQueryWrapper<Content> queryWrapper, String sort) {
        if ("popular".equalsIgnoreCase(sort)) {
            queryWrapper.orderByDesc(Content::getLikeCount)
                    .orderByDesc(Content::getCollectionCount)
                    .orderByDesc(Content::getCommentCount)
                    .orderByDesc(Content::getCreateTime);
            return;
        }

        if ("view_desc".equalsIgnoreCase(sort) || "view".equalsIgnoreCase(sort)) {
            queryWrapper.orderByDesc(Content::getViewCount)
                    .orderByDesc(Content::getCreateTime);
            return;
        }

        if ("oldest".equalsIgnoreCase(sort)) {
            queryWrapper.orderByAsc(Content::getCreateTime);
            return;
        }

        queryWrapper.orderByDesc(Content::getCreateTime);
    }

    private void syncInteractionCounters(Content content) {
        if (content == null) {
            return;
        }
        syncInteractionCounters(List.of(content));
    }

    private void syncInteractionCounters(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }

        for (Content content : contents) {
            if (content == null || content.getId() == null) {
                continue;
            }

            Long contentId = content.getId();
            int latestLikeCount = safeCount(likeMapper.countByContentId(contentId));
            int latestCollectionCount = safeCount(contentCollectionMapper.countByContentId(contentId));
            int latestCommentCount = safeCount(commentMapper.countByContentId(contentId));

            boolean changed = !Objects.equals(content.getLikeCount(), latestLikeCount)
                    || !Objects.equals(content.getCollectionCount(), latestCollectionCount)
                    || !Objects.equals(content.getCommentCount(), latestCommentCount);

            content.setLikeCount(latestLikeCount);
            content.setCollectionCount(latestCollectionCount);
            content.setCommentCount(latestCommentCount);

            if (!changed) {
                continue;
            }

            Content patch = new Content();
            patch.setId(contentId);
            patch.setLikeCount(latestLikeCount);
            patch.setCollectionCount(latestCollectionCount);
            patch.setCommentCount(latestCommentCount);
            patch.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(patch);
        }
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private long calculateImageSizeBytes(String images) {
        if (images == null || images.isBlank()) {
            return 0L;
        }

        long totalSize = 0L;
        Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        String[] imageUrls = images.split(",");
        for (String imageUrl : imageUrls) {
            String normalizedUrl = imageUrl == null ? "" : imageUrl.trim();
            if (normalizedUrl.isEmpty()) {
                continue;
            }

            String relativePath;
            if (normalizedUrl.startsWith("/api/uploads/")) {
                relativePath = normalizedUrl.substring("/api/uploads/".length());
            } else if (normalizedUrl.startsWith("/uploads/")) {
                relativePath = normalizedUrl.substring("/uploads/".length());
            } else if (normalizedUrl.startsWith("uploads/")) {
                relativePath = normalizedUrl.substring("uploads/".length());
            } else {
                continue;
            }

            Path filePath = uploadRoot.resolve(relativePath).normalize();
            if (!filePath.startsWith(uploadRoot)) {
                continue;
            }

            try {
                if (Files.isRegularFile(filePath)) {
                    totalSize += Files.size(filePath);
                }
            } catch (IOException ignored) {
                // ignore invalid files
            }
        }
        return totalSize;
    }

    private void enrichAuthorInfo(Content content) {
        if (content == null || content.getUserId() == null) {
            return;
        }
        User user = userService.getById(content.getUserId());
        if (user == null) {
            return;
        }
        content.setAuthorName(resolveDisplayName(user));
        content.setAuthorAvatar(user.getAvatar());
    }

    private void enrichAuthorInfo(Collection<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }

        Set<Long> userIds = contents.stream()
                .map(Content::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (first, second) -> first));

        for (Content content : contents) {
            if (content == null || content.getUserId() == null) {
                continue;
            }
            User user = userMap.get(content.getUserId());
            if (user == null) {
                continue;
            }
            content.setAuthorName(resolveDisplayName(user));
            content.setAuthorAvatar(user.getAvatar());
        }
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername() == null ? "" : user.getUsername();
    }

    private Map<String, Object> buildViewResult(boolean counted, Integer viewCount) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("counted", counted);
        result.put("viewCount", viewCount == null ? 0 : viewCount);
        result.put("dedupMinutes", VIEW_DEDUP_WINDOW_MINUTES);
        return result;
    }

    private void applyUserContentSort(LambdaQueryWrapper<Content> queryWrapper, String sort) {
        if ("popular".equalsIgnoreCase(sort) || "like_desc".equalsIgnoreCase(sort)) {
            queryWrapper.orderByDesc(Content::getLikeCount)
                    .orderByDesc(Content::getCreateTime);
            return;
        }

        if ("view_desc".equalsIgnoreCase(sort) || "view".equalsIgnoreCase(sort)) {
            queryWrapper.orderByDesc(Content::getViewCount)
                    .orderByDesc(Content::getCreateTime);
            return;
        }

        queryWrapper.orderByDesc(Content::getCreateTime);
    }
}

