package com.share.service.impl;

import com.share.entity.Content;
import com.share.mapper.ContentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock
    private ContentMapper contentMapper;

    @InjectMocks
    private ContentServiceImpl contentService;

    private Content testContent;

    @BeforeEach
    void setUp() {
        testContent = new Content();
        testContent.setId(1L);
        testContent.setTitle("Test Title");
        testContent.setContent("Test content here");
        testContent.setUserId(1L);
        testContent.setCategoryId(1L);
        
        // 使用反射设置 baseMapper
        try {
            java.lang.reflect.Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(contentService, contentMapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void publish_ValidContent_ReturnsTrue() {
        when(contentMapper.insert(any(Content.class))).thenReturn(1);

        boolean result = contentService.publish(testContent);

        assertTrue(result);
        assertEquals(0, testContent.getViewCount());
        assertEquals(0, testContent.getLikeCount());
        assertEquals(0, testContent.getCommentCount());
        assertEquals(1, testContent.getStatus());
        assertNotNull(testContent.getCreateTime());
        assertNotNull(testContent.getUpdateTime());
        verify(contentMapper).insert(any(Content.class));
    }

    @Test
    void publish_InsertFails_ReturnsFalse() {
        when(contentMapper.insert(any(Content.class))).thenReturn(0);

        boolean result = contentService.publish(testContent);

        assertFalse(result);
        verify(contentMapper).insert(any(Content.class));
    }

    @Test
    void publish_NullContent_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            contentService.publish(null);
        });
    }
}