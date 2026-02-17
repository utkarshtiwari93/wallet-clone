# 💳 Paytm Wallet Clone (Backend)

A Spring Boot–based digital wallet backend inspired by Paytm.  
This project is being developed in **phases** to demonstrate real-world backend engineering practices, clean architecture, and production-ready design.

---

## 🚀 Project Status
**Phase 1 — Foundation (Day 1 & Day 2) ✅ Completed**

---

## 🧩 Tech Stack
- **Java 21**
- **Spring Boot 3**
- **Spring Data JPA**
- **Spring Security (UserDetails based auth)**
- **MySQL**
- **Flyway (Database Migrations)**
- **Maven**
- **Hibernate ORM**

---

## 📅 Day 1 — Project Setup & Database Foundation

### ✅ What was done
- Created Spring Boot project with Maven
- Configured MySQL datasource using `application.yml`
- Enabled Flyway for database migrations
- Designed production-grade database schema
- Implemented versioned SQL migrations
- Verified automatic schema creation on startup

### 📂 Database Tables Created
- `users` — stores registered users
- `wallets` — one wallet per user
- `transactions` — immutable ledger for all balance changes
- `razorpay_orders` — tracks Razorpay payment attempts
- `flyway_schema_history` — migration history

### 🧠 Key Concepts Applied
- Database-first design
- Ledger-based transaction system
- Schema versioning with Flyway
- Separation of concerns (DB managed outside JPA)

---

## 📅 Day 2 — JPA Entities & Repository Layer

### ✅ What was done
- Mapped all database tables to JPA entities
- Implemented relationships between entities
- Implemented `UserDetails` for Spring Security compatibility
- Added repositories for all core entities
- Enabled strict schema validation (`ddl-auto: validate`)
- Successfully tested DB persistence

### 📦 Entities Implemented
- `User` — represents system users, integrates with Spring Security
- `Wallet` — manages user balance with optimistic locking
- `Transaction` — ledger entity for all wallet operations
- `RazorpayOrder` — tracks payment lifecycle

### 🗂 Repository Interfaces
- `UserRepository`
  - `findByEmail`
  - `findByPhone`
- `WalletRepository`
  - `findByUserId`
- `TransactionRepository`
- `RazorpayOrderRepository`

### 🧪 Testing
- Verified JPA–DB synchronization
- Inserted test user via repository
- Confirmed persistence through MySQL queries

---

## 🏗️ Project Architecture (So Far)

