package com.chat.service;

import com.chat.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores chat messages in memory for the duration of the session.
 * Messages are lost when the server restarts (no DB persistence by design).
 */
@Service
public class MessageHistoryService {

    // Shared message history for the single chat room
    private final List<ChatMessage> history = Collections.synchronizedList(new ArrayList<>());

    public void addMessage(ChatMessage message) {
        history.add(message);
    }

    public List<ChatMessage> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void clearHistory() {
        history.clear();
    }

    public int getMessageCount() {
        return history.size();
    }
}
