package com.share.security;

public final class UserStatusCodes {

    public static final int LEGACY_DISABLED = 0;
    public static final int NORMAL = 1;
    public static final int MUTED = 2;
    public static final int BANNED = 3;

    private UserStatusCodes() {
    }

    public static boolean isBanned(Integer status) {
        return status != null && (status == BANNED || status == LEGACY_DISABLED);
    }

    public static boolean isMuted(Integer status) {
        return status != null && status == MUTED;
    }

    public static boolean isUsable(Integer status) {
        return status != null && !isBanned(status);
    }
}
