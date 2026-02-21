💳 Paytm Wallet Clone (Full Stack FinTech Application)

A production-inspired digital wallet system built using Spring Boot and modern web technologies, replicating core features of platforms like Paytm / PhonePe.

The project is developed in structured engineering phases to demonstrate real-world backend architecture, secure payment handling, and scalable system design.

🚀 Project Status
Phase	Description	Status
Phase 1	Foundation & Database Design	✅ Completed
Phase 2	Wallet Operations & Payments	✅ Completed
Phase 3	Real-Time & Advanced Features	🚧 In Progress
🧩 Tech Stack
Backend

Java 21

Spring Boot 3

Spring Security (JWT Authentication)

Spring Data JPA

Hibernate ORM

MySQL

Flyway (DB Migrations)

Maven

Payments

Razorpay Payment Gateway

UPI & Card Test Payments

Secure order tracking

Frontend

HTML

CSS

JavaScript

REST API Integration

📅 Phase 1 — Foundation
✅ Database & Project Setup

Spring Boot project initialization

MySQL datasource configuration

Flyway migration setup

Production-style schema design

Schema validation enabled

Database Tables

users

wallets

transactions

razorpay_orders

flyway_schema_history

Key Engineering Concepts

Ledger-based wallet design

Database-first architecture

Versioned migrations

Clean layered structure

📅 Phase 2 — Wallet & Payment System
🔐 Authentication & Security

User Signup & Login

BCrypt password encryption

JWT Authentication

Role-Based Authorization

Secure protected APIs

Backend validation against invalid data

👛 Wallet Management

Automatic wallet creation per user

Wallet balance tracking

Secure balance updates

Optimistic locking support

💸 Money Transfer System

Core FinTech functionality implemented:

Send money using phone number

Receiver lookup before transfer

Shows registered receiver name

Transfer confirmation step

Balance validation

Atomic debit & credit handling

💳 Add Money (Payment Gateway Integration)

Razorpay order creation

UPI & Card payment support

Payment success handling

Transaction recording after payment

📜 Transaction Ledger

Users can:

View transaction history

Track:

Amount

Date & Time

Sender/Receiver

Credit/Debit type

Transaction status

🌐 Frontend Application

Built custom frontend UI supporting:

Login & Registration

Dashboard with wallet balance

Add Money

Send Money

Transaction History

API integration using JavaScript

🏗️ Project Architecture
Controller Layer
        ↓
Service Layer
        ↓
Repository Layer
        ↓
Database (MySQL)
Additional Layers

Security Layer (JWT + Roles)

Payment Integration Layer

Validation Layer

🧠 Engineering Concepts Demonstrated

REST API Design

Secure Authentication Systems

Financial Transaction Handling

Payment Gateway Integration

Ledger-Based Accounting Model

Backend Validation & Security

Clean Architecture Principles

Full Stack API Integration

🔮 Future Enhancements

WebSocket real-time balance updates

Forgot password workflow

Transaction receipt PDF generation

Notification system

Redis caching

Docker deployment

Microservices architecture

🧪 Running the Project
Backend
mvn spring-boot:run
Access Application
http://localhost:8080
👨‍💻 Author

Utkarsh Tiwari
Java Full Stack Developer (Aspirant)

⭐ Why This Project Matters

This project demonstrates the design of a real-world FinTech backend system rather than a simple CRUD application, focusing on security, transaction integrity, and scalable architecture.
