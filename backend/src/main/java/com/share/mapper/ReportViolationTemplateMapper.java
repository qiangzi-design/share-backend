package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.ReportViolationTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * Mapper职责：举报违规模板数据访问，支撑用户举报模板可配置化。
 */
public interface ReportViolationTemplateMapper extends BaseMapper<ReportViolationTemplate> {
}
