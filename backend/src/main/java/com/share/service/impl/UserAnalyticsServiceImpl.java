package com.share.service.impl;

import com.share.mapper.UserAnalyticsMapper;
import com.share.service.UserAnalyticsService;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * 用户侧数据分析服务。
 * 说明：所有统计只看“当前登录用户本人数据”，并统一在服务层固化口径，避免前后端口径漂移。
 */
public class UserAnalyticsServiceImpl implements UserAnalyticsService {

    private static final String GRANULARITY_DAY = "day";
    private static final String GRANULARITY_WEEK = "week";
    private static final String GRANULARITY_MONTH = "month";

    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserAnalyticsMapper userAnalyticsMapper;

    public UserAnalyticsServiceImpl(UserAnalyticsMapper userAnalyticsMapper) {
        this.userAnalyticsMapper = userAnalyticsMapper;
    }

    @Override
    public Map<String, Object> getOverview(Long userId, String granularity, Integer days) {
        // 总览卡片基于趋势聚合后的总和，确保与趋势图口径一致。
        String validGranularity = normalizeGranularity(granularity);
        int periodCount = normalizePeriodCount(validGranularity, days);
        TrendBundle trendBundle = loadTrendBundle(userId, validGranularity, periodCount);

        long totalViews = sumMapValues(trendBundle.viewMap);
        long totalLikes = sumMapValues(trendBundle.likeMap);
        long totalCollections = sumMapValues(trendBundle.collectionMap);
        long totalComments = sumMapValues(trendBundle.commentMap);
        long totalNewFollowers = sumMapValues(trendBundle.followerMap);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalViews", totalViews);
        summary.put("totalLikes", totalLikes);
        summary.put("totalCollections", totalCollections);
        summary.put("totalComments", totalComments);
        summary.put("newFollowerCount", totalNewFollowers);
        summary.put("engagementRate", safeRate(totalLikes + totalCollections + totalComments, totalViews));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("granularity", validGranularity);
        result.put("days", periodCount);
        result.put("summary", summary);
        return result;
    }

    @Override
    public Map<String, Object> getTopContents(Long userId, Integer limit) {
        // Top 内容按全量累计浏览量排序，不受时间筛选影响（产品约定）。
        int validLimit = normalizeTopLimit(limit);
        List<Map<String, Object>> rows = safeRows(userAnalyticsMapper.listTopVisibleContents(userId, validLimit));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long viewCount = fieldLong(row, "viewCount");
            long likeCount = fieldLong(row, "likeCount");
            long collectionCount = fieldLong(row, "collectionCount");
            long commentCount = fieldLong(row, "commentCount");
            long totalEngagement = likeCount + collectionCount + commentCount;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("contentId", fieldLong(row, "contentId"));
            item.put("title", fieldString(row, "title"));
            item.put("categoryId", fieldLong(row, "categoryId"));
            item.put("categoryName", fieldString(row, "categoryName"));
            item.put("createTime", fieldValue(row, "createTime"));
            item.put("viewCount", viewCount);
            item.put("likeCount", likeCount);
            item.put("collectionCount", collectionCount);
            item.put("commentCount", commentCount);
            item.put("totalEngagement", totalEngagement);
            item.put("engagementRate", safeRate(totalEngagement, viewCount));
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("limit", validLimit);
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    @Override
    public Map<String, Object> getTrend(Long userId, String granularity, Integer days) {
        // 趋势返回固定时间轴点位，缺失日期补 0，避免图表断点。
        String validGranularity = normalizeGranularity(granularity);
        int periodCount = normalizePeriodCount(validGranularity, days);
        TrendBundle trendBundle = loadTrendBundle(userId, validGranularity, periodCount);

        long totalViews = sumMapValues(trendBundle.viewMap);
        long totalLikes = sumMapValues(trendBundle.likeMap);
        long totalCollections = sumMapValues(trendBundle.collectionMap);
        long totalComments = sumMapValues(trendBundle.commentMap);
        long totalNewFollowers = sumMapValues(trendBundle.followerMap);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalViews", totalViews);
        summary.put("totalLikes", totalLikes);
        summary.put("totalCollections", totalCollections);
        summary.put("totalComments", totalComments);
        summary.put("newFollowerCount", totalNewFollowers);
        summary.put("engagementRate", safeRate(totalLikes + totalCollections + totalComments, totalViews));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("granularity", validGranularity);
        result.put("days", periodCount);
        result.put("summary", summary);
        result.put("points", trendBundle.points);
        return result;
    }

    @Override
    public Map<String, Object> getTaxonomy(Long userId, Integer days) {
        // 标签/分类效果基于筛选周期内“本人可见作品集”计算。
        int validDays = normalizeRangeDays(days);
        LocalDateTime fromTime = LocalDate.now().minusDays(validDays - 1L).atStartOfDay();
        List<Map<String, Object>> rows = safeRows(userAnalyticsMapper.listVisibleContentsInRange(userId, fromTime));

        Map<String, CategoryAgg> categoryAggMap = new LinkedHashMap<>();
        Map<String, TagAgg> tagAggMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            long viewCount = fieldLong(row, "viewCount");
            long likeCount = fieldLong(row, "likeCount");
            long collectionCount = fieldLong(row, "collectionCount");
            long commentCount = fieldLong(row, "commentCount");
            long totalEngagement = likeCount + collectionCount + commentCount;

            String categoryName = normalizeCategory(fieldString(row, "categoryName"));
            CategoryAgg categoryAgg = categoryAggMap.computeIfAbsent(categoryName, CategoryAgg::new);
            categoryAgg.add(viewCount, likeCount, collectionCount, commentCount, totalEngagement);

            Set<String> uniqueTags = parseTags(fieldString(row, "tags"));
            for (String tag : uniqueTags) {
                TagAgg tagAgg = tagAggMap.computeIfAbsent(tag, TagAgg::new);
                tagAgg.add(viewCount, likeCount, collectionCount, commentCount, totalEngagement);
            }
        }

        List<Map<String, Object>> categoryList = categoryAggMap.values().stream()
                .sorted(Comparator.comparingLong(CategoryAgg::totalEngagement).reversed()
                        .thenComparingLong(CategoryAgg::contentCount).reversed()
                        .thenComparing(CategoryAgg::name))
                .map(CategoryAgg::toView)
                .toList();

        List<Map<String, Object>> tagList = tagAggMap.values().stream()
                .sorted(Comparator.comparingLong(TagAgg::totalEngagement).reversed()
                        .thenComparingLong(TagAgg::contentCount).reversed()
                        .thenComparing(TagAgg::name))
                .map(TagAgg::toView)
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("contentCount", rows.size());
        summary.put("categoryCount", categoryList.size());
        summary.put("tagCount", tagList.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", validDays);
        result.put("summary", summary);
        result.put("categories", categoryList);
        result.put("tags", tagList);
        return result;
    }

    @Override
    public Map<String, Object> getPublishTime(Long userId, Integer days) {
        // 发布时间段：按小时聚合互动率，并返回推荐时段 Top3。
        int validDays = normalizeRangeDays(days);
        LocalDateTime fromTime = LocalDate.now().minusDays(validDays - 1L).atStartOfDay();
        List<Map<String, Object>> rows = safeRows(userAnalyticsMapper.listVisibleContentsInRange(userId, fromTime));

        List<HourAgg> hourAggs = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourAggs.add(new HourAgg(hour));
        }

        for (Map<String, Object> row : rows) {
            LocalDateTime createTime = toLocalDateTime(fieldValue(row, "createTime"));
            if (createTime == null) {
                continue;
            }
            int hour = createTime.getHour();

            long viewCount = fieldLong(row, "viewCount");
            long likeCount = fieldLong(row, "likeCount");
            long collectionCount = fieldLong(row, "collectionCount");
            long commentCount = fieldLong(row, "commentCount");
            long totalEngagement = likeCount + collectionCount + commentCount;

            hourAggs.get(hour).add(viewCount, likeCount, collectionCount, commentCount, totalEngagement);
        }

        List<Map<String, Object>> points = hourAggs.stream()
                .map(HourAgg::toView)
                .toList();

        List<HourAgg> preferred = hourAggs.stream()
                // 推荐优先取内容样本不少于 2 条的小时，降低偶然性。
                .filter(item -> item.contentCount >= 2)
                .sorted(Comparator.comparingDouble(HourAgg::engagementRate).reversed()
                        .thenComparingLong(HourAgg::contentCount).reversed()
                        .thenComparingInt(item -> item.hour))
                .toList();

        List<HourAgg> recommended = new ArrayList<>();
        for (HourAgg item : preferred) {
            recommended.add(item);
            if (recommended.size() >= 3) {
                break;
            }
        }

        if (recommended.size() < 3) {
            Set<Integer> chosenHours = recommended.stream().map(item -> item.hour).collect(Collectors.toSet());
            List<HourAgg> backups = hourAggs.stream()
                    .filter(item -> !chosenHours.contains(item.hour))
                    .sorted(Comparator.comparingLong(HourAgg::contentCount).reversed()
                            .thenComparingDouble(HourAgg::engagementRate).reversed()
                            .thenComparingInt(item -> item.hour))
                    .toList();
            for (HourAgg backup : backups) {
                recommended.add(backup);
                if (recommended.size() >= 3) {
                    break;
                }
            }
        }

        List<Map<String, Object>> recommendedSlots = recommended.stream()
                .map(HourAgg::toRecommendView)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", validDays);
        result.put("points", points);
        result.put("recommendedSlots", recommendedSlots);
        return result;
    }

    @Override
    public Map<String, Object> getGovernance(Long userId) {
        // 治理提醒使用治理口径，不受“可见内容过滤”影响。
        long pendingContentCount = safeLong(userAnalyticsMapper.countPendingContents(userId));
        long offShelfContentCount = safeLong(userAnalyticsMapper.countOffShelfContents(userId));
        long totalReportCount = safeLong(userAnalyticsMapper.countReportedContentsAll(userId));
        long pendingReportCount = safeLong(userAnalyticsMapper.countReportedContentsPending(userId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingContentCount", pendingContentCount);
        result.put("offShelfContentCount", offShelfContentCount);
        result.put("reportTotalCount", totalReportCount);
        result.put("reportPendingCount", pendingReportCount);
        return result;
    }

    private TrendBundle loadTrendBundle(Long userId, String granularity, int periodCount) {
        // 趋势统一在服务层做五类指标装配，保证各接口复用同一时间维度逻辑。
        LocalDateTime fromTime = resolveFromTime(granularity, periodCount);

        Map<String, Long> viewMap = toCountMap(userAnalyticsMapper.listViewTrend(userId, fromTime, granularity));
        Map<String, Long> likeMap = toCountMap(userAnalyticsMapper.listLikeTrend(userId, fromTime, granularity));
        Map<String, Long> collectionMap = toCountMap(userAnalyticsMapper.listCollectionTrend(userId, fromTime, granularity));
        Map<String, Long> commentMap = toCountMap(userAnalyticsMapper.listCommentTrend(userId, fromTime, granularity));
        Map<String, Long> followerMap = toCountMap(userAnalyticsMapper.listFollowerTrend(userId, fromTime, granularity));

        List<String> labels = buildLabels(granularity, periodCount);
        List<Map<String, Object>> points = new ArrayList<>();

        for (String label : labels) {
            long viewCount = viewMap.getOrDefault(label, 0L);
            long likeCount = likeMap.getOrDefault(label, 0L);
            long collectionCount = collectionMap.getOrDefault(label, 0L);
            long commentCount = commentMap.getOrDefault(label, 0L);
            long followerCount = followerMap.getOrDefault(label, 0L);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", label);
            point.put("viewCount", viewCount);
            point.put("likeCount", likeCount);
            point.put("collectionCount", collectionCount);
            point.put("commentCount", commentCount);
            point.put("newFollowerCount", followerCount);
            point.put("engagementRate", safeRate(likeCount + collectionCount + commentCount, viewCount));
            points.add(point);
        }
        return new TrendBundle(viewMap, likeMap, collectionMap, commentMap, followerMap, points);
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            String label = fieldString(row, "label");
            if (label == null || label.isBlank()) {
                continue;
            }
            result.put(label, fieldLong(row, "total"));
        }
        return result;
    }

    private List<String> buildLabels(String granularity, int periodCount) {
        List<String> labels = new ArrayList<>(periodCount);
        if (GRANULARITY_MONTH.equals(granularity)) {
            YearMonth start = YearMonth.now().minusMonths(periodCount - 1L);
            for (int i = 0; i < periodCount; i++) {
                labels.add(start.plusMonths(i).format(MONTH_LABEL_FORMATTER));
            }
            return labels;
        }
        if (GRANULARITY_WEEK.equals(granularity)) {
            LocalDate weekStart = LocalDate.now()
                    .minusWeeks(periodCount - 1L)
                    .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            for (int i = 0; i < periodCount; i++) {
                labels.add(toWeekLabel(weekStart.plusWeeks(i)));
            }
            return labels;
        }

        LocalDate start = LocalDate.now().minusDays(periodCount - 1L);
        for (int i = 0; i < periodCount; i++) {
            labels.add(start.plusDays(i).format(DAY_LABEL_FORMATTER));
        }
        return labels;
    }

    private LocalDateTime resolveFromTime(String granularity, int periodCount) {
        if (GRANULARITY_MONTH.equals(granularity)) {
            YearMonth start = YearMonth.now().minusMonths(periodCount - 1L);
            return start.atDay(1).atStartOfDay();
        }
        if (GRANULARITY_WEEK.equals(granularity)) {
            LocalDate start = LocalDate.now()
                    .minusWeeks(periodCount - 1L)
                    .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            return start.atStartOfDay();
        }
        return LocalDate.now().minusDays(periodCount - 1L).atStartOfDay();
    }

    private String toWeekLabel(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        int week = date.get(weekFields.weekOfWeekBasedYear());
        int weekYear = date.get(weekFields.weekBasedYear());
        return String.format(Locale.ROOT, "%04d-W%02d", weekYear, week);
    }

    private String normalizeGranularity(String granularity) {
        if (GRANULARITY_WEEK.equalsIgnoreCase(granularity)) {
            return GRANULARITY_WEEK;
        }
        if (GRANULARITY_MONTH.equalsIgnoreCase(granularity)) {
            return GRANULARITY_MONTH;
        }
        return GRANULARITY_DAY;
    }

    private int normalizePeriodCount(String granularity, Integer days) {
        // 不同粒度设置不同上限，避免超大窗口导致统计查询压力过高。
        int fallback = 30;
        int value = (days == null || days < 1) ? fallback : days;
        int max = switch (granularity) {
            case GRANULARITY_WEEK -> 52;
            case GRANULARITY_MONTH -> 24;
            default -> 90;
        };
        return Math.min(value, max);
    }

    private int normalizeRangeDays(Integer days) {
        // taxonomy/publish-time 统计窗口单独限制，防止一次请求扫描过久。
        int fallback = 30;
        int value = (days == null || days < 1) ? fallback : days;
        return Math.min(value, 180);
    }

    private int normalizeTopLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    private long sumMapValues(Map<String, Long> map) {
        if (map == null || map.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (Long value : map.values()) {
            total += safeLong(value);
        }
        return total;
    }

    private String normalizeCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return "未分类";
        }
        return raw.trim();
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : tags.split(",")) {
            String value = item == null ? "" : item.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private double safeRate(long numerator, long denominator) {
        // 分母为 0 时强制返回 0，避免前端出现 NaN/Infinity。
        if (denominator <= 0) {
            return 0D;
        }
        return round((double) numerator / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10000D) / 10000D;
    }

    private List<Map<String, Object>> safeRows(List<Map<String, Object>> rows) {
        return rows == null ? List.of() : rows;
    }

    private Object fieldValue(Map<String, Object> row, String key) {
        if (row == null || row.isEmpty() || key == null || key.isBlank()) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String fieldString(Map<String, Object> row, String key) {
        Object value = fieldValue(row, key);
        return value == null ? "" : String.valueOf(value);
    }

    private long fieldLong(Map<String, Object> row, String key) {
        return safeLong(fieldValue(row, key));
    }

    private long safeLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // try next format
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static final class TrendBundle {
        private final Map<String, Long> viewMap;
        private final Map<String, Long> likeMap;
        private final Map<String, Long> collectionMap;
        private final Map<String, Long> commentMap;
        private final Map<String, Long> followerMap;
        private final List<Map<String, Object>> points;

        private TrendBundle(Map<String, Long> viewMap,
                            Map<String, Long> likeMap,
                            Map<String, Long> collectionMap,
                            Map<String, Long> commentMap,
                            Map<String, Long> followerMap,
                            List<Map<String, Object>> points) {
            this.viewMap = viewMap;
            this.likeMap = likeMap;
            this.collectionMap = collectionMap;
            this.commentMap = commentMap;
            this.followerMap = followerMap;
            this.points = points;
        }
    }

    private static final class CategoryAgg {
        private final String name;
        private long contentCount;
        private long totalViews;
        private long totalLikes;
        private long totalCollections;
        private long totalComments;
        private long totalEngagement;

        private CategoryAgg(String name) {
            this.name = name;
        }

        private void add(long views, long likes, long collections, long comments, long engagement) {
            this.contentCount += 1;
            this.totalViews += views;
            this.totalLikes += likes;
            this.totalCollections += collections;
            this.totalComments += comments;
            this.totalEngagement += engagement;
        }

        private long totalEngagement() {
            return totalEngagement;
        }

        private long contentCount() {
            return contentCount;
        }

        private String name() {
            return name;
        }

        private Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("name", name);
            view.put("contentCount", contentCount);
            view.put("totalViews", totalViews);
            view.put("totalLikes", totalLikes);
            view.put("totalCollections", totalCollections);
            view.put("totalComments", totalComments);
            view.put("totalEngagement", totalEngagement);
            view.put("avgEngagementRate", totalViews <= 0 ? 0D : Math.round(((double) totalEngagement / totalViews) * 10000D) / 10000D);
            return view;
        }
    }

    private static final class TagAgg {
        private final String name;
        private long contentCount;
        private long totalViews;
        private long totalLikes;
        private long totalCollections;
        private long totalComments;
        private long totalEngagement;

        private TagAgg(String name) {
            this.name = name;
        }

        private void add(long views, long likes, long collections, long comments, long engagement) {
            this.contentCount += 1;
            this.totalViews += views;
            this.totalLikes += likes;
            this.totalCollections += collections;
            this.totalComments += comments;
            this.totalEngagement += engagement;
        }

        private long totalEngagement() {
            return totalEngagement;
        }

        private long contentCount() {
            return contentCount;
        }

        private String name() {
            return name;
        }

        private Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("name", name);
            view.put("contentCount", contentCount);
            view.put("totalViews", totalViews);
            view.put("totalLikes", totalLikes);
            view.put("totalCollections", totalCollections);
            view.put("totalComments", totalComments);
            view.put("totalEngagement", totalEngagement);
            view.put("avgEngagementRate", totalViews <= 0 ? 0D : Math.round(((double) totalEngagement / totalViews) * 10000D) / 10000D);
            return view;
        }
    }

    private static final class HourAgg {
        private final int hour;
        private long contentCount;
        private long totalViews;
        private long totalLikes;
        private long totalCollections;
        private long totalComments;
        private long totalEngagement;

        private HourAgg(int hour) {
            this.hour = hour;
        }

        private void add(long views, long likes, long collections, long comments, long engagement) {
            this.contentCount += 1;
            this.totalViews += views;
            this.totalLikes += likes;
            this.totalCollections += collections;
            this.totalComments += comments;
            this.totalEngagement += engagement;
        }

        private long contentCount() {
            return contentCount;
        }

        private double engagementRate() {
            if (totalViews <= 0) {
                return 0D;
            }
            return Math.round(((double) totalEngagement / totalViews) * 10000D) / 10000D;
        }

        private Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("hour", hour);
            view.put("label", String.format(Locale.ROOT, "%02d:00", hour));
            view.put("contentCount", contentCount);
            view.put("totalViews", totalViews);
            view.put("totalLikes", totalLikes);
            view.put("totalCollections", totalCollections);
            view.put("totalComments", totalComments);
            view.put("totalEngagement", totalEngagement);
            view.put("engagementRate", engagementRate());
            return view;
        }

        private Map<String, Object> toRecommendView() {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("hour", hour);
            view.put("label", String.format(Locale.ROOT, "%02d:00", hour));
            view.put("contentCount", contentCount);
            view.put("engagementRate", engagementRate());
            return view;
        }
    }
}
