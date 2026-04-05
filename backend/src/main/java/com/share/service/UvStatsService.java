package com.share.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * 站点 UV 统计服务：
 * - 使用 Redis HyperLogLog 做近似去重；
 * - 按天写入，按天查询；
 * - 按月通过按天键合并得到月 UV。
 */
public interface UvStatsService {

    /**
     * 记录一次站点访问。
     * 仅使用 viewerKey 作为 UV 去重标识，不再混用 userId/ip+ua。
     */
    void recordVisit(String viewerKey);

    long getDailyUv(LocalDate date);

    long getMonthlyUv(YearMonth month);

    Map<String, Long> listDailyUv(LocalDate fromDate, LocalDate toDate);

    Map<String, Long> listMonthlyUv(YearMonth fromMonth, YearMonth toMonth);
}
