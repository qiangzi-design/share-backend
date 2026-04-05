package com.share.service;

import java.util.Map;

public interface UserAnalyticsService {

    Map<String, Object> getOverview(Long userId, String granularity, Integer days);

    Map<String, Object> getTopContents(Long userId, Integer limit);

    Map<String, Object> getTrend(Long userId, String granularity, Integer days);

    Map<String, Object> getTaxonomy(Long userId, Integer days);

    Map<String, Object> getPublishTime(Long userId, Integer days);

    Map<String, Object> getGovernance(Long userId);
}

