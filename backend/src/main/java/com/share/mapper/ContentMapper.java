package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Content;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容数据访问接口
 * 定义内容相关的数据库操作方法
 */
@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    @Update("UPDATE contents SET view_count = COALESCE(view_count, 0) + 1 WHERE id = #{contentId} AND status = 1")
    int incrementViewCount(@Param("contentId") Long contentId);

    @Select("SELECT view_count FROM contents WHERE id = #{contentId} LIMIT 1")
    Integer getViewCountById(@Param("contentId") Long contentId);
}
