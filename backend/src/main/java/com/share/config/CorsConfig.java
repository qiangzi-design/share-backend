package com.share.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域资源共享(CORS)配置类
 * 用于解决前端跨域访问后端API的问题
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final LegacyApiPathInterceptor legacyApiPathInterceptor;

    public CorsConfig(LegacyApiPathInterceptor legacyApiPathInterceptor) {
        this.legacyApiPathInterceptor = legacyApiPathInterceptor;
    }

    /**
     * 配置CORS映射规则
     * @param registry CORS注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 对所有路径应用CORS规则
                .allowedOriginPatterns("*")  // 允许所有来源访问（生产环境应限制具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的HTTP方法
                .allowedHeaders("*")  // 允许所有请求头
                .allowCredentials(true)  // 允许携带凭证（如Cookie）
                .maxAge(3600);  // 预检请求的有效期（秒），在此期间无需重复发送预检请求
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(legacyApiPathInterceptor);
    }
}
