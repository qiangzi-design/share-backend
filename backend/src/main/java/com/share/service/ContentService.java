package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.dto.ContentUpdateRequest;
import com.share.dto.PageResult;
import com.share.entity.Content;

import java.util.Map;

public interface ContentService extends IService<Content> {

    boolean publish(Content content);

    PageResult<Content> getPublishedContents(Integer page, Integer pageSize, Long categoryId, String keyword, String sort);

    Content getPublishedContentById(Long id);

    Content getContentDetailByViewer(Long id, Long viewerUserId);

    boolean updateContent(Long contentId, Long userId, ContentUpdateRequest request);

    boolean deleteContent(Long contentId, Long userId);

    PageResult<Content> getPublishedContentsByUserId(Long userId, Integer page, Integer pageSize, String sort);

    PageResult<Content> getContentsByOwner(Long userId, Integer page, Integer pageSize, String sort);

    Map<String, Object> recordContentView(Long contentId, Long viewerUserId, String viewerKey, String ip, String userAgent);
}
