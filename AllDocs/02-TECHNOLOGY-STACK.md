# Part 1: Project Overview (Continued)

## 2. Technology Stack - Deep Dive

### Why These Technologies Were Chosen

Every technology in PayFlow Wallet was chosen for a specific reason. Let's break down each one:

---

## Backend Technologies

### 1. **Java 17** 
**What it is:** Programming language  
**Why chosen:**
- ✅ **Type safety:** Catches errors at compile time
- ✅ **Mature ecosystem:** Lots of libraries & frameworks
- ✅ **Enterprise standard:** Used in most fintech companies
- ✅ **Performance:** Fast execution, optimized JVM
- ✅ **Multithreading:** Built-in concurrency support

**Interview Answer:**
"I chose Java 17 because it's the industry standard for financial applications. It provides strong type safety which is crucial for handling money, and has excellent support for concurrent operations which I needed for handling simultaneous wallet transactions."

---

### 2. **Spring Boot 3.2.15**
**What it is:** Application framework  
**Why chosen:**
- ✅ **Rapid development:** Auto-configuration saves time
- ✅ **Production-ready:** Built-in health checks, metrics
- ✅ **Dependency Injection:** Loose coupling, testable code
- ✅ **Embedded server:** No need for external Tomcat
- ✅ **Spring ecosystem:** Security, Data, WebSocket all integrate perfectly

**What it does in PayFlow:**
- Handles HTTP requests/responses
- Manages application lifecycle
- Provides dependency injection
- Embeds Tomcat server
- Configures database connections

**Interview Answer:**
"Spring Boot allowed me to focus on business logic rather than configuration. Its auto-configuration and starter dependencies made it easy to integrate Spring Security for authentication, Spring Data JPA for database operations, and Spring WebSocket for real-time features."

**Alternative Considered:** Node.js + Express  
**Why Spring Boot Won:** Better type safety, stronger security features, easier transaction management

---

### 3. **Spring Security 6.5.7**
**What it is:** Authentication & Authorization framework  
**Why chosen:**
- ✅ **Industry standard:** Battle-tested in production
- ✅ **JWT support:** Modern stateless authentication
- ✅ **Filter chain:** Flexible request processing
- ✅ **CORS support:** Handles cross-origin requests
- ✅ **Password encryption:** BCrypt built-in

**What it does in PayFlow:**
- Validates JWT tokens on every request
- Encrypts passwords with BCrypt
- Configures CORS for frontend
- Protects API endpoints
- Handles authentication failures

**Interview Answer:**
"Security was my top priority for a financial application. Spring Security provided production-ready features like JWT validation, password hashing with BCrypt, and a configurable filter chain that processes every request before it reaches my controllers."

---

### 4. **Spring Data JPA (Hibernate)**
**What it is:** Object-Relational Mapping (ORM)  
**Why chosen:**
- ✅ **Less boilerplate:** No manual SQL for CRUD
- ✅ **Type-safe queries:** Compile-time query validation
- ✅ **Relationship mapping:** Easy @OneToMany, @ManyToOne
- ✅ **Transaction management:** @Transactional annotation
- ✅ **Lazy loading:** Performance optimization

**What it does in PayFlow:**
- Maps Java classes to database tables
- Generates SQL queries automatically
- Manages database transactions
- Handles entity relationships
- Provides pessimistic locking for concurrency

**Interview Answer:**
"JPA eliminated the need to write repetitive SQL queries and allowed me to work with Java objects instead of database rows. This made the code more maintainable and less error-prone. For example, finding a user by email is just `userRepository.findByEmail(email)`."

**Alternative Considered:** MyBatis (raw SQL)  
**Why JPA Won:** Less code, better maintainability, built-in transaction support

---

### 5. **JJWT 0.11.5**
**What it is:** JSON Web Token library  
**Why chosen:**
- ✅ **Stateless auth:** No session storage needed
- ✅ **Secure:** HMAC-SHA256 signing
- ✅ **Standard:** JWT is an industry standard
- ✅ **Scalable:** Works across multiple servers
- ✅ **Easy to use:** Simple API

**What it does in PayFlow:**
- Generates JWT tokens on login
- Signs tokens with secret key
- Validates token signatures
- Extracts user info from tokens
- Checks token expiration

**JWT Structure in PayFlow:**
```
Header: { "alg": "HS256", "typ": "JWT" }
Payload: { "userId": 1, "email": "user@mail.com", "exp": 1234567890 }
Signature: HMACSHA256(header + payload, SECRET_KEY)
```

**Interview Answer:**
"I chose JWT for stateless authentication because it allows the application to scale horizontally - any server can validate the token without checking a central session store. The token contains the user's identity, so there's no database hit on every request."

---

### 6. **MySQL 8.0**
**What it is:** Relational database  
**Why chosen:**
- ✅ **ACID transactions:** Critical for financial data
- ✅ **Foreign keys:** Enforces data integrity
- ✅ **Mature & reliable:** Battle-tested
- ✅ **Good performance:** Indexed queries are fast
- ✅ **Wide adoption:** Easy to find support

**What it does in PayFlow:**
- Stores users, wallets, transactions
- Enforces foreign key relationships
- Provides transaction isolation
- Supports pessimistic locking
- Handles concurrent updates

**Interview Answer:**
"For a financial application, I needed ACID guarantees - especially atomicity for transfers. MySQL's transaction support ensured that either both wallet updates happen or neither does, preventing money from being lost or duplicated."

**Alternative Considered:** PostgreSQL, MongoDB  
**Why MySQL Won:** More familiar, lighter weight, sufficient for project needs

---

### 7. **Flyway 9.x**
**What it is:** Database migration tool  
**Why chosen:**
- ✅ **Version control for DB:** Track schema changes
- ✅ **Reproducible:** Same schema everywhere
- ✅ **Safe migrations:** Validates before running
- ✅ **Team-friendly:** Everyone has same DB structure
- ✅ **Rollback support:** Can undo changes

**What it does in PayFlow:**
- Creates tables on first run
- Applies schema changes in order
- Tracks which migrations ran
- Prevents duplicate migrations
- Validates migration checksums

**Migration Example:**
```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    ...
);
```

**Interview Answer:**
"Flyway ensures everyone on the team has the same database structure. When someone creates a new feature requiring a table change, they write a migration script, and Flyway automatically applies it to everyone's database - including production."

---

### 8. **WebSocket + STOMP**
**What it is:** Real-time bidirectional communication  
**Why chosen:**
- ✅ **Real-time:** No polling needed
- ✅ **Bidirectional:** Server can push to client
- ✅ **Efficient:** Single TCP connection
- ✅ **Standard:** STOMP is well-supported
- ✅ **Spring integration:** Works seamlessly with Spring

**What it does in PayFlow:**
- Keeps persistent connection to browser
- Pushes notifications when money received
- Updates balance in real-time
- Sends to specific users only
- Authenticates via JWT in STOMP frames

**Interview Answer:**
"I implemented WebSocket with STOMP to give users instant notifications when they receive money, similar to how WhatsApp delivers messages. This creates a much better user experience than having to refresh the page to see updates."

**Alternative Considered:** Server-Sent Events (SSE)  
**Why WebSocket Won:** Bidirectional communication, better browser support

---

### 9. **iText7**
**What it is:** PDF generation library  
**Why chosen:**
- ✅ **Professional PDFs:** High-quality output
- ✅ **Flexible:** Full control over layout
- ✅ **Tables & styling:** Easy to format
- ✅ **Industry standard:** Widely used
- ✅ **Active development:** Regular updates

**What it does in PayFlow:**
- Generates transaction receipts
- Creates formatted PDF with tables
- Adds branding (PayFlow logo)
- Includes transaction details
- Returns downloadable file

**Interview Answer:**
"For regulatory compliance and user convenience, I added PDF receipt generation using iText7. Users can download professional receipts for any transaction, which is important for record-keeping and tax purposes."

---

### 10. **SLF4J + Logback**
**What it is:** Logging framework  
**Why chosen:**
- ✅ **Production debugging:** Track issues
- ✅ **Performance monitoring:** Log slow operations
- ✅ **Audit trail:** Who did what when
- ✅ **Flexible:** Can change log levels
- ✅ **Standard:** Industry best practice

**What it does in PayFlow:**
- Logs every transfer
- Records authentication attempts
- Tracks payment webhooks
- Logs errors with stack traces
- Can filter by log level (INFO, ERROR, DEBUG)

**Log Example:**
```
INFO - Transfer initiated: user1@mail → user2@mail | Amount: ₹100
INFO - ✅ Transfer completed | TxnRef: 123-456-789
INFO - 🔔 WebSocket notification sent to user2@mail
```

---

## Frontend Technologies

### 1. **Vanilla JavaScript (ES6+)**
**Why chosen:**
- ✅ **No framework overhead:** Faster load times
- ✅ **Learn fundamentals:** Better understanding
- ✅ **No build step:** Simple deployment
- ✅ **Full control:** No framework abstractions
- ✅ **Interview friendly:** Shows JS knowledge

**What it does:**
- Handles form submissions
- Makes API calls with fetch()
- Stores JWT in localStorage
- Validates inputs client-side
- Manages WebSocket connections

**Interview Answer:**
"I chose vanilla JavaScript to demonstrate my understanding of core JavaScript concepts without relying on frameworks. This also kept the frontend lightweight with no build step required."

---

### 2. **HTML5 & CSS3**
**Why chosen:**
- ✅ **Semantic HTML:** Better structure
- ✅ **Modern CSS:** Flexbox, Grid
- ✅ **Responsive:** Works on mobile
- ✅ **No dependencies:** Pure CSS
- ✅ **Custom design:** Unique UI

**Features Used:**
- Flexbox for layouts
- CSS Grid for cards
- CSS animations for notifications
- Media queries for responsive design
- CSS variables for theming

---

### 3. **SockJS + Stomp.js**
**Why chosen:**
- ✅ **WebSocket client:** Browser support
- ✅ **Fallback:** Works even if WebSocket blocked
- ✅ **STOMP protocol:** Message framing
- ✅ **CDN available:** Easy to include
- ✅ **Spring compatible:** Works with Spring WebSocket

**What it does:**
- Connects to backend WebSocket
- Subscribes to user-specific queues
- Receives real-time notifications
- Handles reconnection automatically

---

### 4. **Razorpay Checkout SDK**
**Why chosen:**
- ✅ **Trusted gateway:** RBI approved
- ✅ **Easy integration:** Drop-in SDK
- ✅ **Multiple payment options:** UPI, cards, netbanking
- ✅ **Webhook support:** Server-side verification
- ✅ **Test mode:** Can test without real money

**What it does:**
- Opens payment modal
- Handles payment processing
- Returns payment status
- Securely collects card details
- Sends webhook to backend

---

## External APIs & Services

### **Razorpay Payment Gateway**
**What it provides:**
- Payment processing
- Order creation API
- Payment verification webhook
- Test mode for development
- Dashboard for monitoring

**Security Features:**
- HMAC-SHA256 signature verification
- PCI-DSS compliant
- Encrypted communication
- Webhook secret validation

---

## Development Tools

### 1. **Maven**
- Dependency management
- Build automation
- Project structure
- Plugin management

### 2. **Git**
- Version control
- Feature branches
- Commit history
- Collaboration

### 3. **IntelliJ IDEA / VS Code**
- Code editing
- Debugging
- Refactoring
- Git integration

---

## Tech Stack Summary Table

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 17 | Backend logic |
| **Framework** | Spring Boot | 3.2.15 | Application framework |
| **Security** | Spring Security | 6.5.7 | Auth & authorization |
| **ORM** | Spring Data JPA | 3.2.x | Database operations |
| **Auth** | JJWT | 0.11.5 | JWT generation & validation |
| **Database** | MySQL | 8.0 | Data persistence |
| **Migration** | Flyway | 9.x | Database versioning |
| **WebSocket** | Spring WebSocket | 6.x | Real-time communication |
| **PDF** | iText7 | 7.2.5 | Receipt generation |
| **Logging** | SLF4J + Logback | 2.0.x | Application logging |
| **Build** | Maven | 3.9.x | Dependency management |
| **Frontend** | Vanilla JS | ES6+ | User interface |
| **Payment** | Razorpay | 2.x | Payment gateway |

---

## Why NOT These Alternatives?

### **Why NOT React/Angular/Vue?**
- ❌ Adds complexity
- ❌ Requires build step
- ❌ Larger bundle size
- ✅ Vanilla JS shows fundamental knowledge

### **Why NOT MongoDB?**
- ❌ No transactions (in older versions)
- ❌ Eventual consistency risks
- ❌ Harder to enforce relationships
- ✅ MySQL provides ACID guarantees

### **Why NOT Microservices?**
- ❌ Overkill for this scale
- ❌ Adds deployment complexity
- ❌ Network latency between services
- ✅ Monolith is simpler and sufficient

### **Why NOT GraphQL?**
- ❌ More complex setup
- ❌ REST is simpler
- ❌ Harder to cache
- ✅ REST meets all requirements

---

## Interview Tip: Discussing Tech Stack

**When asked: "Why did you choose these technologies?"**

**Structure your answer:**
1. **Start with the problem:** "For a financial application, I needed..."
2. **Explain requirements:** "ACID transactions, security, real-time updates..."
3. **Match tech to requirements:** "Spring Boot provided..., MySQL ensured..., WebSocket enabled..."
4. **Mention alternatives:** "I considered MongoDB but chose MySQL because..."
5. **Show learning:** "This project taught me..."

**Example:**
"I chose Spring Boot because it's the industry standard for financial applications and provides robust security features through Spring Security. MySQL was essential for ACID transactions - I couldn't risk losing or duplicating money. For real-time notifications, WebSocket was the right choice over polling because it's more efficient and provides instant updates."

---

## Next: System Architecture Deep Dive →

Now that we understand WHAT technologies were used and WHY, let's explore HOW they all work together in the architecture.
