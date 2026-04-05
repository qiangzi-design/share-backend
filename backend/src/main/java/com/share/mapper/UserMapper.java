package com.share.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户数据访问接口
 * 定义用户相关的数据库操作方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象
     */
    User findByEmail(String email);

    @Select("""
            SELECT DISTINCT r.code
            FROM admin_user_roles ur
            INNER JOIN admin_roles r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY r.code
            """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.code
            FROM admin_user_roles ur
            INNER JOIN admin_role_permissions rp ON rp.role_id = ur.role_id
            INNER JOIN admin_permissions p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
            ORDER BY p.code
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);
}
