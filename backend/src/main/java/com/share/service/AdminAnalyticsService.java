package com.share.service;

import java.util.Map;

public interface AdminAnalyticsService {

    Map<String, Object> getContentQuality(String granularity, Integer days);

    Map<String, Object> getUserGrowth(String granularity, Integer days);

    Map<String, Object> getModerationEfficiency(String granularity, Integer days);
}
