package com.share.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 每日AI快讯服务：
 * - 负责读取今日快讯与历史快讯；
 * - 负责触发 Python 抓取并把结果落库；
 * - 对外返回稳定的前端展示结构。
 */
public interface DailyAiBriefService {

    /**
     * 获取今日快讯。
     * 若今日尚未生成，则回退到最近一条历史快讯并标记 isFallback=true。
     */
    Map<String, Object> getTodayBrief();

    /**
     * 获取近 N 天历史快讯摘要。
     */
    List<Map<String, Object>> getHistoryBriefs(Integer days);

    /**
     * 触发某一天快讯刷新。
     *
     * @param targetDate 目标日期，为空时使用今天
     * @param operatorUserId 操作者ID（定时任务触发可为空）
     * @param ip 操作来源IP（定时任务触发可为空）
     * @param userAgent 操作来源UA（定时任务触发可为空）
     */
    Map<String, Object> refreshBrief(LocalDate targetDate, Long operatorUserId, String ip, String userAgent);
}
