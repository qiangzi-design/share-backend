package com.share.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.entity.User;

import java.util.List;

/**
 * 用户服务接口
 * 定义用户相关的业务逻辑方法
 */
public interface UserService extends IService<User> {
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象，不存在则返回null
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户对象，不存在则返回null
     */
    User findByEmail(String email);

    /**
     * 用户注册
     * @param user 用户信息
     */
    void register(User user);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回用户对象
     */
    User login(String username, String password);

    /**
     * 用户登出
     * @param username 用户名
     */
    void logout(String username);

    /**
     * 根据用户名获取用户ID
     * @param username 用户名
     * @return 用户ID，不存在则返回null
     */
    Long getUserIdByUsername(String username);

    /**
     * 更新用户资料
     */
    boolean updateProfile(Long userId, String nickname, String bio);

    /**
     * 更新用户头像
     */
    boolean updateAvatar(Long userId, String avatar);

    /**
     * 获取用户管理角色
     */
    List<String> getRoleCodesByUserId(Long userId);

    /**
     * 获取用户管理权限
     */
    List<String> getPermissionCodesByUserId(Long userId);
}
