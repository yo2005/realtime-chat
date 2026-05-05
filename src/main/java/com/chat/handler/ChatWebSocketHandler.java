package com.chat.handler;

import com.chat.model.ChatMessage;
import com.chat.service.MessageHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central WebSocket handler.
 *
 * Flow:
 *  1. Client connects  → onOpen  → stored in activeSessions map
 *  2. Client sends msg → onMessage → forwarded to the other user
 *  3. Client leaves    → onClose → other user is notified
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // username -> WebSocketSession  (max 2 entries)
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MessageHistoryService historyService;

    // ---------------------------------------------------------------
    // Connection opened
    // ---------------------------------------------------------------
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = getUsername(session);
        if (username == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Replace any stale session for this user
        activeSessions.put(username, session);
        System.out.println("✅  " + username + " connected. [sessionId=" + session.getId() + "]");

        // Send message history to the newly connected user
        sendHistory(session);

        // Broadcast JOIN event to the other user
        String other = getOtherUser(username);
        ChatMessage joinMsg = new ChatMessage(ChatMessage.MessageType.JOIN, "System",
                username + " has joined the chat.");
        broadcastTo(other, joinMsg);
        historyService.addMessage(joinMsg);
    }

    // ---------------------------------------------------------------
    // Message received
    // ---------------------------------------------------------------
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage raw) throws Exception {
        String username = getUsername(session);
        if (username == null) return;

        ChatMessage incoming;
        try {
            incoming = objectMapper.readValue(raw.getPayload(), ChatMessage.class);
        } catch (Exception e) {
            sendError(session, "Invalid message format. Expected JSON.");
            return;
        }

        // Stamp the sender (don't trust the client)
        incoming.setSender(username);
        incoming.setType(ChatMessage.MessageType.CHAT);

        System.out.println("💬  " + incoming);
        historyService.addMessage(incoming);

        // Forward to the other connected user
        String other = getOtherUser(username);
        if (other != null && activeSessions.containsKey(other)) {
            broadcastTo(other, incoming);
        } else {
            // Echo an info message back: other user isn't connected
            sendInfo(session, "The other user is not connected yet. Your message is saved.");
        }

        // Echo back to sender with confirmation
        broadcastTo(username, incoming);
    }

    // ---------------------------------------------------------------
    // Connection closed
    // ---------------------------------------------------------------
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = getUsername(session);
        if (username == null) return;

        activeSessions.remove(username);
        System.out.println("❌  " + username + " disconnected. Reason: " + status.getReason());

        // Notify the other user
        String other = getOtherUser(username);
        ChatMessage leaveMsg = new ChatMessage(ChatMessage.MessageType.LEAVE, "System",
                username + " has left the chat.");
        broadcastTo(other, leaveMsg);
        historyService.addMessage(leaveMsg);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("⚠️  Transport error for session " + session.getId() + ": " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Sends all in-memory history to a freshly connected user. */
    private void sendHistory(WebSocketSession session) throws IOException {
        for (ChatMessage msg : historyService.getHistory()) {
            send(session, msg);
        }
    }

    /** Sends a message to a specific user by username. */
    private void broadcastTo(String username, ChatMessage msg) {
        if (username == null) return;
        WebSocketSession target = activeSessions.get(username);
        if (target != null && target.isOpen()) {
            try {
                send(target, msg);
            } catch (IOException e) {
                System.err.println("Failed to send to " + username + ": " + e.getMessage());
            }
        }
    }

    private void send(WebSocketSession session, ChatMessage msg) throws IOException {
        String json = objectMapper.writeValueAsString(msg);
        session.sendMessage(new TextMessage(json));
    }

    private void sendError(WebSocketSession session, String errorText) {
        ChatMessage err = new ChatMessage(ChatMessage.MessageType.CHAT, "System", "ERROR: " + errorText);
        try { send(session, err); } catch (IOException ignored) {}
    }

    private void sendInfo(WebSocketSession session, String infoText) {
        ChatMessage info = new ChatMessage(ChatMessage.MessageType.CHAT, "System", infoText);
        try { send(session, info); } catch (IOException ignored) {}
    }

    /** Extracts username stored in the WebSocket session attributes. */
    private String getUsername(WebSocketSession session) {
        Object user = session.getAttributes().get("username");
        return (user instanceof String s) ? s : null;
    }

    /** For a two-user chat, returns the peer's username. */
    private String getOtherUser(String username) {
        if ("user1".equals(username)) return "user2";
        if ("user2".equals(username)) return "user1";
        return null;
    }

    public Map<String, WebSocketSession> getActiveSessions() {
        return activeSessions;
    }
}
