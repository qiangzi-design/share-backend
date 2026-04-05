package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.dto.PageResult;
import com.share.entity.ContentCollection;

import java.util.Map;

public interface ContentCollectionService extends IService<ContentCollection> {

    boolean toggleCollection(Long userId, Long contentId);

    boolean isCollected(Long userId, Long contentId);

    Integer getCollectionCount(Long contentId);

    PageResult<Map<String, Object>> getCollectedContents(Long userId, Integer page, Integer pageSize);
}
