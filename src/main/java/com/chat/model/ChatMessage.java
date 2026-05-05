package com.chat.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {

    public enum MessageType {
        CHAT,       // Regular text message
        JOIN,       // User connected
        LEAVE       // User disconnected
    }

    private MessageType type;
    private String sender;
    private String recipient;
    private String content;
    private String timestamp;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public ChatMessage(MessageType type, String sender, String content) {
        this();
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}
