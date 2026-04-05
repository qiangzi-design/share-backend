package com.share.service;

import java.util.Map;

public interface AdminDashboardService {

    Map<String, Object> getOverview();

    Map<String, Object> getTrends(String granularity, Integer days);
}

