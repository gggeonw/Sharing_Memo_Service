package server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TextEditorWebSocketHandler textEditorWebSocketHandler;

    public WebSocketConfig(TextEditorWebSocketHandler textEditorWebSocketHandler) {
        this.textEditorWebSocketHandler = textEditorWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(textEditorWebSocketHandler, "/ws/text-editor").setAllowedOrigins("*");
    }
}
