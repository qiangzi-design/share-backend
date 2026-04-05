package com.share.service;

import com.share.dto.PageResult;

import java.util.Map;

public interface AdminTaxonomyService {

    PageResult<Map<String, Object>> getCategories(Integer page, Integer pageSize, String keyword, Integer status);

    Map<String, Object> createCategory(Long operatorUserId, String name, String description, Integer sortOrder, String ip, String userAgent);

    Map<String, Object> updateCategory(Long operatorUserId, Long categoryId, String name, String description, Integer sortOrder, String ip, String userAgent);

    Map<String, Object> changeCategoryStatus(Long operatorUserId, Long categoryId, Integer status, String ip, String userAgent);

    PageResult<Map<String, Object>> getTags(Integer page, Integer pageSize, String keyword, Integer status);

    Map<String, Object> createTag(Long operatorUserId, String name, String ip, String userAgent);

    Map<String, Object> updateTag(Long operatorUserId, Long tagId, String name, String ip, String userAgent);

    Map<String, Object> changeTagStatus(Long operatorUserId, Long tagId, Integer status, String ip, String userAgent);

    PageResult<Map<String, Object>> getTagContents(Long tagId, Integer page, Integer pageSize);
}
