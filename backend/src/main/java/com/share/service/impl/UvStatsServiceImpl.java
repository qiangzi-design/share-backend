package com.share.service.impl;

import com.share.service.UvStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UvStatsServiceImpl implements UvStatsService {

    private static final Logger log = LoggerFactory.getLogger(UvStatsServiceImpl.class);
    private static final DateTimeFormatter DAY_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int KEY_TTL_DAYS = 400;

    private final StringRedisTemplate stringRedisTemplate;

    public UvStatsServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void recordVisit(String viewerKey) {
        // 仅使用 viewerKey 去重，确保 UV 口径单一一致。
        String visitorId = normalizeVisitorId(viewerKey);
        if (visitorId == null) {
            return;
        }

        String key = buildDailyKey(LocalDate.now());
        try {
            stringRedisTemplate.opsForHyperLogLog().add(key, visitorId);
            stringRedisTemplate.expire(key, Duration.ofDays(KEY_TTL_DAYS));
        } catch (Exception ex) {
            // fail-open：Redis 异常不阻断主流程
            log.warn("UV record failed, continue without blocking request. key={}, reason={}", key, ex.getMessage());
        }
    }

    @Override
    public long getDailyUv(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        String key = buildDailyKey(targetDate);
        return safeSize(key);
    }

    @Override
    public long getMonthlyUv(YearMonth month) {
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        LocalDate cursor = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();
        return countMerged(cursor, end);
    }

    @Override
    public Map<String, Long> listDailyUv(LocalDate fromDate, LocalDate toDate) {
        LocalDate start = fromDate == null ? LocalDate.now() : fromDate;
        LocalDate end = toDate == null ? LocalDate.now() : toDate;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        Map<String, Long> result = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            result.put(cursor.format(DAY_LABEL_FORMATTER), getDailyUv(cursor));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    @Override
    public Map<String, Long> listMonthlyUv(YearMonth fromMonth, YearMonth toMonth) {
        YearMonth start = fromMonth == null ? YearMonth.now() : fromMonth;
        YearMonth end = toMonth == null ? YearMonth.now() : toMonth;
        if (start.isAfter(end)) {
            YearMonth tmp = start;
            start = end;
            end = tmp;
        }

        Map<String, Long> result = new LinkedHashMap<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            result.put(cursor.format(MONTH_LABEL_FORMATTER), getMonthlyUv(cursor));
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private long countMerged(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            return 0L;
        }
        try {
            int days = (int) (toDate.toEpochDay() - fromDate.toEpochDay() + 1);
            String[] keys = new String[days];
            LocalDate cursor = fromDate;
            int idx = 0;
            while (!cursor.isAfter(toDate)) {
                keys[idx++] = buildDailyKey(cursor);
                cursor = cursor.plusDays(1);
            }
            Long merged = stringRedisTemplate.opsForHyperLogLog().size(keys);
            return merged == null ? 0L : Math.max(merged, 0L);
        } catch (Exception ex) {
            log.warn("UV merged count failed, fallback to 0. from={}, to={}, reason={}", fromDate, toDate, ex.getMessage());
            return 0L;
        }
    }

    private long safeSize(String key) {
        try {
            Long value = stringRedisTemplate.opsForHyperLogLog().size(key);
            return value == null ? 0L : Math.max(value, 0L);
        } catch (Exception ex) {
            log.warn("UV query failed, fallback to 0. key={}, reason={}", key, ex.getMessage());
            return 0L;
        }
    }

    private String buildDailyKey(LocalDate date) {
        return "uv:site:day:" + date.format(DAY_KEY_FORMATTER);
    }

    /**
     * 标准化 visitorId：
     * 1. 只接收安全字符，避免异常输入污染 Redis key/value；
     * 2. 统一前缀 vk:，明确该值来自 viewer_key。
     */
    private String normalizeVisitorId(String viewerKey) {
        String cleanedViewerKey = normalizeViewerKey(viewerKey);
        if (cleanedViewerKey == null) {
            return null;
        }
        return "vk:" + cleanedViewerKey;
    }

    private String normalizeViewerKey(String viewerKey) {
        if (viewerKey == null || viewerKey.isBlank()) {
            return null;
        }
        String cleaned = viewerKey.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }
}
