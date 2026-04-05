package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.entity.Comment;
import com.share.security.CurrentUserService;
import com.share.service.CommentLikeService;
import com.share.service.CommentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/comment", "/api/comment"})
/**
 * 控制器职责：评论与评论点赞入口。
 * 处理评论创建、分页列表、回复读取、删除与评论点赞相关接口。
 */
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final CurrentUserService currentUserService;

    public CommentController(CommentService commentService,
                             CommentLikeService commentLikeService,
                             CurrentUserService currentUserService) {
        this.commentService = commentService;
        this.commentLikeService = commentLikeService;
        this.currentUserService = currentUserService;
    }

    /**
     * 创建评论或回复。
     * 关键规则：禁言用户不可评论；评论作者由服务端从当前登录态注入，避免前端伪造。
     */
    @PostMapping("/create")
    public ApiResponse createComment(@RequestBody Comment comment) {
        Long userId = currentUserService.requireCurrentUserId();
        currentUserService.requireNotMuted("post comment");
        comment.setUserId(userId);

        boolean success = commentService.createComment(comment);
        if (success) {
            return ApiResponse.success("Comment created");
        }
        return ApiResponse.error(500, "Failed to create comment");
    }

    /**
     * 分页获取内容评论（含总数），供详情页主评论区使用。
     */
    @GetMapping("/list")
    public ApiResponse getComments(@RequestParam Long contentId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        List<Comment> comments = commentService.getCommentsByContentId(contentId, page, pageSize);
        Integer total = commentService.getCommentCountByContentId(contentId);

        Map<String, Object> result = new HashMap<>();
        result.put("comments", comments);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ApiResponse.success(result);
    }

    /**
     * 获取某条评论的回复列表。
     */
    @GetMapping("/replies")
    public ApiResponse getReplies(@RequestParam Long commentId) {
        return ApiResponse.success(commentService.getRepliesByCommentId(commentId));
    }

    /**
     * 删除评论（逻辑删除）。
     * 权限边界：仅评论作者本人允许删除。
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse deleteComment(@PathVariable Long id) {
        Long userId = currentUserService.requireCurrentUserId();

        Comment comment = commentService.getById(id);
        if (comment == null) {
            return ApiResponse.error(404, "Comment not found");
        }
        // 防止越权删除他人评论。
        if (!comment.getUserId().equals(userId)) {
            return ApiResponse.error(403, "No permission to delete this comment");
        }

        boolean success = commentService.deleteComment(id);
        if (success) {
            return ApiResponse.success("Deleted");
        }
        return ApiResponse.error(500, "Delete failed");
    }

    /**
     * 评论点赞开关：已点赞则取消，未点赞则新增。
     */
    @PostMapping("/like/toggle")
    public ApiResponse toggleLike(@RequestParam Long commentId) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean liked = commentLikeService.toggleLike(userId, commentId);
        Integer likeCount = commentLikeService.getLikeCount(commentId);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", liked);
        result.put("likeCount", likeCount);
        return ApiResponse.success(result);
    }

    /**
     * 获取评论点赞状态与计数，便于列表渲染。
     */
    @GetMapping("/like/status")
    public ApiResponse getLikeStatus(@RequestParam Long commentId) {
        Long userId = currentUserService.requireCurrentUserId();
        boolean liked = commentLikeService.isLiked(userId, commentId);
        Integer likeCount = commentLikeService.getLikeCount(commentId);

        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", liked);
        result.put("likeCount", likeCount);
        return ApiResponse.success(result);
    }

    /**
     * 获取评论点赞数（公开读）。
     */
    @GetMapping("/like/count")
    public ApiResponse getLikeCount(@RequestParam Long commentId) {
        return ApiResponse.success(commentLikeService.getLikeCount(commentId));
    }
}
