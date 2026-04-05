package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论数据访问接口
 * 定义评论相关的数据库操作方法
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    
    /**
     * 根据内容ID查询顶级评论（分页）
     * @param contentId 内容ID
     * @param offset 偏移量
     * @param pageSize 每页数量
     * @return 评论列表
     */
    @Select("""
            SELECT c.*,
                   u.username AS username,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS nickname,
                   u.avatar AS avatar
            FROM comments c
            LEFT JOIN users u ON c.user_id = u.id
            WHERE c.content_id = #{contentId}
              AND c.parent_id IS NULL
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
            ORDER BY c.create_time DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Comment> findByContentId(@Param("contentId") Long contentId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    /**
     * 根据父评论ID查询回复列表
     * @param parentId 父评论ID
     * @return 回复列表
     */
    @Select("""
            SELECT c.*,
                   u.username AS username,
                   COALESCE(NULLIF(u.nickname, ''), u.username) AS nickname,
                   u.avatar AS avatar
            FROM comments c
            LEFT JOIN users u ON c.user_id = u.id
            WHERE c.parent_id = #{parentId}
              AND c.status = 1
              AND (c.review_status = 'approved' OR c.review_status IS NULL)
            ORDER BY c.create_time ASC
            """)
    List<Comment> findByParentId(@Param("parentId") Long parentId);

    /**
     * 统计内容的顶级评论数量
     * @param contentId 内容ID
     * @return 评论数量
     */
    @Select("SELECT COUNT(*) FROM comments WHERE content_id = #{contentId} AND parent_id IS NULL AND status = 1 AND (review_status = 'approved' OR review_status IS NULL)")
    Integer countByContentId(@Param("contentId") Long contentId);

    /**
     * 统计评论的回复数量
     * @param parentId 父评论ID
     * @return 回复数量
     */
    @Select("SELECT COUNT(*) FROM comments WHERE parent_id = #{parentId} AND status = 1 AND (review_status = 'approved' OR review_status IS NULL)")
    Integer countByParentId(@Param("parentId") Long parentId);
}
