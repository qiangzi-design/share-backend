package com.share.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.mapper.UserMapper;
import com.share.security.UserStatusCodes;
import com.share.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * 服务职责：用户账号核心流程。
 * 负责注册、登录鉴权、资料维护以及 RBAC 角色/权限查询。
 */
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public void register(User user) {
        // 用户名与邮箱任一重复都拒绝，避免账号体系出现歧义身份。
        User existingUser = findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "用户名或邮箱已存在");
        }

        User existingEmail = findByEmail(user.getEmail());
        if (existingEmail != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "用户名或邮箱已存在");
        }

        // 密码只以加密摘要入库，不落明文。
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(UserStatusCodes.NORMAL);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        boolean saved = save(user);
        if (!saved) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, 500, "注册失败，请稍后重试");
        }
    }

    @Override
    public User login(String username, String password) {
        User user = findByUsername(username);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "用户名或密码错误");
        }

        // 封禁用户禁止登录，确保治理策略实时生效。
        if (UserStatusCodes.isBanned(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "用户已被封禁");
        }

        return user;
    }

    @Override
    public void logout(String username) {
        // 无状态 JWT 模式下，登出主要由前端清理 token 完成。
    }

    @Override
    public Long getUserIdByUsername(String username) {
        User user = findByUsername(username);
        return user != null ? user.getId() : null;
    }

    @Override
    public boolean updateProfile(Long userId, String nickname, String bio) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }

        user.setNickname(nickname);
        user.setBio(bio);
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public boolean updateAvatar(Long userId, String avatar) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }

        user.setAvatar(avatar);
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        // 管理端路由与按钮权限的角色来源。
        return userMapper.findRoleCodesByUserId(userId);
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        // 细粒度权限校验来源（接口兜底校验使用）。
        return userMapper.findPermissionCodesByUserId(userId);
    }
}
