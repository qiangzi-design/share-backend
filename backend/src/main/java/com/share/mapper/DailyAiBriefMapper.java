package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.DailyAiBrief;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日AI快讯主表 Mapper：负责快讯主记录的增删改查。
 */
@Mapper
public interface DailyAiBriefMapper extends BaseMapper<DailyAiBrief> {
}
