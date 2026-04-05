package com.share.config;

import com.share.realtime.NotificationWebSocketAuthInterceptor;
import com.share.realtime.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationWebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                           NotificationWebSocketAuthInterceptor authInterceptor) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
