package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
/**
 * Mapper职责：评论点赞数据访问，维护评论点赞明细与计数。
 */
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
    @Select("SELECT * FROM comment_likes WHERE user_id = #{userId} AND comment_id = #{commentId}")
    CommentLike findByUserIdAndCommentId(Long userId, Long commentId);

    @Select("SELECT COUNT(*) FROM comment_likes WHERE comment_id = #{commentId}")
    Integer countByCommentId(Long commentId);
}
