package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.dto.PageResult;
import com.share.dto.UserProfileUpdateRequest;
import com.share.entity.Content;
import com.share.entity.User;
import com.share.security.CurrentUserService;
import com.share.security.UserStatusCodes;
import com.share.service.ContentCollectionService;
import com.share.service.ContentService;
import com.share.service.LikeService;
import com.share.service.UserFollowService;
import com.share.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping({"/users", "/api/users"})
/**
 * 控制器职责：用户中心与公开主页相关接口。
 * 覆盖我的资料、头像上传、我的点赞/收藏列表、用户公开资料与作品列表。
 */
public class UserController {

    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final UserService userService;
    private final ContentService contentService;
    private final ContentCollectionService contentCollectionService;
    private final LikeService likeService;
    private final UserFollowService userFollowService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService,
                          ContentService contentService,
                          ContentCollectionService contentCollectionService,
                          LikeService likeService,
                          UserFollowService userFollowService,
                          CurrentUserService currentUserService) {
        this.userService = userService;
        this.contentService = contentService;
        this.contentCollectionService = contentCollectionService;
        this.likeService = likeService;
        this.userFollowService = userFollowService;
        this.currentUserService = currentUserService;
    }

    /**
     * 获取当前登录用户资料摘要。
     */
    @GetMapping("/me")
    public ApiResponse getMyProfile() {
        Long userId = currentUserService.requireCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("bio", user.getBio());
        profile.put("followerCount", userFollowService.getFollowerCount(userId));
        profile.put("followingCount", userFollowService.getFollowingCount(userId));
        return ApiResponse.success(profile);
    }

    /**
     * 获取我的收藏列表（分页）。
     */
    @GetMapping("/me/collections")
    public ApiResponse getMyCollections(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        PageResult<Map<String, Object>> result = contentCollectionService.getCollectedContents(userId, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 获取我的点赞列表（分页）。
     */
    @GetMapping("/me/likes")
    public ApiResponse getMyLikes(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        PageResult<Map<String, Object>> result = likeService.getLikedContents(userId, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 更新我的昵称与简介。
     */
    @PutMapping("/me")
    public ApiResponse updateMyProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean updated = userService.updateProfile(userId, request.getNickname(), request.getBio());
        if (!updated) {
            return ApiResponse.error(404, "用户不存在");
        }
        return ApiResponse.success("更新成功");
    }

    /**
     * 上传用户头像。
     * 关键规则：大小、后缀、MIME 三重校验，并做路径穿越防护。
     */
    @PostMapping("/me/avatar")
    public ApiResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = currentUserService.requireCurrentUserId();
        if (file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的文件");
        }
        // 控制头像体积，避免存储膨胀与慢加载。
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ApiResponse.error(400, "头像大小不能超过2MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ApiResponse.error(400, "文件格式不合法");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        String contentType = file.getContentType();
        // 双重校验确保上传文件真实为图片。
        if (!ALLOWED_EXTENSIONS.contains(extension) || contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ApiResponse.error(400, "仅支持 JPG、PNG、GIF、WEBP 格式图片");
        }

        try {
            Path uploadRoot = Paths.get("uploads", "avatars").toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            String newFilename = UUID.randomUUID() + extension;
            Path target = uploadRoot.resolve(newFilename).normalize();
            // 路径归一化后再次确认目标目录，防止目录穿越写入。
            if (!target.startsWith(uploadRoot)) {
                return ApiResponse.error(400, "非法文件路径");
            }

            file.transferTo(target);
            String avatarUrl = "/api/uploads/avatars/" + newFilename;
            boolean updated = userService.updateAvatar(userId, avatarUrl);
            if (!updated) {
                return ApiResponse.error(404, "用户不存在");
            }
            return ApiResponse.success(avatarUrl);
        } catch (IOException ex) {
            return ApiResponse.error(500, "头像上传失败");
        }
    }

    /**
     * 获取指定用户作品列表。
     * 口径：本人查看可看到自己的完整作品；他人查看仅返回可发布内容。
     */
    @GetMapping("/{id}/contents")
    public ApiResponse getUserContents(@PathVariable Long id,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(defaultValue = "latest") String sort) {
        Long viewerId = currentUserService.getCurrentUserIdOrNull();
        PageResult<Content> result = (viewerId != null && viewerId.equals(id))
                ? contentService.getContentsByOwner(id, page, pageSize, sort)
                : contentService.getPublishedContentsByUserId(id, page, pageSize, sort);
        return ApiResponse.success(result);
    }

    /**
     * 获取用户公开资料页信息。
     * 风控边界：被封禁用户不提供公开资料。
     */
    @GetMapping("/{id}/public")
    public ApiResponse getPublicProfile(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || UserStatusCodes.isBanned(user.getStatus())) {
            return ApiResponse.error(404, "用户不存在");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("bio", user.getBio());
        profile.put("followerCount", userFollowService.getFollowerCount(id));
        profile.put("followingCount", userFollowService.getFollowingCount(id));
        return ApiResponse.success(profile);
    }
}
