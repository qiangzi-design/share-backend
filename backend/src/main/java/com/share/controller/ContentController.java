package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.dto.ContentRequest;
import com.share.dto.ContentUpdateRequest;
import com.share.dto.PageResult;
import com.share.entity.Content;
import com.share.security.CurrentUserService;
import com.share.service.CategoryService;
import com.share.service.ContentService;
import com.share.service.TagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/content", "/api/content"})
/**
 * 内容域控制器。
 * 负责发布/编辑/删除、媒体上传、列表检索与浏览事件上报。
 * 说明：控制器只做参数编排与访问控制，核心业务规则在 ContentService 中统一实现。
 */
public class ContentController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final long MAX_VIDEO_SIZE = 80 * 1024 * 1024L;

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime"
    );
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".webm", ".mov"
    );

    private final ContentService contentService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CurrentUserService currentUserService;

    public ContentController(ContentService contentService,
                             CategoryService categoryService,
                             TagService tagService,
                             CurrentUserService currentUserService) {
        this.contentService = contentService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/publish")
    public ApiResponse publish(@Valid @RequestBody ContentRequest request) {
        // 发布要求登录且未被禁言，避免禁言用户绕过前端直接调用接口。
        Long userId = currentUserService.requireCurrentUserId();
        currentUserService.requireNotMuted("publish content");

        Content content = new Content();
        content.setTitle(request.getTitle());
        content.setContent(request.getContent());
        content.setUserId(userId);
        content.setCategoryId(request.getCategoryId());
        content.setTags(normalizeTags(request.getTags()));
        content.setImages(request.getImages());
        content.setVideos(request.getVideos());

        boolean success = contentService.publish(content);
        if (!success) {
            return ApiResponse.error(500, "Publish failed");
        }
        return ApiResponse.success("Published successfully, waiting for review");
    }

    @PutMapping("/{id}")
    public ApiResponse updateContent(@PathVariable Long id, @Valid @RequestBody ContentUpdateRequest request) {
        // 编辑后会在服务层重置为待审核状态，确保内容变更可再次审核。
        Long userId = currentUserService.requireCurrentUserId();
        request.setTags(normalizeTags(request.getTags()));
        boolean success = contentService.updateContent(id, userId, request);
        if (!success) {
            return ApiResponse.error(403, "No permission or content not found");
        }
        return ApiResponse.success("Updated successfully and pending review again");
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteContent(@PathVariable Long id) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean success = contentService.deleteContent(id, userId);
        if (!success) {
            return ApiResponse.error(403, "No permission or content not found");
        }
        return ApiResponse.success("Deleted successfully");
    }

    @PostMapping("/upload")
    public ApiResponse uploadImage(@RequestParam("file") MultipartFile file) {
        // 图片上传与视频上传复用统一校验逻辑，减少安全规则分叉。
        return uploadMedia(
                file,
                MAX_IMAGE_SIZE,
                IMAGE_CONTENT_TYPES,
                IMAGE_EXTENSIONS,
                Paths.get("uploads"),
                "/api/uploads/",
                "Image upload failed"
        );
    }

    @PostMapping("/upload/video")
    public ApiResponse uploadVideo(@RequestParam("file") MultipartFile file) {
        return uploadMedia(
                file,
                MAX_VIDEO_SIZE,
                VIDEO_CONTENT_TYPES,
                VIDEO_EXTENSIONS,
                Paths.get("uploads", "videos"),
                "/api/uploads/videos/",
                "Video upload failed"
        );
    }

    @GetMapping("/categories")
    public ApiResponse getCategories() {
        return ApiResponse.success(categoryService.getActiveCategories());
    }

    @GetMapping("/tags")
    public ApiResponse getTags() {
        return ApiResponse.success(tagService.getActiveTags());
    }

    @GetMapping("/list")
    public ApiResponse getContentList(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "latest") String sort) {
        // 列表与搜索共用同一服务方法，确保口径一致。
        PageResult<Content> result = contentService.getPublishedContents(page, pageSize, categoryId, keyword, sort);
        return ApiResponse.success(result);
    }

    @GetMapping("/search")
    public ApiResponse searchContent(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "latest") String sort) {
        PageResult<Content> result = contentService.getPublishedContents(page, pageSize, categoryId, keyword, sort);
        return ApiResponse.success(result);
    }

    @GetMapping("/detail/{id}")
    public ApiResponse getContentDetail(@PathVariable Long id) {
        Long viewerUserId = currentUserService.getCurrentUserIdOrNull();
        Content content = contentService.getContentDetailByViewer(id, viewerUserId);
        if (content == null) {
            return ApiResponse.error(404, "Content not found or not approved");
        }
        return ApiResponse.success(content);
    }

    @PostMapping("/view/{id}")
    public ApiResponse reportContentView(@PathVariable Long id,
                                         @RequestHeader(value = "X-Viewer-Id", required = false) String viewerId,
                                         HttpServletRequest request) {
        // 浏览事件同时支持登录用户与匿名访客（viewerKey），用于去重统计。
        Long viewerUserId = currentUserService.getCurrentUserIdOrNull();
        String viewerKey = resolveViewerKey(viewerId, request);
        String clientIp = resolveClientIp(request);
        String userAgent = normalizeUserAgent(request.getHeader("User-Agent"));
        return ApiResponse.success(contentService.recordContentView(id, viewerUserId, viewerKey, clientIp, userAgent));
    }

    private ApiResponse uploadMedia(MultipartFile file,
                                    long maxSize,
                                    Set<String> allowedContentTypes,
                                    Set<String> allowedExtensions,
                                    Path uploadRootPath,
                                    String publicPrefix,
                                    String failureMessage) {
        // 服务端双重校验：MIME + 扩展名，避免伪装文件绕过上传限制。
        if (file.isEmpty()) {
            return ApiResponse.error(400, "Please choose a file");
        }
        if (file.getSize() > maxSize) {
            return ApiResponse.error(400, "File exceeds size limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ApiResponse.error(400, "Invalid file format");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        if (contentType == null || !allowedExtensions.contains(extension) || !allowedContentTypes.contains(contentType)) {
            return ApiResponse.error(400, "Unsupported file type");
        }

        try {
            Path uploadRoot = uploadRootPath.toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            String newFilename = UUID.randomUUID() + extension;
            Path target = uploadRoot.resolve(newFilename).normalize();
            if (!target.startsWith(uploadRoot)) {
                return ApiResponse.error(400, "Invalid file path");
            }

            file.transferTo(target);
            return ApiResponse.success(publicPrefix + newFilename);
        } catch (IOException ex) {
            return ApiResponse.error(500, failureMessage);
        }
    }

    private String normalizeTags(String tags) {
        // 标签规则：去重、去空、单个长度限制、最多 5 个，且必须是启用标签。
        if (tags == null || tags.isBlank()) {
            return null;
        }

        String[] items = tags.split(",");
        LinkedHashSet<String> uniqueTags = new LinkedHashSet<>();
        for (String item : items) {
            String tag = item == null ? "" : item.trim();
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.length() > 15) {
                throw new IllegalArgumentException("Tag length cannot exceed 15 characters");
            }
            uniqueTags.add(tag);
        }

        if (uniqueTags.size() > 5) {
            throw new IllegalArgumentException("You can select up to 5 tags");
        }

        if (!uniqueTags.isEmpty()) {
            List<String> activeTagNames = tagService.getActiveTags().stream()
                    .map(tag -> tag.getName() == null ? "" : tag.getName().trim())
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toList());
            for (String tag : uniqueTags) {
                if (!activeTagNames.contains(tag)) {
                    throw new IllegalArgumentException("Contains disabled or missing tag: " + tag);
                }
            }
        }

        return uniqueTags.isEmpty() ? null : String.join(",", uniqueTags);
    }

    private String resolveViewerKey(String viewerId, HttpServletRequest request) {
        if (viewerId != null) {
            String cleaned = viewerId.trim().replaceAll("[^a-zA-Z0-9_-]", "");
            if (!cleaned.isEmpty()) {
                return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
            }
        }

        String ip = resolveClientIp(request).replace(":", "_").replace(".", "_");
        if (ip.length() > 48) {
            ip = ip.substring(0, 48);
        }
        return "anon_" + ip;
    }

    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String value = userAgent.trim();
        return value.length() > 255 ? value.substring(0, 255) : value;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }
        return remoteAddr.trim();
    }
}
