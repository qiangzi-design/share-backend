package com.share.service.impl;

import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setUpdateTime(LocalDateTime.now());

        try {
            java.lang.reflect.Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                    .getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(userService, userMapper);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void findByUsername_ExistingUser_ReturnsUser() {
        when(userMapper.findByUsername("testuser")).thenReturn(testUser);

        User result = userService.findByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userMapper).findByUsername("testuser");
    }

    @Test
    void findByUsername_NonExistingUser_ReturnsNull() {
        when(userMapper.findByUsername("nonexistent")).thenReturn(null);

        User result = userService.findByUsername("nonexistent");

        assertEquals(null, result);
        verify(userMapper).findByUsername("nonexistent");
    }

    @Test
    void findByEmail_ExistingEmail_ReturnsUser() {
        when(userMapper.findByEmail("test@example.com")).thenReturn(testUser);

        User result = userService.findByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userMapper).findByEmail("test@example.com");
    }

    @Test
    void register_NewUser_Success() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password123");

        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(userMapper.findByEmail("new@example.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.register(newUser));
        assertEquals("encodedPassword123", newUser.getPassword());
        assertEquals(1, newUser.getStatus());
        assertNotNull(newUser.getCreateTime());
        assertNotNull(newUser.getUpdateTime());
    }

    @Test
    void register_ExistingUsername_ThrowsBusinessException() {
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password123");

        when(userMapper.findByUsername("testuser")).thenReturn(testUser);

        assertThrows(BusinessException.class, () -> userService.register(newUser));
        verify(userMapper).findByUsername("testuser");
        verify(userMapper, never()).findByEmail(any());
    }

    @Test
    void register_ExistingEmail_ThrowsBusinessException() {
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("test@example.com");
        newUser.setPassword("password123");

        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(userMapper.findByEmail("test@example.com")).thenReturn(testUser);

        assertThrows(BusinessException.class, () -> userService.register(newUser));
        verify(userMapper).findByUsername("newuser");
        verify(userMapper).findByEmail("test@example.com");
    }

    @Test
    void login_ValidCredentials_ReturnsUser() {
        when(userMapper.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        User result = userService.login("testuser", "password123");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void login_InvalidUsername_ThrowsBusinessException() {
        when(userMapper.findByUsername("invaliduser")).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.login("invaliduser", "password123"));
    }

    @Test
    void login_InvalidPassword_ThrowsBusinessException() {
        when(userMapper.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.login("testuser", "wrongpassword"));
    }

    @Test
    void login_InactiveUser_ThrowsBusinessException() {
        User inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setUsername("inactive");
        inactiveUser.setPassword("encodedPassword");
        inactiveUser.setStatus(3);

        when(userMapper.findByUsername("inactive")).thenReturn(inactiveUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.login("inactive", "password123"));
    }

    @Test
    void getUserIdByUsername_ExistingUser_ReturnsId() {
        when(userMapper.findByUsername("testuser")).thenReturn(testUser);
        Long result = userService.getUserIdByUsername("testuser");
        assertEquals(1L, result);
    }

    @Test
    void getUserIdByUsername_NonExistingUser_ReturnsNull() {
        when(userMapper.findByUsername("nonexistent")).thenReturn(null);
        Long result = userService.getUserIdByUsername("nonexistent");
        assertEquals(null, result);
    }

    @Test
    void login_ReturnedUserShouldBeActive() {
        when(userMapper.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        User result = userService.login("testuser", "password123");
        assertTrue(result.getStatus() == 1);
    }
}
