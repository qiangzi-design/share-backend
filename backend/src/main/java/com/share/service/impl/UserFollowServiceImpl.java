package com.share.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.dto.PageResult;
import com.share.entity.User;
import com.share.entity.UserFollow;
import com.share.exception.BusinessException;
import com.share.mapper.UserFollowMapper;
import com.share.mapper.UserMapper;
import com.share.security.UserStatusCodes;
import com.share.service.UserFollowService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
/**
 * 关注关系服务：
 * - 管理关注/取关关系；
 * - 记录 follow_events 供趋势统计；
 * - 提供粉丝/关注列表与增长曲线。
 */
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements UserFollowService {

    private static final int FOLLOW_EVENT_TYPE = 1;
    private static final int UNFOLLOW_EVENT_TYPE = 2;

    private static final DateTimeFormatter DAY_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;

    public UserFollowServiceImpl(UserFollowMapper userFollowMapper, UserMapper userMapper) {
        this.userFollowMapper = userFollowMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    // 切换关注：已关注则取关，未关注则关注，并写入事件表。
    public boolean toggleFollow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "不能关注自己");
        }

        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null || UserStatusCodes.isBanned(targetUser.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "目标用户不存在");
        }

        UserFollow existing = userFollowMapper.findByUserIdAndTargetUserId(userId, targetUserId);
        if (existing != null) {
            removeById(existing.getId());
            userFollowMapper.insertFollowEvent(userId, targetUserId, UNFOLLOW_EVENT_TYPE);
            return false;
        }

        UserFollow follow = new UserFollow();
        follow.setUserId(userId);
        follow.setTargetUserId(targetUserId);
        follow.setCreateTime(LocalDateTime.now());
        save(follow);
        userFollowMapper.insertFollowEvent(userId, targetUserId, FOLLOW_EVENT_TYPE);
        return true;
    }

    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        return userFollowMapper.findByUserIdAndTargetUserId(userId, targetUserId) != null;
    }

    @Override
    public Integer getFollowerCount(Long userId) {
        return userFollowMapper.countFollowers(userId);
    }

    @Override
    public Integer getFollowingCount(Long userId) {
        return userFollowMapper.countFollowing(userId);
    }

    @Override
    public Integer getUnfollowCount(Long userId) {
        return userFollowMapper.countUnfollowEvents(userId);
    }

    @Override
    // 汇总用于个人主页统计卡片。
    public Map<String, Object> getFollowSummary(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("followerCount", getFollowerCount(userId));
        result.put("followingCount", getFollowingCount(userId));
        result.put("unfollowCount", getUnfollowCount(userId));
        return result;
    }

    @Override
    // 关注增长支持按天/按月/按年三种粒度。
    public List<Map<String, Object>> getFollowerGrowth(Long userId, String period, Integer size) {
        String normalizedPeriod = normalizePeriod(period);
        int points = normalizeSize(normalizedPeriod, size);

        return switch (normalizedPeriod) {
            case "month" -> buildMonthGrowth(userId, points);
            case "year" -> buildYearGrowth(userId, points);
            default -> buildDayGrowth(userId, points);
        };
    }

    @Override
    // 粉丝列表分页。
    public PageResult<Map<String, Object>> getFollowerList(Long userId, Integer page, Integer pageSize) {
        int validPage = page == null || page < 1 ? 1 : page;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (validPage - 1) * validPageSize;

        List<Map<String, Object>> list = userFollowMapper.findFollowers(userId, offset, validPageSize);
        long total = userFollowMapper.countFollowersForList(userId);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    // 关注列表分页。
    public PageResult<Map<String, Object>> getFollowingList(Long userId, Integer page, Integer pageSize) {
        int validPage = page == null || page < 1 ? 1 : page;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (validPage - 1) * validPageSize;

        List<Map<String, Object>> list = userFollowMapper.findFollowingUsers(userId, offset, validPageSize);
        long total = userFollowMapper.countFollowingForList(userId);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    private List<Map<String, Object>> buildDayGrowth(Long userId, int points) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(points - 1L);
        LocalDateTime startTime = start.atStartOfDay();

        Map<String, Integer> followMap = toCountMap(userFollowMapper.countFollowEventsByDay(userId, startTime, FOLLOW_EVENT_TYPE));
        Map<String, Integer> unfollowMap = toCountMap(userFollowMapper.countFollowEventsByDay(userId, startTime, UNFOLLOW_EVENT_TYPE));

        List<Map<String, Object>> result = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            LocalDate current = start.plusDays(i);
            String key = current.format(DAY_KEY_FORMATTER);
            result.add(buildGrowthPoint(key, current.format(DAY_LABEL_FORMATTER), followMap, unfollowMap));
        }
        return result;
    }

    private List<Map<String, Object>> buildMonthGrowth(Long userId, int points) {
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(points - 1L);
        LocalDateTime startTime = start.atDay(1).atStartOfDay();

        Map<String, Integer> followMap = toCountMap(userFollowMapper.countFollowEventsByMonth(userId, startTime, FOLLOW_EVENT_TYPE));
        Map<String, Integer> unfollowMap = toCountMap(userFollowMapper.countFollowEventsByMonth(userId, startTime, UNFOLLOW_EVENT_TYPE));

        List<Map<String, Object>> result = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            YearMonth current = start.plusMonths(i);
            String key = current.format(MONTH_KEY_FORMATTER);
            result.add(buildGrowthPoint(key, key, followMap, unfollowMap));
        }
        return result;
    }

    private List<Map<String, Object>> buildYearGrowth(Long userId, int points) {
        int endYear = LocalDate.now().getYear();
        int startYear = endYear - points + 1;
        LocalDateTime startTime = LocalDate.of(startYear, 1, 1).atStartOfDay();

        Map<String, Integer> followMap = toCountMap(userFollowMapper.countFollowEventsByYear(userId, startTime, FOLLOW_EVENT_TYPE));
        Map<String, Integer> unfollowMap = toCountMap(userFollowMapper.countFollowEventsByYear(userId, startTime, UNFOLLOW_EVENT_TYPE));

        List<Map<String, Object>> result = new ArrayList<>(points);
        for (int year = startYear; year <= endYear; year++) {
            String key = String.valueOf(year);
            result.add(buildGrowthPoint(key, key, followMap, unfollowMap));
        }
        return result;
    }

    private Map<String, Object> buildGrowthPoint(String key,
                                                 String label,
                                                 Map<String, Integer> followMap,
                                                 Map<String, Integer> unfollowMap) {
        int followCount = followMap.getOrDefault(key, 0);
        int unfollowCount = unfollowMap.getOrDefault(key, 0);

        Map<String, Object> point = new HashMap<>();
        point.put("period", key);
        point.put("label", label);
        point.put("followCount", followCount);
        point.put("unfollowCount", unfollowCount);
        point.put("netCount", followCount - unfollowCount);
        point.put("count", followCount);
        return point;
    }

    private Map<String, Integer> toCountMap(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        return rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row.get("bucket")),
                row -> parseCount(row.get("total")),
                Integer::sum
        ));
    }

    private int parseCount(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String normalizePeriod(String period) {
        if (period == null) {
            return "day";
        }
        String normalized = period.trim().toLowerCase(Locale.ROOT);
        if ("month".equals(normalized) || "year".equals(normalized)) {
            return normalized;
        }
        return "day";
    }

    private int normalizeSize(String period, Integer size) {
        int fallback = switch (period) {
            case "month" -> 12;
            case "year" -> 5;
            default -> 14;
        };
        if (size == null || size <= 0) {
            return fallback;
        }
        int max = switch (period) {
            case "month" -> 36;
            case "year" -> 20;
            default -> 90;
        };
        return Math.min(size, max);
    }
}
