package com.chat.handler;

import com.chat.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Runs BEFORE the WebSocket handshake is completed.
 *
 * It reads ?username=user1&password=pass1 from the WS URL query string,
 * validates credentials via AuthService, and either:
 *   - stores "username" in the session attributes (allow)
 *   - returns false to reject the handshake (deny)
 */
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String query = request.getURI().getQuery(); // "username=user1&password=pass1"
        if (query == null || query.isBlank()) {
            System.out.println("🚫  Handshake rejected: no credentials in URL.");
            return false;
        }

        Map<String, String> params = parseQuery(query);
        String username = params.get("username");
        String password = params.get("password");

        if (!authService.authenticate(username, password)) {
            System.out.println("🚫  Handshake rejected: bad credentials for user=" + username);
            return false;
        }

        // Store username so the handler can read it later
        attributes.put("username", username);
        System.out.println("🔑  Handshake accepted for: " + username);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after handshake
    }

    /** Parses "key=value&key2=value2" into a Map. */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new java.util.HashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}
