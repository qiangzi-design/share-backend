package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.share.dto.PageResult;
import com.share.entity.Category;
import com.share.entity.Content;
import com.share.entity.Tag;
import com.share.exception.BusinessException;
import com.share.service.AdminAuditService;
import com.share.service.AdminTaxonomyService;
import com.share.service.CategoryService;
import com.share.service.ContentService;
import com.share.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 管理端分类/标签运营服务：
 * - 分类与标签的增改启停；
 * - 标签关联内容查询；
 * - 所有写操作记录审计日志。
 */
public class AdminTaxonomyServiceImpl implements AdminTaxonomyService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryService categoryService;
    private final TagService tagService;
    private final ContentService contentService;
    private final AdminAuditService adminAuditService;

    public AdminTaxonomyServiceImpl(CategoryService categoryService,
                                    TagService tagService,
                                    ContentService contentService,
                                    AdminAuditService adminAuditService) {
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.contentService = contentService;
        this.adminAuditService = adminAuditService;
    }

    @Override
    // 分类分页查询（关键字 + 状态）。
    public PageResult<Map<String, Object>> getCategories(Integer page, Integer pageSize, String keyword, Integer status) {
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.like(Category::getName, keyword.trim());
        }
        if (status != null) {
            queryWrapper.eq(Category::getStatus, status);
        }
        queryWrapper.orderByAsc(Category::getSortOrder).orderByDesc(Category::getId);

        Page<Category> pageData = categoryService.page(new Page<>(validPage, validPageSize), queryWrapper);
        return toPageResult(pageData.getRecords().stream().map(this::toCategoryView).toList(), pageData.getTotal(), validPage, validPageSize);
    }

    @Override
    @Transactional
    // 新建分类并记录审计日志。
    public Map<String, Object> createCategory(Long operatorUserId, String name, String description, Integer sortOrder, String ip, String userAgent) {
        String normalizedName = normalizeRequired(name, 60, "Category name is required");
        ensureCategoryNameUnique(normalizedName, null);

        Category category = new Category();
        category.setName(normalizedName);
        category.setDescription(normalizeOptional(description, 500));
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        category.setStatus(1);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        categoryService.save(category);

        Map<String, Object> after = toCategoryView(category);
        adminAuditService.log(operatorUserId, "admin.category.create", "category", category.getId(), null, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    // 编辑分类名称/描述/排序。
    public Map<String, Object> updateCategory(Long operatorUserId, Long categoryId, String name, String description, Integer sortOrder, String ip, String userAgent) {
        Category category = mustGetCategory(categoryId);
        Map<String, Object> before = toCategoryView(category);

        String normalizedName = normalizeRequired(name, 60, "Category name is required");
        ensureCategoryNameUnique(normalizedName, categoryId);

        category.setName(normalizedName);
        category.setDescription(normalizeOptional(description, 500));
        category.setSortOrder(sortOrder == null ? category.getSortOrder() : sortOrder);
        category.setUpdateTime(LocalDateTime.now());
        categoryService.updateById(category);

        Map<String, Object> after = toCategoryView(category);
        adminAuditService.log(operatorUserId, "admin.category.update", "category", categoryId, before, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    // 分类启停只改状态，不迁移历史内容。
    public Map<String, Object> changeCategoryStatus(Long operatorUserId, Long categoryId, Integer status, String ip, String userAgent) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Category status must be 0 or 1");
        }
        Category category = mustGetCategory(categoryId);
        Map<String, Object> before = toCategoryView(category);

        category.setStatus(status);
        category.setUpdateTime(LocalDateTime.now());
        categoryService.updateById(category);

        Map<String, Object> after = toCategoryView(category);
        adminAuditService.log(operatorUserId,
                status == 1 ? "admin.category.enable" : "admin.category.disable",
                "category",
                categoryId,
                before,
                after,
                ip,
                userAgent);
        return after;
    }

    @Override
    // 标签分页查询（关键字 + 状态）。
    public PageResult<Map<String, Object>> getTags(Integer page, Integer pageSize, String keyword, Integer status) {
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.like(Tag::getName, keyword.trim());
        }
        if (status != null) {
            queryWrapper.eq(Tag::getStatus, status);
        }
        queryWrapper.orderByDesc(Tag::getUseCount).orderByAsc(Tag::getName);

        Page<Tag> pageData = tagService.page(new Page<>(validPage, validPageSize), queryWrapper);
        return toPageResult(pageData.getRecords().stream().map(this::toTagView).toList(), pageData.getTotal(), validPage, validPageSize);
    }

    @Override
    @Transactional
    // 新建标签。
    public Map<String, Object> createTag(Long operatorUserId, String name, String ip, String userAgent) {
        String normalizedName = normalizeRequired(name, 30, "Tag name is required");
        ensureTagNameUnique(normalizedName, null);

        Tag tag = new Tag();
        tag.setName(normalizedName);
        tag.setUseCount(0);
        tag.setStatus(1);
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        tagService.save(tag);

        Map<String, Object> after = toTagView(tag);
        adminAuditService.log(operatorUserId, "admin.tag.create", "tag", tag.getId(), null, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    // 编辑标签名称。
    public Map<String, Object> updateTag(Long operatorUserId, Long tagId, String name, String ip, String userAgent) {
        Tag tag = mustGetTag(tagId);
        Map<String, Object> before = toTagView(tag);

        String normalizedName = normalizeRequired(name, 30, "Tag name is required");
        ensureTagNameUnique(normalizedName, tagId);

        tag.setName(normalizedName);
        tag.setUpdateTime(LocalDateTime.now());
        tagService.updateById(tag);

        Map<String, Object> after = toTagView(tag);
        adminAuditService.log(operatorUserId, "admin.tag.update", "tag", tagId, before, after, ip, userAgent);
        return after;
    }

    @Override
    @Transactional
    // 标签启停：禁用后仅影响新发/编辑可选项。
    public Map<String, Object> changeTagStatus(Long operatorUserId, Long tagId, Integer status, String ip, String userAgent) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Tag status must be 0 or 1");
        }
        Tag tag = mustGetTag(tagId);
        Map<String, Object> before = toTagView(tag);

        tag.setStatus(status);
        tag.setUpdateTime(LocalDateTime.now());
        tagService.updateById(tag);

        Map<String, Object> after = toTagView(tag);
        adminAuditService.log(operatorUserId,
                status == 1 ? "admin.tag.enable" : "admin.tag.disable",
                "tag",
                tagId,
                before,
                after,
                ip,
                userAgent);
        return after;
    }

    @Override
    // 标签关联内容：仅返回前台可见（已通过审核且上架）内容。
    public PageResult<Map<String, Object>> getTagContents(Long tagId, Integer page, Integer pageSize) {
        Tag tag = mustGetTag(tagId);
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<Content> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Content::getStatus, 1)
                .and(wrapper -> wrapper.eq(Content::getReviewStatus, "approved").or().isNull(Content::getReviewStatus))
                .like(Content::getTags, tag.getName())
                .orderByDesc(Content::getCreateTime);

        Page<Content> pageData = contentService.page(new Page<>(validPage, validPageSize), queryWrapper);
        List<Map<String, Object>> list = pageData.getRecords().stream().map(content -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", content.getId());
            map.put("title", content.getTitle());
            map.put("userId", content.getUserId());
            map.put("tags", content.getTags());
            map.put("createTime", content.getCreateTime());
            return map;
        }).toList();

        return toPageResult(list, pageData.getTotal(), validPage, validPageSize);
    }

    private Category mustGetCategory(Long categoryId) {
        if (categoryId == null || categoryId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid category id");
        }
        Category category = categoryService.getById(categoryId);
        if (category == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Category not found");
        }
        return category;
    }

    private Tag mustGetTag(Long tagId) {
        if (tagId == null || tagId < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid tag id");
        }
        Tag tag = tagService.getById(tagId);
        if (tag == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Tag not found");
        }
        return tag;
    }

    private void ensureCategoryNameUnique(String name, Long ignoreId) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, name);
        if (ignoreId != null) {
            queryWrapper.ne(Category::getId, ignoreId);
        }
        if (categoryService.count(queryWrapper) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Category name already exists");
        }
    }

    private void ensureTagNameUnique(String name, Long ignoreId) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getName, name);
        if (ignoreId != null) {
            queryWrapper.ne(Tag::getId, ignoreId);
        }
        if (tagService.count(queryWrapper) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Tag name already exists");
        }
    }

    private Map<String, Object> toCategoryView(Category category) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", category.getId());
        map.put("name", category.getName());
        map.put("description", category.getDescription());
        map.put("sortOrder", category.getSortOrder());
        map.put("status", category.getStatus());
        map.put("createTime", category.getCreateTime());
        map.put("updateTime", category.getUpdateTime());
        return map;
    }

    private Map<String, Object> toTagView(Tag tag) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tag.getId());
        map.put("name", tag.getName());
        map.put("useCount", tag.getUseCount());
        map.put("status", tag.getStatus());
        map.put("createTime", tag.getCreateTime());
        map.put("updateTime", tag.getUpdateTime());
        return map;
    }

    private String normalizeRequired(String value, int max, String message) {
        String normalized = normalizeOptional(value, max);
        if (normalized == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, message);
        }
        return normalized;
    }

    private String normalizeOptional(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private PageResult<Map<String, Object>> toPageResult(List<Map<String, Object>> list, long total, int page, int pageSize) {
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
}
