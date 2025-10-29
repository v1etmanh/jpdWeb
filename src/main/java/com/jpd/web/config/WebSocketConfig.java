package com.jpd.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Client subscribe để nhận message
        config.enableSimpleBroker("/topic", "/queue");
        // Client gửi message đến server
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-quiz")
                // ĐỔI TỪ setAllowedOrigins("*") SANG:
                .setAllowedOriginPatterns("*")  // Cho phép mọi origin
                .withSockJS();
        
        // HOẶC nếu muốn cụ thể hơn (khuyến nghị cho production):
        // .setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
    }
}