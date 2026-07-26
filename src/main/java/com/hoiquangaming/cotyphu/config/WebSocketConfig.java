package com.hoiquangaming.cotyphu.config; // Điều chỉnh package cho đúng cấu trúc của Fen

import com.hoiquangaming.cotyphu.handler.GameHandler; // Import Trọng tài
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // BẬT CÔNG NGHỆ WEBSOCKET CHO TOÀN BỘ PROJECT
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // ĐĂNG KÝ: Báo với Spring Boot là:
        // "Nếu có ai kết nối tới đường dẫn ws://localhost:8080/game
        // Thì hãy chuyển cho ông trọng tài GameHandler xử lý nhé!"
        registry.addHandler(new GameHandler(), "/game")
                .setAllowedOrigins("*"); // Cho phép mọi thiết bị (Mobile, Laptop...) kết nối vào
    }
}