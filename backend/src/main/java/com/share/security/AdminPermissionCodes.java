package com.share.security;

import java.util.Set;

public final class AdminPermissionCodes {

    public static final String DASHBOARD_READ = "admin.dashboard.read";
    public static final String USER_READ = "admin.user.read";
    public static final String USER_BAN = "admin.user.ban";
    public static final String USER_MUTE = "admin.user.mute";
    public static final String CONTENT_READ = "admin.content.read";
    public static final String CONTENT_OFF_SHELF = "admin.content.off_shelf";
    public static final String CONTENT_RESTORE = "admin.content.restore";
    public static final String COMMENT_READ = "admin.comment.read";
    public static final String COMMENT_HIDE = "admin.comment.hide";
    public static final String REPORT_READ = "admin.report.read";
    public static final String REPORT_HANDLE = "admin.report.handle";
    public static final String AUDIT_READ = "admin.audit.read";
    public static final String CATEGORY_READ = "admin.category.read";
    public static final String CATEGORY_WRITE = "admin.category.write";
    public static final String TAG_READ = "admin.tag.read";
    public static final String TAG_WRITE = "admin.tag.write";
    public static final String ANNOUNCEMENT_READ = "admin.announcement.read";
    public static final String ANNOUNCEMENT_WRITE = "admin.announcement.write";
    public static final String TEMPLATE_READ = "admin.template.read";
    public static final String TEMPLATE_WRITE = "admin.template.write";
    public static final String ANALYTICS_READ = "admin.analytics.read";
    public static final String USER_RISK_MARK = "admin.user.risk_mark";

    public static final Set<String> ALL = Set.of(
            DASHBOARD_READ,
            USER_READ,
            USER_BAN,
            USER_MUTE,
            CONTENT_READ,
            CONTENT_OFF_SHELF,
            CONTENT_RESTORE,
            COMMENT_READ,
            COMMENT_HIDE,
            REPORT_READ,
            REPORT_HANDLE,
            AUDIT_READ,
            CATEGORY_READ,
            CATEGORY_WRITE,
            TAG_READ,
            TAG_WRITE,
            ANNOUNCEMENT_READ,
            ANNOUNCEMENT_WRITE,
            TEMPLATE_READ,
            TEMPLATE_WRITE,
            ANALYTICS_READ,
            USER_RISK_MARK
    );

    private AdminPermissionCodes() {
    }
}
