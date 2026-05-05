package com.chat.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    // Anyone can register — username just needs to be unique
    private final Map<String, String> USERS = new ConcurrentHashMap<>();

    public boolean authenticate(String username, String password) {
        if (username == null || password == null || username.isBlank()) return false;
        // If username doesn't exist yet, register them automatically
        USERS.putIfAbsent(username, password);
        return USERS.get(username).equals(password);
    }

    public boolean userExists(String username) {
        return USERS.containsKey(username);
    }

    public String getOtherUser(String username) {
        return USERS.keySet().stream()
                .filter(u -> !u.equals(username))
                .findFirst()
                .orElse(null);
    }
}