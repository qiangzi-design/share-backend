package com.share.security;

import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CurrentUserService {

    private final UserService userService;

    public CurrentUserService(UserService userService) {
        this.userService = userService;
    }

    public String requireCurrentUsername() {
        String username = getCurrentUsernameOrNull();
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("用户未登录");
        }
        return username;
    }

    public Long requireCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("用户不存在");
        }
        return userId;
    }

    public User requireCurrentActiveUser() {
        Long userId = requireCurrentUserId();
        User user = userService.getById(userId);
        if (user == null || !UserStatusCodes.isUsable(user.getStatus())) {
            throw new IllegalStateException("用户已被封禁");
        }
        return user;
    }

    public void requireNotMuted(String actionName) {
        User user = requireCurrentActiveUser();
        if (UserStatusCodes.isMuted(user.getStatus())
                || (user.getMuteUntil() != null && user.getMuteUntil().isAfter(LocalDateTime.now()))) {
            String action = actionName == null || actionName.isBlank() ? "执行当前操作" : actionName;
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "用户已被禁言，暂不可" + action);
        }
    }

    public String getCurrentUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    public Long getCurrentUserIdOrNull() {
        String username = getCurrentUsernameOrNull();
        if (username == null || username.isBlank()) {
            return null;
        }
        return userService.getUserIdByUsername(username);
    }
}
