package com.share.service.impl;

import com.share.mapper.AdminStatsMapper;
import com.share.service.AdminDashboardService;
import com.share.service.UvStatsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AdminStatsMapper adminStatsMapper;
    private final UvStatsService uvStatsService;

    public AdminDashboardServiceImpl(AdminStatsMapper adminStatsMapper, UvStatsService uvStatsService) {
        this.adminStatsMapper = adminStatsMapper;
        this.uvStatsService = uvStatsService;
    }

    @Override
    public Map<String, Object> getOverview() {
        long userCount = safeCount(adminStatsMapper.countUsers());
        long activeUserCount = safeCount(adminStatsMapper.countActiveUsers());
        long contentCount = safeCount(adminStatsMapper.countApprovedContents());
        long commentCount = safeCount(adminStatsMapper.countApprovedComments());
        long chatMessageCount = safeCount(adminStatsMapper.countChatMessages());
        long reportCount = safeCount(adminStatsMapper.countReports());
        long pendingReportCount = safeCount(adminStatsMapper.countPendingReports());
        long dailyUv = uvStatsService.getDailyUv(LocalDate.now());
        long monthlyUv = uvStatsService.getMonthlyUv(YearMonth.now());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", userCount);
        result.put("activeUserCount", activeUserCount);
        result.put("contentCount", contentCount);
        result.put("commentCount", commentCount);
        result.put("chatMessageCount", chatMessageCount);
        result.put("reportCount", reportCount);
        result.put("pendingReportCount", pendingReportCount);
        result.put("dailyUv", dailyUv);
        result.put("monthlyUv", monthlyUv);
        return result;
    }

    @Override
    public Map<String, Object> getTrends(String granularity, Integer days) {
        String validGranularity = normalizeGranularity(granularity);
        int periodCount = normalizeDays(days, validGranularity);
        LocalDateTime fromTime = resolveFromTime(validGranularity, periodCount);

        List<String> labels = buildLabels(validGranularity, periodCount);
        Map<String, Integer> userMap = groupByPeriod(adminStatsMapper.listUserCreateTimes(fromTime), validGranularity);
        Map<String, Integer> contentMap = groupByPeriod(adminStatsMapper.listContentCreateTimes(fromTime), validGranularity);
        Map<String, Integer> commentMap = groupByPeriod(adminStatsMapper.listCommentCreateTimes(fromTime), validGranularity);
        Map<String, Integer> reportMap = groupByPeriod(adminStatsMapper.listReportCreateTimes(fromTime), validGranularity);
        Map<String, Long> uvMap = buildUvMap(validGranularity, periodCount);

        List<Map<String, Object>> points = new ArrayList<>();
        for (String label : labels) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", label);
            point.put("userCount", userMap.getOrDefault(label, 0));
            point.put("contentCount", contentMap.getOrDefault(label, 0));
            point.put("commentCount", commentMap.getOrDefault(label, 0));
            point.put("reportCount", reportMap.getOrDefault(label, 0));
            point.put("uvCount", uvMap.getOrDefault(label, 0L));
            points.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("granularity", validGranularity);
        result.put("days", periodCount);
        result.put("points", points);
        return result;
    }

    private Map<String, Long> buildUvMap(String granularity, int periodCount) {
        if ("month".equals(granularity)) {
            YearMonth end = YearMonth.now();
            YearMonth start = end.minusMonths(periodCount - 1L);
            return uvStatsService.listMonthlyUv(start, end);
        }

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(periodCount - 1L);
        Map<String, Long> raw = uvStatsService.listDailyUv(start, end);
        Map<String, Long> byLabel = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : raw.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey(), DateTimeFormatter.ISO_LOCAL_DATE);
            byLabel.put(date.format(DAY_LABEL_FORMATTER), entry.getValue());
        }
        return byLabel;
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private String normalizeGranularity(String granularity) {
        if ("month".equalsIgnoreCase(granularity)) {
            return "month";
        }
        return "day";
    }

    private int normalizeDays(Integer days, String granularity) {
        int fallback = "month".equals(granularity) ? 12 : 14;
        int value = (days == null || days < 1) ? fallback : days;
        int max = "month".equals(granularity) ? 36 : 90;
        return Math.min(value, max);
    }

    private LocalDateTime resolveFromTime(String granularity, int periodCount) {
        if ("month".equals(granularity)) {
            YearMonth current = YearMonth.now();
            YearMonth start = current.minusMonths(periodCount - 1L);
            return start.atDay(1).atStartOfDay();
        }

        LocalDate start = LocalDate.now().minusDays(periodCount - 1L);
        return start.atStartOfDay();
    }

    private List<String> buildLabels(String granularity, int periodCount) {
        List<String> labels = new ArrayList<>();
        if ("month".equals(granularity)) {
            YearMonth start = YearMonth.now().minusMonths(periodCount - 1L);
            for (int i = 0; i < periodCount; i++) {
                labels.add(start.plusMonths(i).format(MONTH_LABEL_FORMATTER));
            }
            return labels;
        }

        LocalDate start = LocalDate.now().minusDays(periodCount - 1L);
        for (int i = 0; i < periodCount; i++) {
            labels.add(start.plusDays(i).format(DAY_LABEL_FORMATTER));
        }
        return labels;
    }

    private Map<String, Integer> groupByPeriod(List<LocalDateTime> times, String granularity) {
        Map<String, Integer> grouped = new LinkedHashMap<>();
        if (times == null || times.isEmpty()) {
            return grouped;
        }

        for (LocalDateTime time : times) {
            if (time == null) {
                continue;
            }
            String label = "month".equals(granularity)
                    ? time.format(MONTH_LABEL_FORMATTER)
                    : time.toLocalDate().format(DAY_LABEL_FORMATTER);
            grouped.put(label, grouped.getOrDefault(label, 0) + 1);
        }
        return grouped;
    }
}

