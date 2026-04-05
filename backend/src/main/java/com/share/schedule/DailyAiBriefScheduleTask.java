package com.share.schedule;

import com.share.service.DailyAiBriefService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日AI快讯定时任务：
 * - 每天上午9点触发；
 * - 抓取失败仅记录日志，不影响主业务服务。
 */
@Component
public class DailyAiBriefScheduleTask {

    private static final Logger log = LoggerFactory.getLogger(DailyAiBriefScheduleTask.class);

    private final DailyAiBriefService dailyAiBriefService;

    @Value("${ai-brief.enabled:true}")
    private Boolean enabled;

    public DailyAiBriefScheduleTask(DailyAiBriefService dailyAiBriefService) {
        this.dailyAiBriefService = dailyAiBriefService;
    }

    /**
     * 每日09:00自动抓取AI热点。
     * cron 与时区均支持配置覆盖。
     */
    @Scheduled(cron = "${ai-brief.cron:0 0 9 * * ?}", zone = "${ai-brief.zone:Asia/Shanghai}")
    public void refreshDailyBrief() {
        if (Boolean.FALSE.equals(enabled)) {
            log.info("AI快讯定时任务已关闭，跳过本次执行");
            return;
        }

        LocalDate today = LocalDate.now();
        try {
            dailyAiBriefService.refreshBrief(today, null, null, null);
            log.info("AI快讯定时任务执行完成，date={}", today);
        } catch (Exception ex) {
            log.warn("AI快讯定时任务执行失败，date={}, reason={}", today, ex.getMessage());
        }
    }
}
