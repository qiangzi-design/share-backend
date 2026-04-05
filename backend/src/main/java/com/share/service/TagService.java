package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.entity.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {
    List<Tag> getActiveTags();
}