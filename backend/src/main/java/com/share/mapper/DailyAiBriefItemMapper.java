package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.DailyAiBriefItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日AI快讯条目 Mapper：负责热点条目明细的读写。
 */
@Mapper
public interface DailyAiBriefItemMapper extends BaseMapper<DailyAiBriefItem> {
}
