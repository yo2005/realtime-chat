# Real-Time P2P Chat — Spring WebSocket

A two-user real-time chat application built with Spring Boot 3 and WebSocket.

---

## Project Structure

```
chat-app/
├── pom.xml
└── src/main/
    ├── java/com/chat/
    │   ├── ChatApplication.java          ← Entry point
    │   ├── config/
    │   │   ├── WebSocketConfig.java      ← Registers /chat endpoint
    │   │   └── SecurityConfig.java       ← Disables Spring Security default login
    │   ├── handler/
    │   │   ├── ChatWebSocketHandler.java ← Core: open/message/close logic
    │   │   └── AuthHandshakeInterceptor  ← Auth before WS upgrade
    │   ├── model/
    │   │   └── ChatMessage.java          ← Message POJO (type, sender, content, ts)
    │   └── service/
    │       ├── AuthService.java          ← Hardcoded credentials
    │       └── MessageHistoryService.java← In-memory message log
    └── resources/
        ├── application.properties
        └── static/
            └── index.html                ← Browser chat UI
```

---

## Prerequisites (What You Need to Install)

### 1. Java 17+
Download from: https://adoptium.net  
After install, verify:
```bash
java -version
# should show: openjdk 17...
```

### 2. Maven 3.8+
Download from: https://maven.apache.org/download.cgi  
Or use the Maven wrapper (no install needed — see below).

After install, verify:
```bash
mvn -version
```

> **Tip:** If you use IntelliJ IDEA or VS Code with the Java Extension Pack,
> they bundle Maven — you may not need to install it separately.

---

## Running the App

### Step 1 — Build & Start the server
```bash
cd chat-app
mvn spring-boot:run
```

You'll see:
```
Started ChatApplication in 2.3 seconds
```

### Step 2 — Open the chat UI
Open **two browser tabs** (or two different browsers):

```
http://localhost:8080/index.html
```

### Step 3 — Log in with both users

| Tab   | Username | Password |
|-------|----------|----------|
| Tab 1 | user1    | pass1    |
| Tab 2 | user2    | pass2    |

Click **Connect** in each tab — you're now chatting in real time!

---

## How It Works

```
Browser (user1)                  Spring Boot Server              Browser (user2)
      |                                  |                               |
      |-- WS Handshake (/chat?user1) --> |                               |
      |                    [Auth check]  |                               |
      |<------- Connection Accepted ---- |                               |
      |                                  |                               |
      |                                  | <-- WS Handshake (user2) ---- |
      |                                  | ----Connection Accepted ----> |
      |                                  |                               |
      |--- "Hello!" ---------------->    |                               |
      |                    [Broadcast]   |                               |
      |<--- echo back ---------------    | --- "Hello!" --------------> |
      |                                  |                               |
      |                                  | <-- "Hi there!" ------------ |
      |<--- "Hi there!" -------------    | --- echo back ------------> |
```

### Authentication
- Credentials are passed as query params on the WebSocket URL
- `AuthHandshakeInterceptor` validates them before the connection is upgraded
- Invalid credentials = connection rejected (no message sent)

### Message Flow
1. Client sends JSON: `{ "content": "Hello!" }`
2. Server stamps the sender, saves to in-memory history
3. Server forwards to the other connected user
4. Both users see the message with sender + timestamp

### Disconnection
When a user closes the tab or clicks Disconnect:
- Their session is removed from the active sessions map
- The other user receives a "X has left the chat" system message

---

## Credentials

Defined in `AuthService.java`:
```java
USERS.put("user1", "pass1");
USERS.put("user2", "pass2");
```
Change them there to whatever you want.

---

## Common Issues

| Problem | Fix |
|---|---|
| `java: error: release version 17 not supported` | Install Java 17+ and set `JAVA_HOME` |
| Port 8080 already in use | Change `server.port=8081` in `application.properties` |
| Browser says "WebSocket connection failed" | Make sure the server is running first |
| Both users log in as the same user | The second login replaces the first session |
