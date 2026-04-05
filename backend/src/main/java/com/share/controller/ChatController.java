package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.dto.ChatSendRequest;
import com.share.dto.PageResult;
import com.share.security.CurrentUserService;
import com.share.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping({"/chat", "/api/chat"})
/**
 * 控制器职责：私聊会话与消息接口入口。
 * 统一承接会话列表、消息拉取、未读数、发信、图片上传与已读回执。
 */
public class ChatController {

    private static final long MAX_CHAT_IMAGE_SIZE = 8 * 1024 * 1024L;
    private static final Set<String> CHAT_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> CHAT_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public ChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    /**
     * 获取当前用户的会话列表（按最近消息排序，含未读数）。
     */
    @GetMapping("/conversations")
    public ApiResponse getMyConversations(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        PageResult<Map<String, Object>> result = chatService.getConversations(userId, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 拉取与指定对象的私聊消息。
     */
    @GetMapping("/messages")
    public ApiResponse getMessages(@RequestParam Long targetUserId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        PageResult<Map<String, Object>> result = chatService.getMessages(userId, targetUserId, page, pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 获取当前用户私聊总未读数，供顶部角标展示。
     */
    @GetMapping("/unread-count")
    public ApiResponse getUnreadCount() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(Map.of("unreadCount", chatService.getUnreadCount(userId)));
    }

    /**
     * 获取当前会话发言额度（单向关注限制/互关不限）。
     */
    @GetMapping("/allowance")
    public ApiResponse getChatAllowance(@RequestParam Long targetUserId) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(chatService.getChatAllowance(userId, targetUserId));
    }

    /**
     * 发送文本或图片消息。
     * 关键规则：禁言用户不可发送私聊消息。
     */
    @PostMapping("/messages")
    public ApiResponse sendMessage(@Valid @RequestBody ChatSendRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        currentUserService.requireNotMuted("send private messages");
        return ApiResponse.success(chatService.sendMessage(
                userId,
                request.getTargetUserId(),
                request.getContent(),
                request.getMessageType()
        ));
    }

    /**
     * 上传私聊图片资源。
     * 安全边界：校验大小、MIME、后缀并做路径穿越防护。
     */
    @PostMapping("/upload-image")
    public ApiResponse uploadChatImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "Please choose an image");
        }
        // 限制单图体积，避免私聊通道被大文件占满。
        if (file.getSize() > MAX_CHAT_IMAGE_SIZE) {
            return ApiResponse.error(400, "Image exceeds size limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ApiResponse.error(400, "Invalid image format");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        // 双重校验：后缀 + Content-Type，降低伪装文件上传风险。
        if (contentType == null || !CHAT_IMAGE_EXTENSIONS.contains(extension) || !CHAT_IMAGE_CONTENT_TYPES.contains(contentType)) {
            return ApiResponse.error(400, "Unsupported image format");
        }

        try {
            Path uploadRoot = Paths.get("uploads", "chat").toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            String newFilename = UUID.randomUUID() + extension;
            Path target = uploadRoot.resolve(newFilename).normalize();
            // 路径归一化后再次确认目标目录，防止目录穿越。
            if (!target.startsWith(uploadRoot)) {
                return ApiResponse.error(400, "Invalid file path");
            }

            file.transferTo(target);
            return ApiResponse.success("/api/uploads/chat/" + newFilename);
        } catch (IOException ex) {
            return ApiResponse.error(500, "Image upload failed");
        }
    }

    /**
     * 会话已读回执：将当前用户在该会话收到的消息批量标记为已读。
     */
    @PostMapping("/read")
    public ApiResponse markRead(@RequestParam Long targetUserId) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(chatService.markConversationRead(userId, targetUserId));
    }
}
