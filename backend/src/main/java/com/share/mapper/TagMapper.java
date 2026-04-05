package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：标签字典数据访问，支撑标签运营与内容关联查询。
 */
public interface TagMapper extends BaseMapper<Tag> {
}