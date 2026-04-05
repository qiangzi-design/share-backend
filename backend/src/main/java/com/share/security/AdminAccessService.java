package com.share.security;

import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminAccessService {

    private final UserService userService;

    public AdminAccessService(UserService userService) {
        this.userService = userService;
    }

    public Map<String, Object> getAdminMe(Long userId) {
        User user = userService.getById(userId);
        if (user == null || !UserStatusCodes.isUsable(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "User is disabled");
        }

        List<String> roles = userService.getRoleCodesByUserId(userId);
        List<String> permissions = userService.getPermissionCodesByUserId(userId);
        if (permissions == null || permissions.isEmpty()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "Admin permission is required");
        }

        return Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "roles", roles == null ? List.of() : roles,
                "permissions", permissions
        );
    }

    public void requirePermission(Long userId, String permissionCode) {
        List<String> permissions = userService.getPermissionCodesByUserId(userId);
        if (permissions == null || permissions.isEmpty() || !permissions.contains(permissionCode)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "No permission to access this admin resource");
        }
    }
}
