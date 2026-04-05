package com.share.service.impl;

import com.share.mapper.AdminAnalyticsMapper;
import com.share.service.AdminAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * 管理端运营分析服务：
 * - 汇总内容质量、用户增长、审核效率三类指标。
 * - 统一处理粒度、时间窗、缺失点补齐和分母为 0 的比率兜底。
 */
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsServiceImpl.class);

    private final AdminAnalyticsMapper adminAnalyticsMapper;

    public AdminAnalyticsServiceImpl(AdminAnalyticsMapper adminAnalyticsMapper) {
        this.adminAnalyticsMapper = adminAnalyticsMapper;
    }

    @Override
    // 内容质量口径：浏览/点赞/收藏/评论按时间维度聚合，并补齐空白时间点。
    public Map<String, Object> getContentQuality(String granularity, Integer days) {
        String validGranularity = normalizeGranularity(granularity);
        int periodCount = normalizeDays(days, validGranularity);
        try {
            LocalDateTime fromTime = resolveFromTime(validGranularity, periodCount);

            List<Map<String, Object>> rows = "month".equals(validGranularity)
                    ? safeListQuery(() -> adminAnalyticsMapper.listContentQualityMonth(fromTime), "content quality month")
                    : safeListQuery(() -> adminAnalyticsMapper.listContentQualityDay(fromTime), "content quality day");

            Map<String, Map<String, Object>> rowMap = rowsToMap(rows, item -> fieldString(item, "label"));
            List<String> labels = buildLabels(validGranularity, periodCount);

            List<Map<String, Object>> points = new ArrayList<>();
            long totalViews = 0L;
            long totalLikes = 0L;
            long totalCollections = 0L;
            long totalComments = 0L;

            for (String label : labels) {
                Map<String, Object> raw = rowMap.getOrDefault(label, Map.of());
                long viewCount = fieldLong(raw, "viewCount");
                long likeCount = fieldLong(raw, "likeCount");
                long collectionCount = fieldLong(raw, "collectionCount");
                long commentCount = fieldLong(raw, "commentCount");
                long contentCount = fieldLong(raw, "contentCount");

                Map<String, Object> point = new LinkedHashMap<>();
                point.put("label", label);
                point.put("viewCount", viewCount);
                point.put("likeCount", likeCount);
                point.put("collectionCount", collectionCount);
                point.put("commentCount", commentCount);
                point.put("contentCount", contentCount);
                point.put("engagementRate", safeRate(likeCount + collectionCount + commentCount, viewCount));
                points.add(point);

                totalViews += viewCount;
                totalLikes += likeCount;
                totalCollections += collectionCount;
                totalComments += commentCount;
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalViews", totalViews);
            summary.put("totalLikes", totalLikes);
            summary.put("totalCollections", totalCollections);
            summary.put("totalComments", totalComments);
            summary.put("overallEngagementRate", safeRate(totalLikes + totalCollections + totalComments, totalViews));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("granularity", validGranularity);
            result.put("days", periodCount);
            result.put("summary", summary);
            result.put("points", points);
            return result;
        } catch (Exception ex) {
            log.error("Build content quality analytics failed. granularity={}, days={}", validGranularity, periodCount, ex);
            return emptyAnalyticsResult(validGranularity, periodCount);
        }
    }

    @Override
    // 用户增长口径：新增用户与活跃用户双序列，平均活跃按时间点均值计算。
    public Map<String, Object> getUserGrowth(String granularity, Integer days) {
        String validGranularity = normalizeGranularity(granularity);
        int periodCount = normalizeDays(days, validGranularity);
        try {
            LocalDateTime fromTime = resolveFromTime(validGranularity, periodCount);

            List<Map<String, Object>> newUserRows = "month".equals(validGranularity)
                    ? safeListQuery(() -> adminAnalyticsMapper.listNewUsersMonth(fromTime), "new users month")
                    : safeListQuery(() -> adminAnalyticsMapper.listNewUsersDay(fromTime), "new users day");
            List<Map<String, Object>> activeUserRows = "month".equals(validGranularity)
                    ? safeListQuery(() -> adminAnalyticsMapper.listActiveUsersMonth(fromTime), "active users month")
                    : safeListQuery(() -> adminAnalyticsMapper.listActiveUsersDay(fromTime), "active users day");

            Map<String, Long> newUserMap = rowsToMap(newUserRows, item -> fieldString(item, "label")).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, item -> fieldLong(item.getValue(), "newUserCount"), (a, b) -> a));
            Map<String, Long> activeUserMap = rowsToMap(activeUserRows, item -> fieldString(item, "label")).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, item -> fieldLong(item.getValue(), "activeUserCount"), (a, b) -> a));

            List<String> labels = buildLabels(validGranularity, periodCount);
            List<Map<String, Object>> points = new ArrayList<>();

            long totalNewUsers = 0L;
            long avgActiveUsers = 0L;

            for (String label : labels) {
                long newUsers = newUserMap.getOrDefault(label, 0L);
                long activeUsers = activeUserMap.getOrDefault(label, 0L);

                Map<String, Object> point = new LinkedHashMap<>();
                point.put("label", label);
                point.put("newUserCount", newUsers);
                point.put("activeUserCount", activeUsers);
                points.add(point);

                totalNewUsers += newUsers;
                avgActiveUsers += activeUsers;
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalNewUsers", totalNewUsers);
            summary.put("avgActiveUsers", labels.isEmpty() ? 0D : round((double) avgActiveUsers / labels.size()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("granularity", validGranularity);
            result.put("days", periodCount);
            result.put("summary", summary);
            result.put("points", points);
            return result;
        } catch (Exception ex) {
            log.error("Build user growth analytics failed. granularity={}, days={}", validGranularity, periodCount, ex);
            return emptyAnalyticsResult(validGranularity, periodCount);
        }
    }

    @Override
    // 审核效率口径：工单处理/解决 + 内容/评论通过率，强调治理效率而非流量指标。
    public Map<String, Object> getModerationEfficiency(String granularity, Integer days) {
        String validGranularity = normalizeGranularity(granularity);
        int periodCount = normalizeDays(days, validGranularity);
        try {
            LocalDateTime fromTime = resolveFromTime(validGranularity, periodCount);

            List<Map<String, Object>> rows = "month".equals(validGranularity)
                    ? safeListQuery(() -> adminAnalyticsMapper.listReportEfficiencyMonth(fromTime), "moderation efficiency month")
                    : safeListQuery(() -> adminAnalyticsMapper.listReportEfficiencyDay(fromTime), "moderation efficiency day");
            Map<String, Map<String, Object>> rowMap = rowsToMap(rows, item -> fieldString(item, "label"));
            List<String> labels = buildLabels(validGranularity, periodCount);

            List<Map<String, Object>> points = new ArrayList<>();
            long totalProcessed = 0L;
            long totalResolved = 0L;
            double weightedMinutes = 0D;

            for (String label : labels) {
                Map<String, Object> row = rowMap.getOrDefault(label, Map.of());
                long processed = fieldLong(row, "processedCount");
                long resolved = fieldLong(row, "resolvedCount");
                double avgMinutes = fieldDouble(row, "avgHandleMinutes");

                Map<String, Object> point = new LinkedHashMap<>();
                point.put("label", label);
                point.put("processedCount", processed);
                point.put("resolvedCount", resolved);
                point.put("resolveRate", safeRate(resolved, processed));
                point.put("avgHandleMinutes", round(avgMinutes));
                points.add(point);

                totalProcessed += processed;
                totalResolved += resolved;
                weightedMinutes += avgMinutes * processed;
            }

            Map<String, Object> contentReview = safeMap(safeMapQuery(() -> adminAnalyticsMapper.getContentReviewSummary(fromTime), "content review summary"));
            Map<String, Object> commentReview = safeMap(safeMapQuery(() -> adminAnalyticsMapper.getCommentReviewSummary(fromTime), "comment review summary"));

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("processedCount", totalProcessed);
            summary.put("resolvedCount", totalResolved);
            summary.put("resolveRate", safeRate(totalResolved, totalProcessed));
            summary.put("avgHandleMinutes", totalProcessed == 0 ? 0D : round(weightedMinutes / totalProcessed));
            summary.put("contentApproveRate", safeRate(fieldLong(contentReview, "approvedCount"), fieldLong(contentReview, "totalCount")));
            summary.put("commentApproveRate", safeRate(fieldLong(commentReview, "approvedCount"), fieldLong(commentReview, "totalCount")));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("granularity", validGranularity);
            result.put("days", periodCount);
            result.put("summary", summary);
            result.put("points", points);
            return result;
        } catch (Exception ex) {
            log.error("Build moderation efficiency analytics failed. granularity={}, days={}", validGranularity, periodCount, ex);
            return emptyAnalyticsResult(validGranularity, periodCount);
        }
    }

    // 把查询结果按 label 转 map，便于后续补齐缺失日期。
    private Map<String, Map<String, Object>> rowsToMap(List<Map<String, Object>> rows, Function<Map<String, Object>, String> keyFn) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = keyFn.apply(row);
            if (key == null || key.isBlank()) {
                continue;
            }
            result.putIfAbsent(key, row);
        }
        return result;
    }

    // 查询失败时返回结构化空结果，避免前端 500 或空指针。
    private Map<String, Object> emptyAnalyticsResult(String granularity, int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("granularity", granularity);
        result.put("days", days);
        result.put("summary", Map.of());
        result.put("points", List.of());
        return result;
    }

    // 生成连续标签序列（按天/按月），保证图表 X 轴稳定。
    private List<String> buildLabels(String granularity, int periodCount) {
        List<String> labels = new ArrayList<>();
        if ("month".equals(granularity)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            YearMonth start = YearMonth.now().minusMonths(periodCount - 1L);
            for (int i = 0; i < periodCount; i++) {
                labels.add(start.plusMonths(i).format(formatter));
            }
            return labels;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.now().minusDays(periodCount - 1L);
        for (int i = 0; i < periodCount; i++) {
            labels.add(start.plusDays(i).format(formatter));
        }
        return labels;
    }

    private String normalizeGranularity(String granularity) {
        return "month".equalsIgnoreCase(granularity) ? "month" : "day";
    }

    private int normalizeDays(Integer days, String granularity) {
        int fallback = "month".equals(granularity) ? 12 : 14;
        int value = (days == null || days < 1) ? fallback : days;
        int max = "month".equals(granularity) ? 36 : 90;
        return Math.min(value, max);
    }

    private LocalDateTime resolveFromTime(String granularity, int periodCount) {
        if ("month".equals(granularity)) {
            YearMonth start = YearMonth.now().minusMonths(periodCount - 1L);
            return start.atDay(1).atStartOfDay();
        }
        LocalDate start = LocalDate.now().minusDays(periodCount - 1L);
        return start.atStartOfDay();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Object fieldValue(Map<String, Object> row, String key) {
        if (row == null || row.isEmpty() || key == null || key.isBlank()) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String entryKey = entry.getKey();
            if (entryKey != null && entryKey.equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String fieldString(Map<String, Object> row, String key) {
        return stringValue(fieldValue(row, key));
    }

    private long fieldLong(Map<String, Object> row, String key) {
        return longValue(fieldValue(row, key));
    }

    private double fieldDouble(Map<String, Object> row, String key) {
        return doubleValue(fieldValue(row, key));
    }

    private long longValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private double doubleValue(Object value) {
        if (value == null) return 0D;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0D;
        }
    }

    private double safeRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return round((double) numerator / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10000D) / 10000D;
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? new HashMap<>() : map;
    }

    private List<Map<String, Object>> safeListQuery(AnalyticsListSupplier supplier, String scene) {
        try {
            List<Map<String, Object>> result = supplier.get();
            return result == null ? List.of() : result;
        } catch (Exception ex) {
            log.warn("Analytics query failed [{}], fallback to empty result. message={}", scene, ex.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> safeMapQuery(AnalyticsMapSupplier supplier, String scene) {
        try {
            Map<String, Object> result = supplier.get();
            return result == null ? Map.of() : result;
        } catch (Exception ex) {
            log.warn("Analytics query failed [{}], fallback to empty result. message={}", scene, ex.getMessage());
            return Map.of();
        }
    }

    @FunctionalInterface
    private interface AnalyticsListSupplier {
        List<Map<String, Object>> get();
    }

    @FunctionalInterface
    private interface AnalyticsMapSupplier {
        Map<String, Object> get();
    }
}
