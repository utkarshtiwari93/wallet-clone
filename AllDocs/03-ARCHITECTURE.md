# Part 2: Technical Architecture

## 3. System Architecture

### High-Level Architecture Overview

PayFlow Wallet follows a **3-Tier Architecture:**

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│              (Browser - HTML/CSS/JavaScript)               │
│  • Login/Register Forms   • Dashboard   • Transaction UI   │
│  • WebSocket Client       • Razorpay SDK                   │
└───────────────┬─────────────────────────────────────────────┘
                │ HTTP/HTTPS (REST API)
                │ WebSocket (WSS)
                ↓
┌─────────────────────────────────────────────────────────────┐
│                   APPLICATION LAYER                         │
│           (Spring Boot - Business Logic)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Controllers  │  │   Services   │  │  Repositories│    │
│  │ (REST API)   │→ │  (Business)  │→ │     (JPA)    │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Security   │  │   WebSocket  │  │     PDF      │    │
│  │   (JWT)      │  │    (STOMP)   │  │  Generator   │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└───────────────┬─────────────────────────────────────────────┘
                │ JDBC
                ↓
┌─────────────────────────────────────────────────────────────┐
│                     DATA LAYER                              │
│                  (MySQL Database)                           │
│  Tables: users, wallets, transactions, razorpay_orders,    │
│          password_reset_tokens                              │
└─────────────────────────────────────────────────────────────┘
                ↑
                │ Webhook (HTTPS)
┌───────────────┴─────────────────────────────────────────────┐
│               EXTERNAL SERVICES                             │
│              Razorpay Payment Gateway                       │
└─────────────────────────────────────────────────────────────┘
```

**[SPACE FOR DETAILED ARCHITECTURE DIAGRAM SCREENSHOT]**

---

### Component Breakdown

#### **1. Presentation Layer (Frontend)**

**Components:**
- HTML pages (Login, Dashboard, Transfer, History, etc.)
- CSS for styling (responsive design)
- JavaScript for interactivity
- WebSocket client (SockJS + Stomp.js)
- Razorpay Checkout SDK

**Responsibilities:**
- Display UI to user
- Capture user input
- Send API requests
- Receive WebSocket notifications
- Handle Razorpay payment flow

**Technologies:**
- Vanilla JavaScript (ES6+)
- HTML5 & CSS3
- SockJS 1.6.1
- Stomp.js 2.3.3
- Razorpay Checkout v2

---

#### **2. Application Layer (Backend)**

**Package Structure:**
```
com.utkarsh.paytm_wallet_clone
│
├── config/                    # Configuration classes
│   ├── SecurityConfig.java    # Spring Security setup
│   ├── WebSocketConfig.java   # WebSocket configuration
│   └── RazorpayConfig.java    # Razorpay client bean
│
├── controller/                # REST API controllers
│   ├── AuthController.java    # Login, Register, etc.
│   ├── WalletController.java  # Wallet operations
│   └── PaymentController.java # Payment creation
│
├── service/                   # Business logic
│   ├── AuthService.java       # Authentication logic
│   ├── WalletService.java     # Wallet management
│   ├── TransferService.java   # P2P transfers
│   ├── PaymentService.java    # Razorpay integration
│   ├── TransactionService.java # Transaction recording
│   ├── WebSocketService.java  # Real-time notifications
│   └── PdfReceiptService.java # PDF generation
│
├── repository/                # Database access
│   ├── UserRepository.java
│   ├── WalletRepository.java
│   ├── TransactionRepository.java
│   ├── RazorpayOrderRepository.java
│   └── PasswordResetTokenRepository.java
│
├── model/                     # Entity classes
│   ├── User.java
│   ├── Wallet.java
│   ├── Transaction.java
│   ├── RazorpayOrder.java
│   └── PasswordResetToken.java
│
├── dto/                       # Data Transfer Objects
│   ├── request/              # Request DTOs
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── TransferRequest.java
│   │   └── ...
│   ├── response/             # Response DTOs
│   │   ├── AuthResponse.java
│   │   ├── TransferResponse.java
│   │   └── ...
│   └── websocket/            # WebSocket messages
│       └── BalanceUpdateMessage.java
│
├── security/                  # Security components
│   ├── JwtUtil.java          # JWT generation & validation
│   ├── JwtAuthFilter.java    # JWT authentication filter
│   └── WebSocketAuthInterceptor.java # WS auth
│
├── exception/                 # Custom exceptions
│   ├── UserNotFoundException.java
│   ├── InsufficientFundsException.java
│   ├── WalletNotFoundException.java
│   └── GlobalExceptionHandler.java
│
└── webhook/                   # External webhooks
    └── RazorpayWebhookController.java
```

---

### Request Flow Diagrams

#### **A. User Registration Flow**

```
User → Frontend → Controller → Service → Repository → Database
                                  ↓
                            Wallet Created
                                  ↓
                            JWT Generated
                                  ↓
User ← Frontend ← Response ← Service ← Controller
```

**Detailed Steps:**

1. **User fills registration form**
   - Name, Email, Phone, Password

2. **Frontend validates input**
   - Email format
   - Phone format (10 digits)
   - Password strength

3. **POST /api/auth/register**
   - Sends RegisterRequest JSON

4. **AuthController receives request**
   ```java
   @PostMapping("/register")
   public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request)
   ```

5. **AuthService processes**
   ```java
   // Check if email exists
   // Hash password with BCrypt
   // Save user to database
   // Create wallet automatically
   // Generate JWT token
   ```

6. **Database operations**
   ```sql
   INSERT INTO users (name, email, phone, password_hash, ...)
   INSERT INTO wallets (user_id, balance, ...)
   ```

7. **Return JWT token**
   - Token stored in localStorage
   - User redirected to dashboard

**[SPACE FOR REGISTRATION FLOW DIAGRAM]**

---

#### **B. Login Flow**

```
User → Enter credentials → Frontend → POST /api/auth/login
                                          ↓
                                   Verify password
                                          ↓
                                   Generate JWT
                                          ↓
User ← Store token ← JWT ← AuthResponse
```

**Detailed Steps:**

1. **User enters email + password**

2. **Frontend sends POST /api/auth/login**

3. **Spring Security authenticates**
   ```java
   authenticationManager.authenticate(
       new UsernamePasswordAuthenticationToken(email, password)
   )
   ```

4. **BCrypt verifies password**
   - Hashes entered password
   - Compares with stored hash

5. **JWT generated if valid**
   ```java
   String token = jwtUtil.generateToken(userId, email);
   ```

6. **Token returned to frontend**
   ```javascript
   localStorage.setItem('token', response.token);
   ```

7. **All future requests include token**
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
   ```

**[SPACE FOR LOGIN FLOW DIAGRAM]**

---

#### **C. P2P Transfer Flow (Most Complex)**

```
Sender → Transfer Request → Controller → Service
                                           ↓
                                    Validate Funds
                                           ↓
                                    Lock Wallets
                                           ↓
                                    Debit Sender
                                           ↓
                                    Credit Receiver
                                           ↓
                                    Record Transaction
                                           ↓
                                    Send WebSocket ──→ Receiver
                                           ↓
Sender ← Response ← Controller ← Service
```

**Detailed Steps:**

1. **Sender enters transfer details**
   - Recipient phone number
   - Amount
   - Optional note

2. **Frontend looks up receiver name**
   ```javascript
   GET /api/wallet/user/{phone}
   → Shows "Send ₹100 to Bob Kumar?"
   ```

3. **POST /api/wallet/transfer**
   ```json
   {
     "recipientPhone": "9999999999",
     "amount": 100,
     "note": "Lunch split"
   }
   ```

4. **TransferService.transfer() executes**

5. **Validate not self-transfer**
   ```java
   if (sender.getPhone().equals(recipientPhone)) {
       throw new IllegalArgumentException("Cannot transfer to yourself");
   }
   ```

6. **Find recipient**
   ```java
   User recipient = userRepository.findByPhone(recipientPhone)
       .orElseThrow(() -> new UserNotFoundException("..."));
   ```

7. **Lock wallets in order** (prevents deadlock)
   ```java
   if (sender.getId() < recipient.getId()) {
       senderWallet = lockWallet(sender.getId());
       recipientWallet = lockWallet(recipient.getId());
   } else {
       recipientWallet = lockWallet(recipient.getId());
       senderWallet = lockWallet(sender.getId());
   }
   ```
   
   **SQL Generated:**
   ```sql
   SELECT * FROM wallets WHERE id = ? FOR UPDATE
   ```

8. **Check sufficient funds**
   ```java
   if (senderWallet.getBalance().compareTo(amount) < 0) {
       throw new InsufficientFundsException("...");
   }
   ```

9. **Update both wallets** (atomic transaction)
   ```java
   senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
   recipientWallet.setBalance(recipientWallet.getBalance().add(amount));
   walletRepository.save(senderWallet);
   walletRepository.save(recipientWallet);
   ```

10. **Record transaction**
    ```java
    Transaction txn = new Transaction();
    txn.setSenderWallet(senderWallet);
    txn.setReceiverWallet(recipientWallet);
    txn.setAmount(amount);
    txn.setType(TransactionType.TRANSFER);
    // ...
    ```

11. **Send WebSocket notifications**
    ```java
    webSocketService.notifyTransferSent(sender.getEmail(), ...);
    webSocketService.notifyTransferReceived(recipient.getEmail(), ...);
    ```

12. **Commit transaction**
    - If everything succeeds → COMMIT
    - If any error → ROLLBACK

13. **Return response to sender**
    ```json
    {
      "txnRef": "uuid-here",
      "status": "SUCCESS",
      "newBalance": 9900
    }
    ```

**[SPACE FOR TRANSFER FLOW DIAGRAM]**

---

#### **D. Razorpay Payment Flow**

```
User → Add Money → Frontend → POST /api/payment/create-order
                                      ↓
                              Razorpay Order Created
                                      ↓
User ← Order ID ← Response ← Controller
  ↓
Open Razorpay Modal
  ↓
Make Payment (Card/UPI/etc.)
  ↓
Razorpay → Webhook → POST /webhook/razorpay
                          ↓
                   Verify Signature
                          ↓
                   Credit Wallet
                          ↓
                   Send WebSocket → User
```

**Detailed Steps:**

1. **User clicks "Add Money"**
   - Enters amount (e.g., ₹500)

2. **POST /api/payment/create-order**
   ```json
   { "amount": 500 }
   ```

3. **PaymentService creates Razorpay order**
   ```java
   Order razorpayOrder = razorpayClient.orders.create(orderRequest);
   ```

4. **Store order in database**
   ```java
   RazorpayOrder dbOrder = new RazorpayOrder();
   dbOrder.setRazorpayOrderId(razorpayOrderId);
   dbOrder.setAmount(amount);
   dbOrder.setStatus(CREATED);
   // ...
   ```

5. **Return order ID to frontend**

6. **Frontend opens Razorpay modal**
   ```javascript
   const options = {
     key: razorpayKeyId,
     amount: 50000, // paise
     order_id: orderId,
     handler: function(response) {
       // Payment successful
     }
   };
   const rzp = new Razorpay(options);
   rzp.open();
   ```

7. **User completes payment**
   - Enters card details/UPI
   - Payment processed by Razorpay

8. **Razorpay sends webhook**
   ```
   POST /webhook/razorpay
   X-Razorpay-Signature: sha256_hash
   Body: {payment_id, order_id, amount, ...}
   ```

9. **Verify webhook signature**
   ```java
   String expectedSignature = HmacSHA256(webhook_secret, payload);
   if (!expectedSignature.equals(receivedSignature)) {
       throw new SecurityException("Invalid signature");
   }
   ```

10. **Check idempotency** (prevent duplicate credits)
    ```java
    if (order.getRazorpayPaymentId() != null) {
        return; // Already processed
    }
    ```

11. **Credit wallet**
    ```java
    wallet.setBalance(wallet.getBalance().add(amount));
    walletRepository.save(wallet);
    ```

12. **Record transaction**
    ```java
    transactionService.recordCredit(wallet, amount, "Razorpay payment");
    ```

13. **Send WebSocket notification**
    ```java
    webSocketService.notifyPaymentReceived(user.getEmail(), amount, newBalance);
    ```

14. **User sees notification**
    ```
    💰 Payment of ₹500 received!
    Balance: ₹10,500
    ```

**[SPACE FOR RAZORPAY PAYMENT FLOW DIAGRAM]**

---

#### **E. WebSocket Real-Time Notification Flow**

```
User A → Login → Dashboard → Connect WebSocket
                                    ↓
                              Subscribe to /user/{email}/queue/balance
                                    ↓
User B → Transfer ₹100 to User A
                                    ↓
                              TransferService.transfer()
                                    ↓
                              webSocketService.notify(userA)
                                    ↓
                              STOMP Message sent to queue
                                    ↓
User A ← Notification Popup ← WebSocket
```

**Detailed Steps:**

1. **User opens dashboard**

2. **JavaScript connects to WebSocket**
   ```javascript
   const socket = new SockJS('/ws');
   const stompClient = Stomp.over(socket);
   ```

3. **Send JWT in STOMP headers**
   ```javascript
   stompClient.connect({
     Authorization: 'Bearer ' + token
   }, function(frame) {
       // Connected
   });
   ```

4. **Backend authenticates WebSocket**
   ```java
   // WebSocketAuthInterceptor.java
   String token = headers.getFirst("Authorization").substring(7);
   Claims claims = jwtUtil.validateToken(token);
   session.getAttributes().put("email", claims.get("email"));
   ```

5. **Subscribe to user queue**
   ```javascript
   stompClient.subscribe('/user/' + email + '/queue/balance', function(message) {
       const notification = JSON.parse(message.body);
       showNotification(notification);
   });
   ```

6. **Someone sends money to this user**

7. **TransferService calls WebSocketService**
   ```java
   webSocketService.notifyTransferReceived(
       recipientEmail, senderName, amount, newBalance
   );
   ```

8. **WebSocketService sends message**
   ```java
   messagingTemplate.convertAndSendToUser(
       recipientEmail,
       "/queue/balance",
       notification
   );
   ```

9. **Frontend receives message**
   ```javascript
   // Notification object received
   {
     type: "TRANSFER_RECEIVED",
     amount: 100,
     fromUser: "Alice",
     newBalance: 10100,
     message: "💰 You received ₹100 from Alice"
   }
   ```

10. **Display notification popup**
    ```javascript
    showNotification(data);
    updateBalance(data.newBalance);
    playSound();
    ```

**[SPACE FOR WEBSOCKET FLOW DIAGRAM]**

---

### Why This Architecture?

#### **Advantages:**

1. **Separation of Concerns**
   - Controllers handle HTTP
   - Services contain business logic
   - Repositories manage database

2. **Testability**
   - Can mock services in controller tests
   - Can test services without database
   - Integration tests validate full flow

3. **Maintainability**
   - Changes in one layer don't affect others
   - Easy to add new features
   - Clear structure for new developers

4. **Scalability**
   - Stateless (JWT) → can add more servers
   - Database can be replicated
   - WebSocket can use Redis for multi-server

5. **Security**
   - Security layer protects all endpoints
   - JWT validation before business logic
   - SQL injection prevented by JPA

---

### Design Patterns Used

#### **1. Repository Pattern**
**Where:** UserRepository, WalletRepository, etc.

**Why:** Abstracts database operations
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

#### **2. Service Layer Pattern**
**Where:** AuthService, WalletService, TransferService

**Why:** Separates business logic from controllers
```java
@Service
public class TransferService {
    // Business logic here
}
```

#### **3. DTO Pattern**
**Where:** LoginRequest, TransferResponse, etc.

**Why:** Decouples API contract from database entities
```java
public class TransferRequest {
    private String recipientPhone;
    private BigDecimal amount;
    // ...
}
```

#### **4. Dependency Injection**
**Where:** All @Service, @Repository classes

**Why:** Loose coupling, easier testing
```java
@Service
public class TransferService {
    private final UserRepository userRepository; // Injected
    
    public TransferService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

#### **5. Builder Pattern**
**Where:** ResponseEntity, JWT generation

**Why:** Fluent API for object creation
```java
return ResponseEntity
    .ok()
    .header("X-Custom", "value")
    .body(response);
```

---

### Interview Questions on Architecture

**Q: Why did you choose this architecture?**
A: "I chose a 3-tier layered architecture because it provides clear separation of concerns. The presentation layer handles user interaction, the application layer contains business logic, and the data layer manages persistence. This makes the code maintainable, testable, and scalable."

**Q: How does your application scale?**
A: "It's designed to scale horizontally. JWT authentication is stateless, so any server can validate tokens. The database can be replicated for read operations. For WebSocket, I'd add Redis as a message broker to coordinate notifications across servers."

**Q: What happens if the database goes down?**
A: "The application would return 500 errors for all database operations. In production, I'd add connection pooling with retry logic, implement circuit breakers, and have database replicas for failover."

**Q: How do you ensure data consistency?**
A: "I use database transactions with ACID guarantees. For transfers, both wallet updates happen in a single transaction - either both succeed or both rollback. I also use pessimistic locking to prevent race conditions."

---

## Next: Database Design Deep Dive →

Now let's explore how data is structured and why certain design decisions were made.
