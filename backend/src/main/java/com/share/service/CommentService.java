package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.entity.Comment;

import java.util.List;

/**
 * 评论服务接口
 * 定义评论相关的业务逻辑方法
 */
public interface CommentService extends IService<Comment> {
    
    /**
     * 创建评论
     * @param comment 评论信息
     * @return true: 创建成功，false: 创建失败
     */
    boolean createComment(Comment comment);
    
    /**
     * 根据内容ID获取评论列表（分页）
     * @param contentId 内容ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 评论列表
     */
    List<Comment> getCommentsByContentId(Long contentId, Integer page, Integer pageSize);
    
    /**
     * 获取评论的回复列表
     * @param commentId 评论ID
     * @return 回复列表
     */
    List<Comment> getRepliesByCommentId(Long commentId);
    
    /**
     * 获取内容的评论总数
     * @param contentId 内容ID
     * @return 评论总数
     */
    Integer getCommentCountByContentId(Long contentId);
    
    /**
     * 删除评论（逻辑删除）
     * @param commentId 评论ID
     * @return true: 删除成功，false: 删除失败
     */
    boolean deleteComment(Long commentId);
}
