package com.zapiee.orderservice.config;

import com.zapiee.orderservice.handler.OrderWebSocketHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Tag(name = "Order WebSocket Configuration",
        description = "WebSocket configuration for real-time customer, staff and owner order updates."
)
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler handler;
    private final WebSocketInterceptor interceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/orders")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}
