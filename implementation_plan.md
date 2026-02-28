# Fix WebSocket Real-Time Balance Updates & Notifications

## Root Cause

The backend `TransferService` correctly calls `WebSocketService.notifyTransferSent()` and `notifyTransferReceived()`, which use Spring's `convertAndSendToUser(email, "/queue/balance", message)`. This sends messages to a **user-specific destination** (`/user/{email}/queue/balance`).

**The problem:** The frontend STOMP client connects **anonymously** — it passes no authentication:

```javascript
// dashboard.html line 283
stompClient.connect({}, function(frame) { ... });
//                  ^^  — empty headers, no JWT token
```

Without authentication on the WebSocket session, Spring has **no way to know which user** a connection belongs to. So `convertAndSendToUser(email, ...)` silently drops the message because there's no matching session for that email.

There is also **no `ChannelInterceptor`** in the project that could authenticate STOMP frames using the JWT token.

## Proposed Changes

### WebSocket Authentication (Backend)

#### [NEW] [WebSocketAuthInterceptor.java](file:///c:/Users/Adarsh/OneDrive/Documents/Projects/paytm-wallet-clone/src/main/java/com/utkarsh/paytm_wallet_clone/config/WebSocketAuthInterceptor.java)

Create a `ChannelInterceptor` that:
1. Intercepts the STOMP `CONNECT` frame
2. Reads the `Authorization` header (Bearer token) from the STOMP native headers
3. Validates the JWT using existing `JwtUtil`
4. Looks up the user from `UserRepository`
5. Sets a `UsernamePasswordAuthenticationToken` as the `simpUser` on the message accessor — this binds the user identity to the WebSocket session

---

#### [MODIFY] [WebSocketConfig.java](file:///c:/Users/Adarsh/OneDrive/Documents/Projects/paytm-wallet-clone/src/main/java/com/utkarsh/paytm_wallet_clone/config/WebSocketConfig.java)

- Implement `WebSocketMessageBrokerConfigurer.configureClientInboundChannel()` 
- Register the new `WebSocketAuthInterceptor` on the inbound channel
- This ensures every incoming STOMP message goes through JWT authentication

---

### Frontend (Dashboard WebSocket Client)

#### [MODIFY] [dashboard.html](file:///c:/Users/Adarsh/OneDrive/Documents/Projects/paytm-wallet-clone/src/main/resources/static/pages/dashboard.html)

Update `connectWebSocket()` to pass the JWT token in STOMP CONNECT headers:

```javascript
// Before (broken):
stompClient.connect({}, function(frame) { ... });

// After (fixed):
stompClient.connect(
    { 'Authorization': 'Bearer ' + Auth.getToken() },
    function(frame) { ... }
);
```

## Verification Plan

### Manual Verification

> [!IMPORTANT]
> Since the project uses Spring Boot with SockJS/STOMP, the best way to verify is end-to-end testing in the browser.

1. Start the Spring Boot app: `mvnw spring-boot:run` from the project root
2. Open **two separate browser windows** (or incognito for the second)
3. Register/login as **User A** in window 1 → go to Dashboard
4. Register/login as **User B** in window 2 → go to Dashboard
5. Check the WebSocket status indicator in both dashboards — should show **"Live"** (green dot)
6. From **User A**, go to Send Money → transfer ₹100 to **User B**'s phone
7. **Verify in real-time:**
   - User A's dashboard balance should update automatically without page refresh
   - User B's dashboard should show a notification popup ("💰 You received ₹100.00 from User A")
   - User B's balance should update in real-time
8. Check browser console logs for `✅ WebSocket Connected` and `💰 Balance Update:` messages
