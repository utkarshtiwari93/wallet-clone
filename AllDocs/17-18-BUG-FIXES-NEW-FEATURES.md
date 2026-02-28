# Part 6: Bug Fixes & Improvements

## 17. Major Bug Fixes

### Bug Fix 1: WebSocket Real-Time Updates & Notifications ⭐ CRITICAL

**Problem:**
WebSocket was connecting but notifications weren't being delivered to users.

**Root Cause:**
The STOMP client was connecting anonymously. When I called `convertAndSendToUser(email, "/queue/balance", message)`, Spring couldn't map the email to any WebSocket session, so messages were silently dropped.

**Technical Details:**
```java
// Backend was sending to user
messagingTemplate.convertAndSendToUser(
    "user@email.com",  // Target user
    "/queue/balance",   // Destination
    notification        // Message
);

// But WebSocket had no authenticated user!
// Spring couldn't resolve "user@email.com" to any session
// Message dropped silently ❌
```

**Solution - 3 Parts:**

**Part 1: Created WebSocketAuthInterceptor.java**
```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT from STOMP headers
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    // Validate JWT
                    Claims claims = jwtUtil.validateToken(token);
                    String email = claims.get("email", String.class);
                    
                    // Set authenticated user in WebSocket session
                    Principal principal = () -> email;
                    accessor.setUser(principal);
                    
                    log.info("WebSocket authenticated: {}", email);
                } catch (Exception e) {
                    log.error("WebSocket auth failed", e);
                    throw new MessagingException("Invalid token");
                }
            }
        }
        
        return message;
    }
}
```

**Part 2: Updated WebSocketConfig.java**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketAuthInterceptor authInterceptor;

    // Register the interceptor
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
    
    // ... rest of config
}
```

**Part 3: Updated dashboard.html**
```javascript
// Frontend now sends JWT in STOMP headers
stompClient.connect({
    Authorization: 'Bearer ' + localStorage.getItem('token')  // ← Added this!
}, function(frame) {
    console.log('Connected:', frame);
    stompClient.subscribe('/user/' + email + '/queue/balance', function(message) {
        const notification = JSON.parse(message.body);
        showNotification(notification);
    });
});
```

**Why This Fix Works:**

Before:
```
STOMP CONNECT (anonymous) → Server
Server: "I don't know who this is"
convertAndSendToUser("user@mail", ...) → No user found → Dropped ❌
```

After:
```
STOMP CONNECT + JWT → Interceptor validates → Sets Principal(email)
Server: "This is user@mail"
convertAndSendToUser("user@mail", ...) → Maps to session → Delivered ✅
```

**Impact:**
- 🎯 WebSocket notifications now work 100%
- ⚡ Real-time balance updates functional
- 💰 Notification popups appear instantly
- 🔐 Secure - JWT validates each connection

**Interview Answer:**
"I discovered that WebSocket messages weren't being delivered because the STOMP connection was anonymous. Spring's `convertAndSendToUser()` requires an authenticated Principal to route messages. I fixed this by creating a ChannelInterceptor that extracts the JWT from STOMP CONNECT frames, validates it, and sets the user's email as the Principal. This allowed Spring to correctly route user-specific messages."

---

### Bug Fix 2: Logout 500 Error 🔴

**Problem:**
Clicking "Logout" button resulted in HTTP 500 error and user remained logged in.

**Root Cause:**
Relative path resolution issue in nested page structure.

```
Current location: /pages/dashboard.html
Code: window.location.href = 'index.html';
Resolved to: /pages/index.html ❌ (doesn't exist)
Server returns 404 → Frontend shows as 500
```

**File Structure:**
```
static/
├── index.html          ← Login page (root)
└── pages/
    ├── dashboard.html  ← We're here
    ├── transfer.html
    └── history.html
```

**Bad Code:**
```javascript
// In dashboard.html
function logout() {
    localStorage.clear();
    window.location.href = 'index.html';  // ❌ Resolves to /pages/index.html
}
```

**Solution:**
```javascript
// Fixed code
function logout() {
    localStorage.clear();
    window.location.href = '/index.html';  // ✅ Absolute path from root
}
```

**Alternative Solutions Considered:**

1. **Relative with ../**
```javascript
window.location.href = '../index.html';  // Works but fragile
```

2. **Named route**
```javascript
window.location.href = '/';  // Relies on server redirecting / → index.html
```

3. **Absolute path** ✅ (Best)
```javascript
window.location.href = '/index.html';  // Clear, explicit, works everywhere
```

**Why Absolute Path Won:**
- ✅ Works from any nested level
- ✅ Clear intent
- ✅ No path calculation needed
- ✅ Consistent across app

**Testing:**
```
Tested from:
✓ /pages/dashboard.html → Redirects to /index.html ✓
✓ /pages/transfer.html → Redirects to /index.html ✓
✓ /pages/history.html → Redirects to /index.html ✓
```

**Impact:**
- 🎯 Logout now works correctly
- ✅ User redirected to login page
- 🔐 LocalStorage cleared properly
- 💯 No more 500 errors

**Interview Answer:**
"The logout function was using a relative path which resolved incorrectly in the nested page structure. From `/pages/dashboard.html`, `window.location.href = 'index.html'` tried to navigate to `/pages/index.html` which doesn't exist. I fixed it by using an absolute path `/index.html` which always resolves from the root, regardless of the current location."

---

### Bug Fix 3: Forgot Password Reset Token 🔐

**Two Issues Fixed:**

#### **Issue 3A: deleteByUserId() Silently Failing**

**Problem:**
When user requested forgot password multiple times, old tokens weren't being deleted, causing confusion.

**Root Cause:**
Missing `@Modifying` annotation on custom delete query.

**Bad Code:**
```java
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    // ❌ This doesn't work!
    void deleteByUserId(Long userId);
}
```

**What Happened:**
```java
// In AuthService
resetTokenRepository.deleteByUserId(user.getId());  // Silently does nothing!

// User requests reset again
String newToken = generateToken();
resetTokenRepository.save(newToken);

// Now user has 2 tokens: old one (expired) + new one
// User tries old token → "Token expired" error
```

**Fixed Code:**
```java
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    // ✅ Fixed!
    @Modifying          // ← Required for DELETE/UPDATE queries
    @Transactional      // ← Required for @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
```

**Why @Modifying is Required:**

Spring Data JPA behavior:
- Method starts with `find` → SELECT query ✓
- Method starts with `delete` → Should be DELETE...
- **BUT** custom deletes need `@Modifying` annotation!
- Without it → Spring executes SELECT instead of DELETE

**Testing:**
```java
// Before fix
resetTokenRepository.deleteByUserId(1L);
long count = resetTokenRepository.count();  // Count = 1 (not deleted)

// After fix
resetTokenRepository.deleteByUserId(1L);
long count = resetTokenRepository.count();  // Count = 0 (deleted!)
```

---

#### **Issue 3B: Reset Token Hard to Find**

**Problem:**
Reset token was only logged to server console:
```
2026-02-24T10:30:15.123  INFO  --- Token: abc-123-def-456
```

Developers had to:
1. Make forgot password request
2. Switch to server console
3. Scroll through logs
4. Find the token line
5. Copy token
6. Go back to browser
7. Paste token

**Solution:**
Made token impossible to miss with boxed console output!

**Updated AuthService.java:**
```java
@Transactional
public String forgotPassword(ForgotPasswordRequest req) {
    // ... validation code ...
    
    String token = UUID.randomUUID().toString();
    PasswordResetToken resetToken = new PasswordResetToken(user, token);
    resetTokenRepository.save(resetToken);

    // ✨ NEW: Prominent console display
    log.info("");
    log.info("╔════════════════════════════════════════════════════════════╗");
    log.info("║                  🔑 PASSWORD RESET TOKEN 🔑                ║");
    log.info("╠════════════════════════════════════════════════════════════╣");
    log.info("║  Email: {:<46} ║", user.getEmail());
    log.info("║  Token: {:<46} ║", token);
    log.info("║  Expires: 1 hour                                          ║");
    log.info("╚════════════════════════════════════════════════════════════╝");
    log.info("");

    return token;
}
```

**Console Output:**
```
╔════════════════════════════════════════════════════════════╗
║                  🔑 PASSWORD RESET TOKEN 🔑                ║
╠════════════════════════════════════════════════════════════╣
║  Email: user@example.com                                   ║
║  Token: 8c4f5e89-3b2a-4d1e-9c8b-7a6f5e4d3c2b              ║
║  Expires: 1 hour                                           ║
╚════════════════════════════════════════════════════════════╝
```

**Why This Is Better:**
- ✅ Impossible to miss (bordered box)
- ✅ Easy to identify (emoji icons)
- ✅ All info in one place
- ✅ Copy-paste friendly

**Production Note:**
In production, this would be sent via email instead:
```java
emailService.sendPasswordResetEmail(user.getEmail(), token);
```

But for development, console display is perfect!

**Impact:**
- 🎯 Old tokens properly deleted
- 📝 Token easy to find in logs
- ⚡ Faster testing workflow
- 🔐 No token confusion

**Interview Answer:**
"I fixed two issues with forgot password. First, the repository's deleteByUserId() needed @Modifying and @Transactional annotations to actually execute the DELETE query - it was silently failing before. Second, I made the reset token more visible in logs with a bordered console output for easier development testing. In production, this would be sent via email instead."

---

### Bug Fix 4: Unused Import Warning 🧹

**Problem:**
Compiler warning in test file:
```
Warning: Unused import 'static org.mockito.ArgumentMatchers.any'
```

**Location:**
`src/test/java/.../TransferServiceTest.java`

**Bad Code:**
```java
package com.utkarsh.paytm_wallet_clone.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;  // ❌ Imported but never used
import static org.mockito.Mockito.*;

public class TransferServiceTest {
    // ... tests that don't use any()
}
```

**Solution:**
```java
// Simply removed the unused import
import static org.mockito.Mockito.*;

public class TransferServiceTest {
    // Tests work fine without it
}
```

**Why This Matters:**

1. **Clean Code:**
   - Unused imports clutter
   - Harder to read
   - Confuses developers

2. **Build Warnings:**
   - CI/CD pipelines may fail on warnings
   - Professional code has 0 warnings

3. **IDE Performance:**
   - Fewer imports = faster compilation
   - Better autocomplete

4. **Code Reviews:**
   - Reviewers focus on warnings
   - Wastes time

**How to Find:**
```bash
# IntelliJ IDEA
Code → Optimize Imports (Ctrl+Alt+O)

# Eclipse
Source → Organize Imports (Ctrl+Shift+O)

# VS Code
Right-click → Organize Imports
```

**Impact:**
- ✅ 0 compiler warnings
- 📝 Cleaner code
- 🎯 Easier code reviews
- 💯 Professional quality

---

## 18. New Features Added

### Feature 1: Profile Dropdown on Dashboard 👤

**What It Is:**
Clickable emoji avatar in the dashboard header that opens a dropdown showing user info.

**UI Design:**
```
Dashboard Header:
┌────────────────────────────────────────────────┐
│ PayFlow   Search...        [😎 ▼]  Live  Logout│
│                             └─────┘              │
│                               ↓                  │
│                       ┌──────────────┐          │
│                       │ 😎 John Doe  │          │
│                       │ ✉️ john@...  │          │
│                       │ 📱 9999999... │          │
│                       │ ✅ Verified   │          │
│                       └──────────────┘          │
└────────────────────────────────────────────────┘
```

**Features:**
- **Unique emoji per user** (generated from name)
- **User info display:**
  - Full name
  - Email address
  - Phone number
  - "Verified Account" badge
- **Smooth animations**
- **Click outside to close**

**Technical Implementation:**

**Backend - No Changes Needed:**
User data already available in JWT!

**Frontend - dashboard.html:**
```html
<!-- Profile Dropdown -->
<div class="profile-dropdown">
    <button class="profile-button" onclick="toggleProfileDropdown()">
        <span class="profile-emoji" id="profileEmoji">👤</span>
        <span class="dropdown-arrow">▼</span>
    </button>
    
    <div class="profile-menu" id="profileMenu">
        <div class="profile-header">
            <span class="profile-emoji-large" id="profileEmojiLarge">👤</span>
            <div class="profile-name" id="profileName">User</div>
        </div>
        <div class="profile-details">
            <div class="profile-detail">
                <span class="detail-icon">✉️</span>
                <span class="detail-text" id="profileEmail">email@example.com</span>
            </div>
            <div class="profile-detail">
                <span class="detail-icon">📱</span>
                <span class="detail-text" id="profilePhone">9999999999</span>
            </div>
            <div class="profile-badge">
                <span>✅ Verified Account</span>
            </div>
        </div>
    </div>
</div>
```

**JavaScript - Emoji Generation:**
```javascript
function getEmojiForUser(name) {
    // Generate consistent emoji based on name
    const emojis = ['😀', '😎', '🤠', '🧑‍💼', '👨‍💻', '👩‍💻', '🦸', '🧙'];
    const index = name.split('').reduce((sum, char) => sum + char.charCodeAt(0), 0);
    return emojis[index % emojis.length];
}

function loadProfile() {
    const userData = Auth.getUserData();
    const emoji = getEmojiForUser(userData.name);
    
    document.getElementById('profileEmoji').textContent = emoji;
    document.getElementById('profileEmojiLarge').textContent = emoji;
    document.getElementById('profileName').textContent = userData.name;
    document.getElementById('profileEmail').textContent = userData.email;
    document.getElementById('profilePhone').textContent = userData.phone;
}

function toggleProfileDropdown() {
    const menu = document.getElementById('profileMenu');
    menu.classList.toggle('show');
}

// Close when clicking outside
document.addEventListener('click', function(event) {
    const dropdown = document.querySelector('.profile-dropdown');
    if (!dropdown.contains(event.target)) {
        document.getElementById('profileMenu').classList.remove('show');
    }
});
```

**CSS Styling:**
```css
.profile-dropdown {
    position: relative;
}

.profile-button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 25px;
    cursor: pointer;
    transition: all 0.3s;
}

.profile-button:hover {
    background: rgba(255, 255, 255, 0.15);
    transform: translateY(-2px);
}

.profile-menu {
    position: absolute;
    top: 60px;
    right: 0;
    background: rgba(30, 30, 30, 0.98);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    padding: 20px;
    min-width: 280px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
    opacity: 0;
    visibility: hidden;
    transform: translateY(-10px);
    transition: all 0.3s;
}

.profile-menu.show {
    opacity: 1;
    visibility: visible;
    transform: translateY(0);
}

.profile-emoji-large {
    font-size: 48px;
}

.profile-badge {
    background: rgba(76, 175, 80, 0.2);
    color: #4caf50;
    padding: 8px 12px;
    border-radius: 8px;
    text-align: center;
    font-size: 13px;
    font-weight: 600;
}
```

**Why This Feature:**
- **Better UX:** Quick access to profile info
- **Modern Design:** Like Twitter/Facebook dropdowns
- **Visual Identity:** Emoji makes it personal
- **Verified Badge:** Builds trust

**Interview Answer:**
"I added a profile dropdown that displays when clicking the user's avatar emoji. The emoji is generated consistently based on the user's name using a hash function, so the same user always sees the same emoji. The dropdown shows user details and a verified account badge. I implemented it with pure JavaScript and CSS animations for smooth interactions."

---

### Feature 2: Change Password 🔐

**What It Is:**
Full-stack feature allowing users to change their password from the dashboard.

**User Flow:**
```
Dashboard → Click "Change Password" → Modal Opens
→ Enter Current Password
→ Enter New Password  
→ Confirm New Password
→ Submit → Password Changed ✓
```

**Backend Implementation:**

**1. ChangePasswordRequest.java DTO:**
```java
package com.utkarsh.paytm_wallet_clone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    // Getters & Setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
```

**2. AuthService.changePassword():**
```java
@Transactional
public void changePassword(User user, ChangePasswordRequest request) {
    
    log.info("Change password request for user: {}", user.getEmail());
    
    // 1. Verify passwords match
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
        throw new IllegalArgumentException("New passwords do not match");
    }
    
    // 2. Verify current password
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
        log.warn("Change password failed: Invalid current password for {}", user.getEmail());
        throw new IllegalArgumentException("Current password is incorrect");
    }
    
    // 3. Check new password is different
    if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
        throw new IllegalArgumentException("New password must be different from current password");
    }
    
    // 4. Update password
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    
    log.info("✅ Password changed successfully for user: {}", user.getEmail());
}
```

**3. AuthController Endpoint:**
```java
@PostMapping("/change-password")
public ResponseEntity<Map<String, String>> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal User user) {
    
    authService.changePassword(user, request);
    
    return ResponseEntity.ok(Map.of(
        "message", "Password changed successfully"
    ));
}
```

**Frontend Implementation:**

**dashboard.html Modal:**
```html
<!-- Change Password Modal -->
<div id="changePasswordModal" class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h2>🔐 Change Password</h2>
            <button class="modal-close" onclick="closeChangePasswordModal()">✕</button>
        </div>
        
        <form onsubmit="handleChangePassword(event)">
            <div class="form-group">
                <label>Current Password</label>
                <div class="password-wrapper">
                    <input type="password" id="currentPassword" required>
                    <button type="button" class="toggle-password" onclick="togglePassword('currentPassword')">
                        👁️
                    </button>
                </div>
            </div>
            
            <div class="form-group">
                <label>New Password</label>
                <div class="password-wrapper">
                    <input type="password" id="newPassword" minlength="8" required>
                    <button type="button" class="toggle-password" onclick="togglePassword('newPassword')">
                        👁️
                    </button>
                </div>
                <small>At least 8 characters</small>
            </div>
            
            <div class="form-group">
                <label>Confirm New Password</label>
                <div class="password-wrapper">
                    <input type="password" id="confirmPassword" minlength="8" required>
                    <button type="button" class="toggle-password" onclick="togglePassword('confirmPassword')">
                        👁️
                    </button>
                </div>
            </div>
            
            <div class="modal-actions">
                <button type="button" class="btn-secondary" onclick="closeChangePasswordModal()">
                    Cancel
                </button>
                <button type="submit" class="btn-primary">
                    Change Password
                </button>
            </div>
        </form>
    </div>
</div>
```

**JavaScript Handler:**
```javascript
async function handleChangePassword(event) {
    event.preventDefault();
    
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    // Client-side validation
    if (newPassword !== confirmPassword) {
        showAlert('New passwords do not match', 'error');
        return;
    }
    
    if (newPassword.length < 8) {
        showAlert('Password must be at least 8 characters', 'error');
        return;
    }
    
    try {
        const response = await apiCall('/auth/change-password', {
            method: 'POST',
            body: JSON.stringify({
                currentPassword,
                newPassword,
                confirmPassword
            })
        });
        
        showAlert('Password changed successfully!', 'success');
        closeChangePasswordModal();
        
        // Clear form
        document.getElementById('currentPassword').value = '';
        document.getElementById('newPassword').value = '';
        document.getElementById('confirmPassword').value = '';
        
    } catch (error) {
        showAlert(error.message || 'Failed to change password', 'error');
    }
}

function openChangePasswordModal() {
    document.getElementById('changePasswordModal').style.display = 'flex';
}

function closeChangePasswordModal() {
    document.getElementById('changePasswordModal').style.display = 'none';
}
```

**Security Features:**

1. **Verify Current Password First:**
   ```java
   if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
       throw new IllegalArgumentException("Current password is incorrect");
   }
   ```

2. **Require Different Password:**
   ```java
   if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
       throw new IllegalArgumentException("New password must be different");
   }
   ```

3. **Password Strength:**
   - Minimum 8 characters
   - Validation on both client and server

4. **Authentication Required:**
   ```java
   @PostMapping("/change-password")
   public ResponseEntity<...> changePassword(
       @AuthenticationPrincipal User user  // Must be logged in
   ) {
   ```

5. **BCrypt Re-hashing:**
   ```java
   user.setPasswordHash(passwordEncoder.encode(newPassword));
   ```

**Why This Feature:**
- **Security:** Users can change compromised passwords
- **Compliance:** Many regulations require password change
- **UX:** Don't force logout and reset flow
- **Control:** Users manage their own security

**Interview Answer:**
"I implemented a full-stack password change feature. The backend endpoint requires the current password to be verified first before allowing the change - this prevents someone who gains temporary access to a logged-in session from locking out the real user. The new password is hashed with BCrypt before storage, and I enforce that it must be different from the current password. The frontend provides a modal with password visibility toggles for better UX."

---

### Feature 3: Profile API Endpoint 📋

**What It Is:**
GET endpoint that returns authenticated user's profile information.

**Endpoint:**
```
GET /api/auth/profile
Authorization: Bearer {token}
```

**Response:**
```json
{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "9999999999",
    "memberSince": "2024-02-20T10:30:00",
    "isActive": true
}
```

**Backend Implementation:**

**ProfileResponse.java DTO:**
```java
package com.utkarsh.paytm_wallet_clone.dto.response;

import java.time.LocalDateTime;

public class ProfileResponse {
    
    private String name;
    private String email;
    private String phone;
    private LocalDateTime memberSince;
    private Boolean isActive;
    
    public ProfileResponse(String name, String email, String phone, 
                           LocalDateTime memberSince, Boolean isActive) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.memberSince = memberSince;
        this.isActive = isActive;
    }
    
    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDateTime getMemberSince() { return memberSince; }
    public Boolean getIsActive() { return isActive; }
}
```

**AuthController:**
```java
@GetMapping("/profile")
public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal User user) {
    
    ProfileResponse response = new ProfileResponse(
        user.getName(),
        user.getEmail(),
        user.getPhone(),
        user.getCreatedAt(),
        user.getIsActive()
    );
    
    return ResponseEntity.ok(response);
}
```

**Security Configuration:**
```java
// Updated SecurityConfig to require auth for /profile
.requestMatchers("/api/auth/login", "/api/auth/register", 
                 "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
.requestMatchers("/api/auth/**").authenticated()  // profile requires auth
```

**Frontend Usage:**
```javascript
async function loadProfile() {
    try {
        const profile = await apiCall('/auth/profile');
        
        document.getElementById('profileName').textContent = profile.name;
        document.getElementById('profileEmail').textContent = profile.email;
        document.getElementById('profilePhone').textContent = profile.phone;
        
        // Calculate membership duration
        const memberSince = new Date(profile.memberSince);
        const duration = Math.floor((Date.now() - memberSince) / (1000 * 60 * 60 * 24));
        document.getElementById('memberDays').textContent = `${duration} days`;
        
    } catch (error) {
        console.error('Failed to load profile', error);
    }
}
```

**Why This Endpoint:**

1. **Separation of Concerns:**
   - Don't rely solely on JWT payload
   - Allows profile updates to reflect immediately

2. **Data Freshness:**
   - JWT payload is stale (24 hour validity)
   - API call gets latest data

3. **Future Extensibility:**
   ```json
   {
       "name": "...",
       "profilePicture": "url",
       "bio": "...",
       "preferences": {...},
       "statistics": {
           "totalTransfers": 150,
           "totalSpent": 15000
       }
   }
   ```

4. **Mobile App Ready:**
   - Mobile apps need profile endpoint
   - Can't rely on localStorage like web

**Interview Answer:**
"I created a GET /api/auth/profile endpoint that returns the authenticated user's information. While some data is available in the JWT, having a dedicated API endpoint allows getting fresh data and is more extensible for future features like profile pictures or user statistics. It also makes the system more suitable for mobile apps that need to fetch user data."

---

### Feature 4: UI Improvements 🎨

**What Changed:**

**1. Inter Font from Google Fonts:**
```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
```

```css
body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}
```

**Why Inter:**
- ✅ Modern, professional look
- ✅ Excellent readability
- ✅ Used by Stripe, GitHub, Figma
- ✅ Variable font (loads fast)

**2. Time-Based Greeting:**
```javascript
function getGreeting() {
    const hour = new Date().getHours();
    
    if (hour < 12) return '☀️ Good Morning';
    if (hour < 17) return '🌤️ Good Afternoon';
    if (hour < 20) return '🌅 Good Evening';
    return '🌙 Good Night';
}

document.getElementById('greeting').textContent = getGreeting();
```

**Display:**
```
☀️ Good Morning, John!
Manage your digital wallet seamlessly
```

**3. Gradient Balance Card:**
```css
.balance-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    position: relative;
    overflow: hidden;
}

/* Decorative circles */
.balance-card::before {
    content: '';
    position: absolute;
    width: 200px;
    height: 200px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    top: -100px;
    right: -100px;
}

.balance-card::after {
    content: '';
    position: absolute;
    width: 150px;
    height: 150px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 50%;
    bottom: -75px;
    left: -75px;
}
```

**Result:**
```
┌────────────────────────────────┐
│ 💰 Total Balance         ◯     │
│                                │
│ ₹9,594.00                ◯    │
│                                │
│          ◯                     │
└────────────────────────────────┘
```

**4. Action Card Hover Animation:**
```css
.action-card {
    border: 2px solid transparent;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-card:hover {
    border-color: var(--accent-pink);
    transform: translateY(-8px);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.action-card::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #e91e63 0%, #9c27b0 100%);
    transform: scaleX(0);
    transition: transform 0.3s;
}

.action-card:hover::after {
    transform: scaleX(1);
}
```

**5. Cleaner Header Layout:**

**Before:**
```
Welcome, John  [Balance: ₹9594]  Logout
```

**After:**
```
┌─────────────────────────────────────────────┐
│ 👋 Welcome, John                      😎 ▼  │
│ Manage your digital wallet              🔴 Live │
│                                        Logout │
└─────────────────────────────────────────────┘
```

**Impact:**
- ✨ More modern, professional look
- 🎨 Better visual hierarchy
- ⚡ Smoother animations
- 💯 Improved user experience

---

## 🔐 Security Improvement: Tightened Security Config

**What Changed:**
Updated SecurityConfig to be more restrictive.

**Before:**
```java
.requestMatchers("/api/auth/**").permitAll()  // Too permissive!
```

**After:**
```java
// Explicitly list public endpoints
.requestMatchers(
    "/api/auth/login",
    "/api/auth/register",
    "/api/auth/forgot-password",
    "/api/auth/reset-password"
).permitAll()

// Everything else requires authentication
.requestMatchers("/api/auth/**").authenticated()  // profile, change-password, etc.
```

**Why This Matters:**

1. **Principle of Least Privilege:**
   - Only expose what's absolutely necessary
   - Default to authenticated

2. **Prevents Accidents:**
   - New endpoints in /api/auth/ are secure by default
   - Developer must explicitly make them public

3. **Clear Security Boundaries:**
   ```
   Public endpoints:
   ✓ /api/auth/login
   ✓ /api/auth/register
   ✓ /api/auth/forgot-password
   ✓ /api/auth/reset-password
   
   Protected endpoints:
   🔒 /api/auth/profile
   🔒 /api/auth/change-password
   🔒 /api/wallet/**
   🔒 /api/payment/**
   ```

4. **Audit Trail:**
   - Easy to see what's public
   - Security review is straightforward

**Updated JwtAuthFilter:**
```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    
    // Skip JWT validation for public endpoints only
    return path.equals("/api/auth/login") ||
           path.equals("/api/auth/register") ||
           path.equals("/api/auth/forgot-password") ||
           path.equals("/api/auth/reset-password") ||
           path.startsWith("/webhook/") ||
           path.startsWith("/ws/");
}
```

**Security Testing:**
```bash
# Test public endpoints (should work without token)
curl http://localhost:8080/api/auth/login -d '{"email":"...","password":"..."}'  ✓

# Test protected endpoints (should fail without token)
curl http://localhost:8080/api/auth/profile  → 403 Forbidden ✓

# Test with valid token (should work)
curl http://localhost:8080/api/auth/profile -H "Authorization: Bearer {token}"  ✓
```

**Interview Answer:**
"I tightened the security configuration by changing from a blanket permitAll() for /api/auth/** to explicitly listing only the public endpoints. This follows the principle of least privilege - new endpoints are secure by default, and developers must consciously make them public. This prevents accidental exposure of sensitive endpoints."

---

## Summary of Improvements

| Category | Change | Impact |
|----------|--------|--------|
| **Bug Fix** | WebSocket Auth | 🔴 CRITICAL - Notifications now work |
| **Bug Fix** | Logout Path | ✅ Users can logout properly |
| **Bug Fix** | Token Deletion | 🔐 Clean password reset flow |
| **Bug Fix** | Unused Import | 🧹 Clean, warning-free code |
| **Feature** | Profile Dropdown | 👤 Better UX, visual identity |
| **Feature** | Change Password | 🔐 User security control |
| **Feature** | Profile API | 📋 Mobile-ready, extensible |
| **Feature** | UI Improvements | 🎨 Modern, professional design |
| **Security** | Tightened Config | 🔒 Secure by default |

---

**Interview Takeaway:**
"I not only built the core features but also fixed critical bugs and added polish. The WebSocket authentication fix was particularly important - it taught me about STOMP protocol security. The UI improvements show I care about user experience, not just functionality. And the security hardening demonstrates my security-first mindset."
