package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：分类字典数据访问，支撑发布与筛选分类能力。
 */
public interface CategoryMapper extends BaseMapper<Category> {
}