package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> getActiveCategories();
}