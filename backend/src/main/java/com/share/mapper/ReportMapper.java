package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.Report;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：举报工单数据访问，支撑举报闭环与处理流转。
 */
public interface ReportMapper extends BaseMapper<Report> {
}

