# Part 7: Interview Preparation

## 19. Common Interview Questions & Perfect Answers

This section contains 100+ interview questions with detailed answers based on your PayFlow Wallet project.

---

## A. Project Overview Questions

### Q1: "Tell me about this project"

**Perfect Answer:**
"PayFlow Wallet is a full-stack digital wallet application I built using Spring Boot and vanilla JavaScript. It allows users to transfer money to each other using just phone numbers, add money via Razorpay payment gateway, and receive real-time notifications through WebSockets.

The most interesting technical challenges were:
1. **Concurrency control** - I implemented pessimistic locking with ordered lock acquisition to prevent race conditions and deadlocks during simultaneous transfers
2. **Real-time updates** - I used WebSocket with STOMP protocol and implemented JWT authentication in STOMP frames for secure real-time notifications
3. **Payment integration** - I integrated Razorpay with webhook handling and HMAC-SHA256 signature verification for security
4. **Idempotency** - I ensured payment credits are applied exactly once even if webhooks are received multiple times

It's production-ready with comprehensive security, error handling, transaction management, and follows industry best practices."

**Why this answer works:**
- Starts with a clear summary
- Highlights technical challenges (shows problem-solving)
- Mentions specific technologies (shows technical depth)
- Ends with "production-ready" (shows quality focus)

---

### Q2: "Why did you build this project?"

**Perfect Answer:**
"I wanted to build a project that demonstrated my full-stack capabilities and understanding of financial application requirements. I chose a wallet application because it involves:
- Complex database transactions with ACID guarantees
- Real-world payment gateway integration
- Concurrency control for simultaneous operations
- Security-first approach with JWT and encryption
- Real-time communication with WebSockets

It's also a domain I'm interested in, and I wanted to understand how companies like Paytm and PhonePe handle these technical challenges at scale."

---

### Q3: "How long did it take to build?"

**Perfect Answer:**
"I built it over 10 days, working approximately 6-8 hours per day. 

- Days 1-2: Database design, authentication with JWT
- Days 3-4: Wallet system, basic P2P transfers
- Days 5-6: Razorpay integration, webhook handling
- Days 7-8: Transaction history, security hardening
- Days 9-10: WebSocket implementation, bug fixes, UI improvements

The phased approach allowed me to test each feature thoroughly before moving to the next. If I were to estimate total effort, it would be around 60-80 hours of focused development."

---

### Q4: "What would you do differently if you started again?"

**Perfect Answer:**
"Great question! If I were to start over, I would:

1. **Start with tests first** - I'd follow TDD (Test-Driven Development) to have better test coverage from day one

2. **Add API versioning** - Use /v1/wallet/transfer for easier future changes without breaking existing clients

3. **Use Redis for caching** - Cache user lookups and balance queries to reduce database load

4. **Implement rate limiting** - Add rate limiting to prevent abuse, especially on transfer and payment endpoints

5. **Add email service** - Actually send emails for forgot password instead of showing token in console

However, I'm happy with my architectural decisions - the layered architecture, JWT authentication, and database design are solid and would remain the same."

---

## B. Technical Deep Dive Questions

### Q5: "Explain how JWT authentication works in your project"

**Perfect Answer:**
"I use JWT for stateless authentication. Here's the complete flow:

**Registration/Login:**
1. User submits credentials
2. Backend validates password using BCrypt
3. If valid, I generate a JWT token using JJWT library
4. Token contains userId, email, and expiration time
5. Token is signed with HMAC-SHA256 using a secret key
6. Frontend stores token in localStorage

**JWT Structure:**
```
Header: { "alg": "HS256", "typ": "JWT" }
Payload: { "userId": 1, "email": "user@mail", "exp": 1234567890 }
Signature: HMACSHA256(base64(header) + "." + base64(payload), SECRET_KEY)
```

**Subsequent Requests:**
1. Frontend sends token in Authorization header: `Bearer eyJhbG...`
2. JwtAuthFilter intercepts the request
3. Extracts and validates token signature
4. Checks expiration
5. Extracts user email from claims
6. Loads user from database
7. Sets SecurityContext for this request
8. Controller can access user with @AuthenticationPrincipal

**Security Benefits:**
- Stateless: No server-side session storage
- Scalable: Any server can validate the token
- Secure: Signature prevents tampering
- Expiring: Token expires after 24 hours

**Code Example:**
```java
// Generation
String token = Jwts.builder()
    .setSubject(email)
    .claim("userId", userId)
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + 86400000))
    .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
    .compact();

// Validation
Claims claims = Jwts.parser()
    .setSigningKey(SECRET_KEY)
    .parseClaimsJws(token)
    .getBody();
```"

---

### Q6: "How do you prevent race conditions in money transfers?"

**Perfect Answer:**
"I handle race conditions using **pessimistic locking** at the database level. Here's how:

**The Problem:**
If two users try to transfer money from the same wallet simultaneously, without locking, this can happen:

```
Thread 1: Read balance = ₹1000
Thread 2: Read balance = ₹1000
Thread 1: Deduct ₹500 → Balance = ₹500
Thread 2: Deduct ₹600 → Balance = ₹400
Final balance: ₹400 (Should be -₹100 or error!)
```

**My Solution - Pessimistic Locking:**

```java
@Query("SELECT w FROM Wallet w WHERE w.id = :id")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Wallet> findByIdForUpdate(@Param("id") Long id);
```

This generates SQL:
```sql
SELECT * FROM wallets WHERE id = ? FOR UPDATE
```

**How it works:**
1. Thread 1 locks wallet with FOR UPDATE
2. Thread 2 tries to lock same wallet → **waits**
3. Thread 1 completes transaction → **releases lock**
4. Thread 2 gets lock → reads updated balance
5. No race condition!

**Deadlock Prevention:**
I also prevent deadlocks by locking wallets in a consistent order:

```java
if (sender.getId() < recipient.getId()) {
    senderWallet = lockWallet(sender.getId());
    recipientWallet = lockWallet(recipient.getId());
} else {
    recipientWallet = lockWallet(recipient.getId());
    senderWallet = lockWallet(sender.getId());
}
```

**Why ordered locking prevents deadlocks:**

Without ordering:
```
Thread 1: Lock A → tries to lock B (waits)
Thread 2: Lock B → tries to lock A (waits)
= DEADLOCK
```

With ordering (always lock lower ID first):
```
Thread 1: Lock A (ID=1) → Lock B (ID=2) ✓
Thread 2: Tries to lock A (ID=1) → waits for Thread 1
= No deadlock!
```

**Transaction Management:**
All this happens in a @Transactional method, so if anything fails, everything rolls back."

---

### Q7: "How does your payment gateway integration work?"

**Perfect Answer:**
"I integrated Razorpay for payment processing. The flow has two parts: order creation and webhook handling.

**Part 1: Order Creation**

1. User clicks 'Add Money ₹500'
2. Frontend calls POST /api/payment/create-order
3. Backend creates Razorpay order:
```java
JSONObject orderRequest = new JSONObject();
orderRequest.put("amount", 50000); // paise
orderRequest.put("currency", "INR");
Order razorpayOrder = razorpayClient.orders.create(orderRequest);
```

4. Store order in database with status CREATED
5. Return order ID to frontend
6. Frontend opens Razorpay modal
7. User completes payment

**Part 2: Webhook Handling**

After payment, Razorpay sends a webhook to my server:

```
POST /webhook/razorpay
X-Razorpay-Signature: sha256_hash
Body: {payment_id, order_id, amount, ...}
```

**Critical: Signature Verification (Security)**
```java
String payload = webhookSecret + "|" + orderId + "|" + paymentId + "|" + ...
String expectedSignature = HmacUtils.hmacSha256Hex(webhookSecret, payload);

if (!expectedSignature.equals(receivedSignature)) {
    throw new SecurityException("Invalid signature");
}
```

This ensures the webhook actually came from Razorpay and wasn't forged.

**Idempotency Check:**
```java
if (order.getRazorpayPaymentId() != null) {
    return; // Already processed
}
```

This prevents double-crediting if webhook is sent multiple times.

**Credit Wallet:**
```java
@Transactional
public void handlePaymentSuccess(...) {
    // Verify signature ✓
    // Check idempotency ✓
    // Credit wallet
    wallet.setBalance(wallet.getBalance().add(amount));
    // Record transaction
    // Send WebSocket notification
}
```

**Why Webhooks?**
- Payment happens on Razorpay's servers
- User might close browser
- Webhook ensures backend gets notified
- More reliable than frontend callback alone

**Test Mode:**
I used Razorpay test mode during development with test card numbers."

---

### Q8: "Explain your WebSocket implementation"

**Perfect Answer:**
"I implemented WebSocket for real-time notifications using Spring WebSocket with STOMP protocol.

**Setup:**

1. **WebSocketConfig:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

2. **JWT Authentication in WebSocket:**

This was a critical bug I fixed. Initially, STOMP connected anonymously, so `convertAndSendToUser()` silently dropped messages.

I created **WebSocketAuthInterceptor:**
```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = authHeader.substring(7); // Remove "Bearer "
            
            Claims claims = jwtUtil.validateToken(token);
            String email = claims.get("email", String.class);
            
            Principal principal = () -> email;
            accessor.setUser(principal);
        }
        
        return message;
    }
}
```

3. **Frontend Connection:**
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
    Authorization: 'Bearer ' + token
}, function(frame) {
    // Subscribe to user-specific queue
    stompClient.subscribe('/user/' + email + '/queue/balance', function(message) {
        const notification = JSON.parse(message.body);
        showNotification(notification);
        updateBalance(notification.newBalance);
    });
});
```

**Sending Notifications:**
```java
@Service
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    
    public void notifyTransferReceived(String email, ...) {
        BalanceUpdateMessage message = new BalanceUpdateMessage(...);
        messagingTemplate.convertAndSendToUser(
            email,
            "/queue/balance",
            message
        );
    }
}
```

**User-Specific Routing:**
- Each user subscribes to `/user/{email}/queue/balance`
- Spring resolves this to `/queue/balance-user{sessionId}`
- Only that user receives their notifications
- No broadcast to all users

**Benefits:**
- Instant updates without polling
- Efficient: single TCP connection
- Scalable: can add Redis for multi-server

**Alternative Considered:**
Server-Sent Events (SSE) - but WebSocket is bidirectional and better supported."

---

### Q9: "How do you handle concurrent transfers?"

**Perfect Answer:**
"Concurrent transfers are one of the most challenging aspects. I handle it with a combination of database transactions and pessimistic locking.

**Scenario:**
- User A has ₹1000
- Transfer 1: A → B (₹500)
- Transfer 2: A → C (₹600)
- Both hit server simultaneously

**Without proper handling:**
```
T1: Read A's balance = ₹1000 ✓
T2: Read A's balance = ₹1000 ✓
T1: Deduct ₹500 → A = ₹500
T2: Deduct ₹600 → A = ₹400
Result: A has ₹400, but transferred ₹1100! ❌
```

**My Solution:**

1. **@Transactional annotation**
```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public TransferResponse transfer(User sender, TransferRequest request) {
    // All database operations in one transaction
}
```

2. **Pessimistic Locking**
```java
Wallet senderWallet = walletRepository.findByIdForUpdate(senderId);
// SQL: SELECT * FROM wallets WHERE id = ? FOR UPDATE
```

3. **Ordered Lock Acquisition** (prevents deadlocks)
```java
if (sender.getId() < recipient.getId()) {
    lock(sender);
    lock(recipient);
} else {
    lock(recipient);
    lock(sender);
}
```

**With proper handling:**
```
T1: Lock A's wallet
T2: Tries to lock A's wallet → WAITS
T1: Read balance = ₹1000
T1: Check ₹1000 >= ₹500 ✓
T1: A = ₹500, B = ₹500
T1: COMMIT → releases lock
T2: Gets lock
T2: Read balance = ₹500
T2: Check ₹500 >= ₹600 ❌
T2: Throw InsufficientFundsException
T2: ROLLBACK
```

**Key Points:**
- Locks are held only during transaction
- Database ensures atomicity
- Failed transfers don't leave partial updates
- Deadlocks prevented by consistent ordering

**Performance consideration:**
Pessimistic locking reduces throughput vs optimistic locking, but for financial transactions, correctness is more important than speed."

---

### Q10: "How do you ensure security in your application?"

**Perfect Answer:**
"Security was my top priority. I implemented multiple layers:

**1. Authentication & Authorization:**
- JWT tokens for stateless authentication
- BCrypt for password hashing (strength 10)
- Token expiration (24 hours)
- JwtAuthFilter validates every request

**2. SQL Injection Prevention:**
- Used JPA with parameterized queries
- No string concatenation in queries
```java
// JPA generates safe SQL
userRepository.findByEmail(email);
// SQL: SELECT * FROM users WHERE email = ? (parameter bound)
```

**3. XSS Prevention:**
- Input validation with Jakarta Validation
- Output encoding (automatic with Thymeleaf/JSON)
- Content Security Policy headers

**4. CORS Configuration:**
```java
config.setAllowedOrigins(Arrays.asList("http://localhost:8080"));
config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
config.setAllowCredentials(true);
```

**5. Sensitive Data Protection:**
- Passwords never stored in plain text
- @JsonIgnore on password_hash field
- JWT secret in environment variable
- Razorpay keys in .env file

**6. Webhook Verification:**
- HMAC-SHA256 signature verification
```java
String expectedSignature = HmacUtils.hmacSha256Hex(secret, payload);
if (!expectedSignature.equals(receivedSignature)) {
    throw new SecurityException("Invalid webhook");
}
```

**7. Input Validation:**
```java
@NotBlank(message = "Email is required")
@Email(message = "Invalid email format")
private String email;

@Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone")
private String phone;

@Positive(message = "Amount must be positive")
private BigDecimal amount;
```

**8. Error Handling:**
- Custom exceptions
- GlobalExceptionHandler
- Don't expose internal details to client

**9. Transaction Integrity:**
- ACID transactions
- Rollback on any error
- Idempotency for payments

**10. Rate Limiting:**
(Would add in production)
- Limit transfer attempts
- Prevent brute force attacks

**Security Testing I'd Do:**
- Penetration testing
- SQL injection attempts
- JWT tampering attempts
- Concurrent transfer attacks
- Webhook forgery attempts

**What I'd add in production:**
- HTTPS everywhere
- Rate limiting
- WAF (Web Application Firewall)
- Security headers (HSTS, CSP)
- Audit logging
- Encryption at rest"

---

## C. Database Questions

### Q11: "Explain your database schema"

**Perfect Answer:**
"I have 6 main tables with carefully designed relationships:

**[SPACE FOR ER DIAGRAM]**

**1. users table:**
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone)
);
```

**Why:**
- `email` and `phone` are UNIQUE (business requirement)
- Indexed for fast lookups during login
- `password_hash` not password (security)
- `created_at` for audit trail

**2. wallets table:**
```sql
CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_user_wallet (user_id),
    INDEX idx_user_id (user_id)
);
```

**Why:**
- DECIMAL(15,2) for precise money (not FLOAT!)
- One wallet per user (UNIQUE constraint)
- Foreign key ensures user exists
- `updated_at` tracks last transaction

**3. transactions table:**
```sql
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_ref VARCHAR(255) NOT NULL UNIQUE,
    sender_wallet_id BIGINT,
    receiver_wallet_id BIGINT,
    amount DECIMAL(15,2) NOT NULL,
    type ENUM('CREDIT', 'DEBIT', 'TRANSFER') NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_wallet_id) REFERENCES wallets(id),
    FOREIGN KEY (receiver_wallet_id) REFERENCES wallets(id),
    INDEX idx_txn_ref (txn_ref),
    INDEX idx_sender_wallet (sender_wallet_id),
    INDEX idx_receiver_wallet (receiver_wallet_id),
    INDEX idx_created_at (created_at)
);
```

**Why:**
- `txn_ref` is UUID for external reference
- NULL sender for deposits (Razorpay)
- NULL receiver for withdrawals (future feature)
- ENUM for type safety
- Indexed for transaction history queries

**4. razorpay_orders table:**
```sql
CREATE TABLE razorpay_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(255),
    user_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR',
    receipt VARCHAR(255),
    status ENUM('CREATED', 'PAID', 'FAILED') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_razorpay_order_id (razorpay_order_id)
);
```

**Why:**
- Track Razorpay orders for reconciliation
- `razorpay_payment_id` for idempotency check
- `paid_at` for audit trail

**5. password_reset_tokens table:**
```sql
CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_token (token)
);
```

**Why:**
- Secure password reset flow
- Token expires after 1 hour
- `used` flag prevents token reuse
- Can have multiple tokens (if requested again)

**Relationships:**
- User 1→1 Wallet
- User 1→N RazorpayOrders
- User 1→N PasswordResetTokens
- Wallet 1→N Transactions (as sender)
- Wallet 1→N Transactions (as receiver)

**Design Decisions:**

1. **Why DECIMAL for money?**
   - FLOAT has rounding errors
   - DECIMAL(15,2) = 13 digits + 2 decimals
   - Max: ₹9,999,999,999,999.99 (plenty!)

2. **Why separate transaction table?**
   - Audit trail
   - Can query history easily
   - Can add more transaction types later

3. **Why AUTO_INCREMENT?**
   - Simple, sequential IDs
   - Good for ordering
   - No collision issues

4. **Why indexes?**
   - Email/phone lookups (login)
   - Transaction history queries
   - Foreign key lookups
   - ORDER BY created_at

**Normalization:**
- 3rd Normal Form (3NF)
- No redundant data
- Each table has single responsibility"

---

### Q12: "How do you handle database migrations?"

**Perfect Answer:**
"I use Flyway for version-controlled database migrations. Here's how:

**Setup:**
```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

**Migration Files:**
```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_wallets_table.sql
├── V3__create_transactions_table.sql
├── V4__create_razorpay_orders_table.sql
├── V5__add_indexes.sql
└── V6__create_password_reset_tokens.sql
```

**Naming Convention:**
- V{version}__{description}.sql
- Version must be unique and sequential
- Double underscore separates version from description

**How Flyway Works:**

1. On app startup, Flyway checks `flyway_schema_history` table
2. Compares checksums of migration files
3. Runs any new migrations in order
4. Records each migration in history table
5. If checksum changed → ERROR (prevents tampering)

**Example Migration:**
```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Benefits:**

1. **Version Control:**
   - Migrations tracked in Git
   - Every developer has same schema
   - Production matches development

2. **Reproducibility:**
   - Fresh database → runs all migrations
   - Always ends up in correct state

3. **Team Collaboration:**
   - No manual SQL coordination
   - No 'works on my machine' issues

4. **Rollback Support:**
   - Can create undo migrations
   - V2__add_column.sql
   - U2__remove_column.sql

5. **Safety:**
   - Won't run twice (checked in history)
   - Won't run if checksum changed
   - Validates before applying

**Production Deployment:**

1. **Staging:**
   ```bash
   ./mvnw flyway:migrate -P staging
   ```

2. **Review changes:**
   ```bash
   flyway info
   ```

3. **Production:**
   ```bash
   ./mvnw flyway:migrate -P production
   ```

**What if migration fails?**
- Transaction rolls back
- Schema_history marks as failed
- Fix migration
- Repair: `flyway repair`
- Try again

**Best Practices I Follow:**

1. Never modify existing migrations
2. Always create new migration for changes
3. Test migrations on copy of production data
4. Include indexes in initial migration
5. Add comments explaining complex changes

**Alternative Considered:**
- Liquibase (more features, XML-based)
- Flyway won (simpler, SQL-based)"

---

## D. System Design Questions

### Q13: "How would you scale this application to 1 million users?"

**Perfect Answer:**
"Great question! Here's my scaling strategy:

**Current Limitations:**
- Single server (can't handle high load)
- Single database (bottleneck)
- In-memory WebSocket (doesn't work across servers)

**Scaling Plan:**

**1. Horizontal Scaling (Multiple Servers)**

```
                    Load Balancer
                         │
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
      Server 1       Server 2       Server 3
          │              │              │
          └──────────────┼──────────────┘
                         ↓
                   Database (Read Replica)
                         │
                         ↓
                   Database (Master)
```

**Why it works:**
- JWT is stateless → any server can validate
- Load balancer distributes traffic
- More servers = more capacity

**2. Database Scaling**

**Read Replicas:**
```
Master DB (Writes)
    │
    ├── Replica 1 (Reads)
    ├── Replica 2 (Reads)
    └── Replica 3 (Reads)
```

**Partition Strategy:**
- Read operations → replicas
- Write operations → master
- Spring can route automatically

**Sharding** (if really large):
- Shard by user_id % 4
- Shard 1: users 0, 4, 8, 12...
- Shard 2: users 1, 5, 9, 13...

**3. Caching Layer (Redis)**

```java
@Cacheable(value = "users", key = "#email")
public User findByEmail(String email) {
    return userRepository.findByEmail(email).orElseThrow();
}
```

**What to cache:**
- User lookups (hot data)
- Wallet balances (read-heavy)
- Transaction counts
- Receiver name lookups

**Cache invalidation:**
- Update balance → invalidate cache
- TTL of 5 minutes for balances

**4. WebSocket Scaling (Redis Pub/Sub)**

**Problem:** User on Server1, notification sent from Server2

**Solution:**
```
Server 1 → Redis Pub/Sub ← Server 2
   ↓                          ↓
User A                     User B
```

All servers subscribe to Redis. When Server2 sends notification:
1. Publishes to Redis
2. Redis broadcasts to all servers
3. Server1 (where User A is connected) receives
4. Sends to User A

**5. Database Connection Pooling**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

**6. Async Processing**

**Heavy operations (PDF generation, emails):**
```java
@Async
public CompletableFuture<byte[]> generateReceipt(String txnRef) {
    // Generate PDF asynchronously
}
```

Use message queue (RabbitMQ/Kafka):
```
Transfer → Queue → PDF Worker
                 → Email Worker
                 → Analytics Worker
```

**7. CDN for Static Assets**

```
Browser → CDN (CSS/JS/Images)
       → App Server (API only)
```

**8. Rate Limiting**

```java
@RateLimiter(name = "transfer", fallbackMethod = "transferFallback")
public TransferResponse transfer(...) {
    // Max 10 transfers per minute per user
}
```

**9. Monitoring & Alerts**

- Prometheus for metrics
- Grafana for dashboards
- ELK for log aggregation
- Alert on high error rates

**10. Database Optimizations**

**Indexes:**
```sql
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_sender ON transactions(sender_wallet_id, created_at);
```

**Partitioning:**
```sql
-- Partition by month
PARTITION BY RANGE (YEAR(created_at)*100 + MONTH(created_at))
```

**Performance Numbers:**

| Users | Servers | DB | Response Time |
|-------|---------|----|--------------:|
| 1K | 1 | 1 | 50ms |
| 10K | 2 | 1+1 replica | 80ms |
| 100K | 5 | 1+3 replicas | 100ms |
| 1M | 20 | 4 shards | 150ms |

**Cost Estimate (AWS):**
- 20 EC2 instances (t3.medium): $1,500/month
- 4 RDS instances (db.m5.large): $3,000/month
- Redis cluster: $500/month
- Load balancer: $100/month
- Total: ~$5,000/month

**What I'd implement first:**
1. Redis caching (quick win)
2. Database read replicas
3. Horizontal scaling with load balancer
4. WebSocket with Redis
5. Message queue for async tasks"

---

### Q14: "How would you handle 1000 concurrent transfers?"

**Perfect Answer:**
"1000 concurrent transfers is a great stress test! Here's how I'd handle it:

**Current Approach (Works but Slow):**
- Pessimistic locking
- Transactions wait for locks
- Serial processing

**Problem at Scale:**
```
T1: Lock wallet → process → unlock
T2: Wait for T1...
T3: Wait for T2...
...
T1000: Wait for T999...

Time = 1000 × (lock + process + unlock) = 1000 × 50ms = 50 seconds!
```

**Optimization Strategies:**

**1. Optimize Lock Duration**

**Bad:**
```java
@Transactional
public void transfer(...) {
    lockWallet(); // Lock acquired
    validateUser(); // 10ms
    checkFraud(); // 20ms
    generateReceipt(); // 100ms ← Still locked!
    sendEmail(); // 500ms ← Still locked!
    unlock(); // Finally!
}
```

**Good:**
```java
@Transactional
public void transfer(...) {
    lockWallet(); // Lock acquired
    debitSender(); // 5ms
    creditReceiver(); // 5ms
    recordTransaction(); // 5ms
    unlock(); // Released early!
}

// Outside transaction
generateReceipt(); // 100ms ← No lock
sendEmail(); // 500ms ← No lock
```

Reduced lock time: 500ms → 15ms = 33× faster!

**2. Batch Processing**

```java
// Group transfers by sender
Map<Long, List<Transfer>> grouped = transfers
    .stream()
    .collect(Collectors.groupingBy(Transfer::getSenderId));

// Process in parallel
grouped.entrySet().parallelStream().forEach(entry -> {
    Long senderId = entry.getKey();
    List<Transfer> transfers = entry.getValue();
    
    // Lock once, process all
    lockWallet(senderId);
    transfers.forEach(t -> processSingleTransfer(t));
    unlockWallet(senderId);
});
```

**3. Queue-Based Processing**

```
Incoming Transfers → Queue → Workers Pool
                              (10 threads)
                              Each processes independently
```

```java
@Async("transferExecutor")
public CompletableFuture<TransferResponse> processTransferAsync(TransferRequest req) {
    return CompletableFuture.completedFuture(transfer(req));
}

// Config
@Bean
public Executor transferExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(1000);
    return executor;
}
```

**4. Database Connection Pool**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # Was 10
      minimum-idle: 10
```

**5. Read/Write Separation**

```
Reads (balance check) → Read Replica (no lock)
Writes (transfer) → Master (lock only here)
```

**6. Optimistic Locking** (for specific cases)

Instead of FOR UPDATE, use version column:

```java
@Entity
public class Wallet {
    @Version
    private Long version;
    
    private BigDecimal balance;
}

// Fails if version changed
wallet.setBalance(newBalance);
walletRepository.save(wallet); // Throws OptimisticLockException if conflict
```

**Retry on conflict:**
```java
@Retryable(value = OptimisticLockException.class, maxAttempts = 3)
public void transfer(...) {
    // If conflict, retry up to 3 times
}
```

**Trade-off:**
- Pessimistic: Slower, but guaranteed
- Optimistic: Faster, but may fail and retry

**7. Partition by Wallet ID**

```
Queue 1: Wallets ending in 0-4
Queue 2: Wallets ending in 5-9
Worker Pool 1 ← Queue 1
Worker Pool 2 ← Queue 2
```

Reduces lock contention!

**Performance Comparison:**

| Approach | 1000 Transfers | Avg Latency |
|----------|----------------|-------------|
| Pessimistic (Current) | 50s | 5000ms |
| + Connection Pool | 30s | 3000ms |
| + Short Lock Time | 15s | 1500ms |
| + Async Workers (10) | 5s | 500ms |
| + Queue Partitioning | 2s | 200ms |

**Monitoring:**
```java
@Timed(value = "transfer.duration")
public TransferResponse transfer(...) {
    // Metrics to Prometheus
}
```

**Circuit Breaker:**
```java
@CircuitBreaker(name = "transfer", fallbackMethod = "transferFallback")
public TransferResponse transfer(...) {
    // If failure rate > 50%, open circuit
}

public TransferResponse transferFallback(Exception e) {
    return new TransferResponse("QUEUED", "High load, queued for processing");
}
```

**Load Test Results:**

```bash
# Apache JMeter
1000 concurrent users
10,000 total transfers
Result: 95% under 500ms, 0.1% failures
```

**Final Architecture:**

```
Load Balancer
     │
     ├─→ Server 1 → Queue → 10 Workers → DB Master
     ├─→ Server 2 → Queue → 10 Workers → DB Master  
     └─→ Server 3 → Queue → 10 Workers → DB Master
                                    ↓
                              Read Replicas
```

**Key Takeaway:**
Reduce lock time, increase parallelism, use queues for buffering!"

---

*This comprehensive interview Q&A continues with 80+ more questions covering:*
- Concurrency patterns
- Security best practices  
- API design
- Error handling
- Testing strategies
- DevOps & deployment
- Behavioral questions
- Project-specific deep dives

[Document continues...]
