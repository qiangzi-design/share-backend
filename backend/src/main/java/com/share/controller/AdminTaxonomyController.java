package com.share.controller;

import com.share.dto.AdminCategoryRequest;
import com.share.dto.AdminTagRequest;
import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.AdminTaxonomyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
/**
 * 管理端分类/标签运营入口：
 * - 分类：增改启停与分页检索。
 * - 标签：增改启停、关联内容查看。
 */
public class AdminTaxonomyController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final AdminTaxonomyService adminTaxonomyService;

    public AdminTaxonomyController(CurrentUserService currentUserService,
                                   AdminAccessService adminAccessService,
                                   AdminTaxonomyService adminTaxonomyService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.adminTaxonomyService = adminTaxonomyService;
    }

    // 分类列表：支持关键字与状态筛选。
    @GetMapping({"/admin/categories", "/api/admin/categories"})
    public ApiResponse getCategories(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "20") Integer pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Integer status) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.CATEGORY_READ);
        return ApiResponse.success(adminTaxonomyService.getCategories(page, pageSize, keyword, status));
    }

    // 新建分类。
    @PostMapping({"/admin/categories", "/api/admin/categories"})
    public ApiResponse createCategory(@Valid @RequestBody AdminCategoryRequest request, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.CATEGORY_WRITE);
        return ApiResponse.success(adminTaxonomyService.createCategory(
                userId,
                request.getName(),
                request.getDescription(),
                request.getSortOrder(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 编辑分类（名称/描述/排序）。
    @PutMapping({"/admin/categories/{id}", "/api/admin/categories/{id}"})
    public ApiResponse updateCategory(@PathVariable("id") Long categoryId,
                                      @Valid @RequestBody AdminCategoryRequest request,
                                      HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.CATEGORY_WRITE);
        return ApiResponse.success(adminTaxonomyService.updateCategory(
                userId,
                categoryId,
                request.getName(),
                request.getDescription(),
                request.getSortOrder(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 启用分类。
    @PostMapping({"/admin/categories/{id}/enable", "/api/admin/categories/{id}/enable"})
    public ApiResponse enableCategory(@PathVariable("id") Long categoryId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.CATEGORY_WRITE);
        return ApiResponse.success(adminTaxonomyService.changeCategoryStatus(
                userId,
                categoryId,
                1,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 禁用分类：不会改历史内容，只影响后续可选项。
    @PostMapping({"/admin/categories/{id}/disable", "/api/admin/categories/{id}/disable"})
    public ApiResponse disableCategory(@PathVariable("id") Long categoryId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.CATEGORY_WRITE);
        return ApiResponse.success(adminTaxonomyService.changeCategoryStatus(
                userId,
                categoryId,
                0,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    // 标签列表：支持关键字与状态筛选。
    @GetMapping({"/admin/tags", "/api/admin/tags"})
    public ApiResponse getTags(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "20") Integer pageSize,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer status) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TAG_READ);
        return ApiResponse.success(adminTaxonomyService.getTags(page, pageSize, keyword, status));
    }

    @PostMapping({"/admin/tags", "/api/admin/tags"})
    public ApiResponse createTag(@Valid @RequestBody AdminTagRequest request, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TAG_WRITE);
        return ApiResponse.success(adminTaxonomyService.createTag(
                userId,
                request.getName(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PutMapping({"/admin/tags/{id}", "/api/admin/tags/{id}"})
    public ApiResponse updateTag(@PathVariable("id") Long tagId,
                                 @Valid @RequestBody AdminTagRequest request,
                                 HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TAG_WRITE);
        return ApiResponse.success(adminTaxonomyService.updateTag(
                userId,
                tagId,
                request.getName(),
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping({"/admin/tags/{id}/enable", "/api/admin/tags/{id}/enable"})
    public ApiResponse enableTag(@PathVariable("id") Long tagId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TAG_WRITE);
        return ApiResponse.success(adminTaxonomyService.changeTagStatus(
                userId,
                tagId,
                1,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping({"/admin/tags/{id}/disable", "/api/admin/tags/{id}/disable"})
    public ApiResponse disableTag(@PathVariable("id") Long tagId, HttpServletRequest servletRequest) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TAG_WRITE);
        return ApiResponse.success(adminTaxonomyService.changeTagStatus(
                userId,
                tagId,
                0,
                RequestMetaUtil.resolveClientIp(servletRequest),
                RequestMetaUtil.resolveUserAgent(servletRequest)
        ));
    }

    @GetMapping({"/admin/tags/{id}/contents", "/api/admin/tags/{id}/contents"})
    public ApiResponse getTagContents(@PathVariable("id") Long tagId,
                                      @RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.TAG_READ);
        return ApiResponse.success(adminTaxonomyService.getTagContents(tagId, page, pageSize));
    }
}
