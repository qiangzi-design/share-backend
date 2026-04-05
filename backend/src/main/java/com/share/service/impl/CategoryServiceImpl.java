package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.entity.Category;
import com.share.mapper.CategoryMapper;
import com.share.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 服务职责：分类字典读取。
 * 仅返回启用状态分类，并按运营配置的 sortOrder 升序展示。
 */
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> getActiveCategories() {
        // 前台发布/筛选只允许使用启用分类，禁用分类不对外暴露。
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder);
        return list(queryWrapper);
    }
}
