# Part 5: API Documentation

## 14. Complete API Reference

### Base URL
```
http://localhost:8080/api
```

### Authentication
All protected endpoints require JWT token in header:
```
Authorization: Bearer <token>
```

---

## Authentication Endpoints

### 1. Register User
**POST** `/auth/register`

**Description:** Create a new user account. Automatically creates a wallet with ₹0 balance.

**Request:**
```json
{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "9999999999",
    "password": "SecurePass@123"
}
```

**Validation Rules:**
- `name`: Not blank, max 100 characters
- `email`: Valid email format, unique
- `phone`: 10 digits starting with 6-9, unique
- `password`: Minimum 8 characters

**Success Response (201 Created):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "email": "john@example.com",
    "userId": 1,
    "name": "John Doe"
}
```

**Error Responses:**

400 Bad Request:
```json
{
    "timestamp": "2024-02-24T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Email already registered: john@example.com"
}
```

---

### 2. Login
**POST** `/auth/login`

**Description:** Authenticate user and receive JWT token.

**Request:**
```json
{
    "email": "john@example.com",
    "password": "SecurePass@123"
}
```

**Success Response (200 OK):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "email": "john@example.com",
    "userId": 1,
    "name": "John Doe"
}
```

**Error Responses:**

401 Unauthorized:
```json
{
    "timestamp": "2024-02-24T10:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password"
}
```

---

### 3. Get Profile
**GET** `/auth/profile`  
🔒 **Requires Authentication**

**Description:** Get authenticated user's profile information.

**Request Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "9999999999",
    "memberSince": "2024-02-20T10:30:00",
    "isActive": true
}
```

---

### 4. Change Password
**POST** `/auth/change-password`  
🔒 **Requires Authentication**

**Description:** Change user's password. Requires current password verification.

**Request:**
```json
{
    "currentPassword": "OldPass@123",
    "newPassword": "NewPass@456",
    "confirmPassword": "NewPass@456"
}
```

**Success Response (200 OK):**
```json
{
    "message": "Password changed successfully"
}
```

**Error Responses:**

400 Bad Request (wrong current password):
```json
{
    "message": "Current password is incorrect"
}
```

400 Bad Request (passwords don't match):
```json
{
    "message": "New passwords do not match"
}
```

---

### 5. Forgot Password
**POST** `/auth/forgot-password`

**Description:** Generate password reset token. Verifies email + phone match.

**Request:**
```json
{
    "email": "john@example.com",
    "phone": "9999999999"
}
```

**Success Response (200 OK):**
```json
{
    "message": "Password reset token generated",
    "token": "8c4f5e89-3b2a-4d1e-9c8b-7a6f5e4d3c2b"
}
```

⚠️ **Note:** In production, token should be sent via email, not returned in response.

**Error Response:**

404 Not Found:
```json
{
    "message": "Email or phone number not found"
}
```

---

### 6. Reset Password
**POST** `/auth/reset-password`

**Description:** Reset password using token from forgot-password.

**Request:**
```json
{
    "token": "8c4f5e89-3b2a-4d1e-9c8b-7a6f5e4d3c2b",
    "newPassword": "NewSecure@789"
}
```

**Success Response (200 OK):**
```json
{
    "message": "Password reset successful"
}
```

**Error Responses:**

400 Bad Request (invalid/expired token):
```json
{
    "message": "Invalid or expired reset token"
}
```

400 Bad Request (token already used):
```json
{
    "message": "This reset token has already been used"
}
```

---

## Wallet Endpoints

### 7. Get Wallet Balance
**GET** `/wallet/balance`  
🔒 **Requires Authentication**

**Description:** Get authenticated user's wallet balance.

**Success Response (200 OK):**
```json
{
    "balance": 9594.50,
    "currency": "INR",
    "lastUpdated": "2024-02-24T15:30:00"
}
```

---

### 8. Lookup User by Phone
**GET** `/wallet/user/{phone}`  
🔒 **Requires Authentication**

**Description:** Look up user details by phone number. Used for receiver name preview before transfer.

**Path Parameter:**
- `phone`: 10-digit phone number

**Example:**
```
GET /wallet/user/9999999999
```

**Success Response (200 OK) - User Found:**
```json
{
    "name": "Alice Kumar",
    "phone": "9999999999",
    "exists": true
}
```

**Success Response (200 OK) - User Not Found:**
```json
{
    "name": null,
    "phone": null,
    "exists": false
}
```

---

### 9. Transfer Money (P2P)
**POST** `/wallet/transfer`  
🔒 **Requires Authentication**

**Description:** Transfer money from authenticated user to another user via phone number.

**Request:**
```json
{
    "recipientPhone": "9999999999",
    "amount": 100.00,
    "note": "Lunch split"
}
```

**Validation Rules:**
- `recipientPhone`: 10 digits, cannot be sender's phone
- `amount`: Positive number, max 2 decimal places
- `note`: Optional, max 500 characters

**Success Response (200 OK):**
```json
{
    "txnRef": "550e8400-e29b-41d4-a716-446655440000",
    "senderName": "John Doe",
    "recipientName": "Alice Kumar",
    "recipientPhone": "9999999999",
    "amount": 100.00,
    "status": "SUCCESS",
    "note": "Lunch split",
    "newBalance": 9494.50
}
```

**Error Responses:**

400 Bad Request (insufficient funds):
```json
{
    "message": "Insufficient funds. Available: ₹50.00"
}
```

400 Bad Request (self-transfer):
```json
{
    "message": "Cannot transfer to yourself"
}
```

404 Not Found (recipient doesn't exist):
```json
{
    "message": "Recipient not found with phone: 9999999999"
}
```

---

### 10. Get Transaction History
**GET** `/wallet/transactions?page=0&size=10`  
🔒 **Requires Authentication**

**Description:** Get paginated transaction history for authenticated user.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Items per page (default: 10, max: 50)

**Success Response (200 OK):**
```json
{
    "content": [
        {
            "txnRef": "550e8400-e29b-41d4-a716-446655440000",
            "amount": 100.00,
            "type": "TRANSFER",
            "direction": "SENT",
            "status": "SUCCESS",
            "description": "Transfer: Lunch split",
            "counterpartyName": "Alice Kumar",
            "counterpartyPhone": "9999999999",
            "createdAt": "2024-02-24T15:30:00"
        },
        {
            "txnRef": "660f9511-f39c-52e5-b827-557766551111",
            "amount": 500.00,
            "type": "CREDIT",
            "direction": "RECEIVED",
            "status": "SUCCESS",
            "description": "Razorpay payment: pay_ABC123",
            "counterpartyName": null,
            "counterpartyPhone": null,
            "createdAt": "2024-02-24T14:00:00"
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 10
    },
    "totalPages": 5,
    "totalElements": 45,
    "last": false,
    "first": true
}
```

**Transaction Types:**
- `TRANSFER`: P2P money transfer
- `CREDIT`: Money added (Razorpay payment)
- `DEBIT`: Money withdrawn (future feature)

**Direction:**
- `SENT`: Money sent by user
- `RECEIVED`: Money received by user

---

### 11. Get Single Transaction
**GET** `/wallet/transactions/{txnRef}`  
🔒 **Requires Authentication**

**Description:** Get details of a specific transaction by reference number.

**Path Parameter:**
- `txnRef`: Transaction reference (UUID)

**Example:**
```
GET /wallet/transactions/550e8400-e29b-41d4-a716-446655440000
```

**Success Response (200 OK):**
```json
{
    "txnRef": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 100.00,
    "type": "TRANSFER",
    "direction": "SENT",
    "status": "SUCCESS",
    "description": "Transfer: Lunch split",
    "counterpartyName": "Alice Kumar",
    "counterpartyPhone": "9999999999",
    "createdAt": "2024-02-24T15:30:00"
}
```

**Error Response:**

404 Not Found:
```json
{
    "message": "Transaction not found or unauthorized"
}
```

---

### 12. Download Transaction Receipt (PDF)
**GET** `/wallet/receipt/{txnRef}`  
🔒 **Requires Authentication**

**Description:** Download PDF receipt for a transaction.

**Path Parameter:**
- `txnRef`: Transaction reference (UUID)

**Success Response (200 OK):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="receipt_{txnRef}.pdf"`
- Body: PDF binary data

**PDF Contains:**
- PayFlow branding
- Transaction ID
- Date & Time
- Type & Status
- From (name + phone)
- To (name + phone)
- Amount (highlighted)
- Description
- Footer with support info

**Error Response:**

404 Not Found:
```json
{
    "message": "Transaction not found or unauthorized"
}
```

---

## Payment Endpoints

### 13. Create Razorpay Order
**POST** `/payment/create-order`  
🔒 **Requires Authentication**

**Description:** Create Razorpay payment order for adding money to wallet.

**Request:**
```json
{
    "amount": 500.00
}
```

**Validation:**
- `amount`: Positive number, min ₹1, max ₹1,000,000

**Success Response (200 OK):**
```json
{
    "razorpayOrderId": "order_ABC123XYZ",
    "amount": 500.00,
    "currency": "INR",
    "receipt": "receipt_1_1708770600000",
    "status": "CREATED",
    "keyId": "rzp_test_1234567890"
}
```

**Frontend Flow:**
1. Create order (this endpoint)
2. Open Razorpay checkout modal with `razorpayOrderId`
3. User completes payment
4. Razorpay sends webhook to backend
5. Backend credits wallet
6. WebSocket notification sent to user

---

## Webhook Endpoints

### 14. Razorpay Payment Webhook
**POST** `/webhook/razorpay`  
🔓 **Public** (Verified by signature)

**Description:** Receive payment confirmation from Razorpay. Automatically credits wallet.

**Request Headers:**
```
X-Razorpay-Signature: sha256_hash
Content-Type: application/json
```

**Request Body:**
```json
{
    "entity": "event",
    "event": "payment.captured",
    "payload": {
        "payment": {
            "entity": {
                "id": "pay_ABC123",
                "order_id": "order_XYZ789",
                "amount": 50000,
                "currency": "INR",
                "status": "captured"
            }
        }
    }
}
```

**Success Response (200 OK):**
```json
{
    "status": "OK"
}
```

**Security:**
- Verifies HMAC-SHA256 signature
- Checks idempotency (prevents double credit)
- Validates order exists
- Ensures payment not already processed

---

## WebSocket Connection

### 15. WebSocket Endpoint
**CONNECT** `/ws`  
🔓 **Public** (Authenticated via STOMP headers)

**Description:** Establish WebSocket connection for real-time notifications.

**Connection (JavaScript):**
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
    Authorization: 'Bearer ' + token
}, function(frame) {
    console.log('Connected:', frame);
    
    // Subscribe to user-specific queue
    stompClient.subscribe('/user/' + email + '/queue/balance', function(message) {
        const notification = JSON.parse(message.body);
        console.log('Notification:', notification);
    });
});
```

**Notification Message Format:**
```json
{
    "type": "TRANSFER_RECEIVED",
    "newBalance": 10100.00,
    "amount": 100.00,
    "fromUser": "John Doe",
    "message": "💰 You received ₹100.00 from John Doe",
    "timestamp": "2024-02-24T15:30:00"
}
```

**Notification Types:**
- `TRANSFER_RECEIVED`: Money received from another user
- `DEBIT`: Money sent to another user
- `CREDIT`: Money added via Razorpay

---

## Error Response Format

All API errors follow this format:

```json
{
    "timestamp": "2024-02-24T10:30:00.123Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Detailed error message",
    "path": "/api/wallet/transfer"
}
```

### Common HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Success |
| 201 | Created | Registration success |
| 400 | Bad Request | Validation error |
| 401 | Unauthorized | Missing/invalid JWT |
| 403 | Forbidden | JWT valid but insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate email/phone |
| 500 | Internal Error | Server error |

---

## Rate Limiting (Planned)

**Recommended Limits:**
- Login: 5 attempts per minute per IP
- Register: 3 attempts per hour per IP
- Transfer: 10 per minute per user
- Password reset: 3 per hour per email

**Implementation (Future):**
```java
@RateLimiter(name = "transfer")
public TransferResponse transfer(...) {
    // Max 10 per minute
}
```

---

## API Testing Examples

### cURL Examples

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "9999999999",
    "password": "Secure@123"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Secure@123"
  }'
```

**Get Balance:**
```bash
curl -X GET http://localhost:8080/api/wallet/balance \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

**Transfer:**
```bash
curl -X POST http://localhost:8080/api/wallet/transfer \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{
    "recipientPhone": "9999999999",
    "amount": 100,
    "note": "Test transfer"
  }'
```

**Get Transactions:**
```bash
curl -X GET "http://localhost:8080/api/wallet/transactions?page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

---

## Postman Collection

**Import this JSON into Postman:**

```json
{
  "info": {
    "name": "PayFlow Wallet API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/auth/register",
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"Test User\",\n  \"email\": \"test@example.com\",\n  \"phone\": \"9999999999\",\n  \"password\": \"Test@123\"\n}"
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api"
    },
    {
      "key": "token",
      "value": ""
    }
  ]
}
```

---

## Security Headers

**Required Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Optional Headers:**
```
X-Request-ID: unique-id-for-tracing
User-Agent: PayFlow-Mobile/1.0
```

---

## Interview Questions on API

**Q: How do you handle API versioning?**
A: "Currently not versioned, but I would add /v1/ prefix: `/api/v1/wallet/balance`. New versions can introduce breaking changes while v1 remains stable. Clients specify version in URL."

**Q: How do you prevent replay attacks?**
A: "JWT has expiration (24 hours). For critical operations like transfers, I'd add a nonce field and track used nonces in Redis with expiration."

**Q: How do you handle concurrent requests?**
A: "Idempotency for payments (check razorpay_payment_id). For transfers, pessimistic locking ensures consistency. Could add idempotency keys for all write operations."

**Q: Why REST over GraphQL?**
A: "REST is simpler for this use case. Fixed endpoints, caching is easier, no over-fetching issues with small payloads. GraphQL would be overkill here but useful for complex data relationships."
