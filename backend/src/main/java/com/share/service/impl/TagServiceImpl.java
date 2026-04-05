package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.entity.Tag;
import com.share.mapper.TagMapper;
import com.share.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 服务职责：标签字典读取。
 * 仅返回启用标签，并按 use_count 倒序，便于前端优先展示高频标签。
 */
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Override
    public List<Tag> getActiveTags() {
        // 发布/编辑页可选标签口径：status=1 的启用标签。
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tag::getStatus, 1)
                .orderByDesc(Tag::getUseCount);
        return list(queryWrapper);
    }
}
