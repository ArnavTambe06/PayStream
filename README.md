# PayStream API 💸

> Production-grade payment orchestration system built with Java 21 + Spring Boot 3.5

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon.tech-blue?logo=postgresql)](https://neon.tech)
[![Redis](https://img.shields.io/badge/Redis-Upstash-red?logo=redis)](https://upstash.com)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Active%20Development-success)]()

---

## 🎯 Overview

PayStream is a **production-grade banking and payment orchestration API** demonstrating enterprise-level backend architecture. Built to showcase real-world engineering decisions — from ACID-compliant transactions to circuit breakers and distributed caching.

Designed for **BFSI/Fintech backend roles** in the Indian job market.

## Frontend
The React dashboard for this API is at:
👉 https://github.com/ArnavTambe06/paystream-ui

### What Makes This Different from a Typical CRUD App

| Pattern | Implementation |
|--------|---------------|
| Double-entry bookkeeping | Every balance change creates two ledger entries |
| Pessimistic locking | `SELECT FOR UPDATE` prevents concurrent balance corruption |
| Idempotency | Duplicate requests return same result without re-executing |
| Circuit breaker | Resilience4j auto-fails over to backup payment provider |
| Versioned DB migrations | Flyway SQL scripts — no `ddl-auto=update` |
| Event audit trail | Every action logged with user, entity, and timestamp |
| Redis caching | Per-cache TTLs with explicit eviction on writes |
| Rate limiting | Redis token bucket, 60 req/min per user |

---

## 🏗️ Architecture

```
React (Vercel)
      │
      │ HTTPS + JWT
      ▼
Spring Boot 3.5 (Railway)
      │
      ├── JWT Auth Filter
      ├── Rate Limiter Filter
      ├── Security Config (RBAC)
      │
      ├── Auth Controller
      ├── Wallet Controller
      ├── Transaction Controller
      ├── Payment Controller
      └── Admin Controller
            │
            ├── Transaction Engine
            │     ├── Pessimistic locking
            │     ├── Double-entry bookkeeping
            │     └── Idempotency checks
            │
            ├── Payment Orchestrator
            │     ├── Circuit Breaker (Resilience4j)
            │     ├── Retry (3 attempts)
            │     └── Fallback provider
            │
            ├── PostgreSQL (Neon.tech)
            │     └── Flyway migrations
            │
            └── Redis (Upstash)
                  ├── Balance cache
                  ├── Rate limit counters
                  └── Distributed locks
```

---

## ✨ Features

### ✅ Phase 1 — Auth + Wallets (Complete)
- User registration + login with JWT (access + refresh tokens)
- BCrypt password hashing
- Role-based access control — `CUSTOMER` and `ADMIN`
- Auto-created SAVINGS wallet on registration
- Multiple wallets per user (SAVINGS, CURRENT)
- Ownership-enforced wallet access
- Input validation + global exception handler
- Consistent `ApiResponse<T>` wrapper on all endpoints
- Flyway DB migrations (V1, V2)
- Swagger/OpenAPI documentation

### ✅ Phase 2 — Transaction Engine (In Progress)
- Deposit, withdraw, P2P transfer
- Double-entry ledger (every debit paired with a credit)
- `@Transactional` with pessimistic locking (`SELECT FOR UPDATE`)
- Deadlock prevention via consistent UUID lock ordering
- Velocity checks (max 10 transactions/hour per wallet)
- Fraud checks (amount limits per operation type)
- Transaction history with pagination

### ✅ Phase 3 — Payment Gateway Orchestration
- Mock payment providers (RazorGate primary, StripeGate fallback)
- Circuit breaker — CLOSED → OPEN → HALF-OPEN state machine
- Retry with exponential backoff (3 attempts)
- Automatic fallback when circuit is OPEN
- Idempotency keys for all payment requests
- Live circuit breaker health via `/actuator/health`

### ✅ Phase 4 — Redis + Admin APIs
- Redis caching with per-cache TTL overrides
- Explicit cache eviction on balance changes
- Rate limiting — 60 req/min per user (Redis token bucket)
- Admin dashboard stats
- User deactivation/reactivation
- Full audit log access
- All admin routes locked to `ROLE_ADMIN`

### ✅ Phase 5 — React Frontend
- Login + Register pages
- Wallet dashboard with balance
- Send money form
- Transaction history table
- Admin panel
- Deployed to Vercel

### ✅ Phase 6 — DevOps
- Docker + Docker Compose
- GitHub Actions CI/CD
- Railway deployment (backend)
- Environment-based config

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Language | Java 21 | LTS, virtual threads ready |
| Framework | Spring Boot 3.5 | REST API, DI, autoconfiguration |
| Security | Spring Security 6 + JWT (jjwt 0.12.3) | Stateless auth, RBAC |
| ORM | Spring Data JPA + Hibernate 6 | Entity mapping, JPQL |
| Database | PostgreSQL 18 (Neon.tech) | ACID, relational integrity |
| Migrations | Flyway 11 | Versioned schema evolution |
| Cache | Redis (Upstash) | Caching, rate limiting, locks |
| Resilience | Resilience4j 2.2 | Circuit breaker, retry, rate limiter |
| Docs | SpringDoc OpenAPI 2.8 | Swagger UI, OAS 3.1 |
| Build | Maven 3.9 | Dependency management |
| Frontend | React + Vite + Tailwind | Dashboard UI |
| Deploy | Railway + Vercel | Backend + frontend hosting |

---

## 🚀 Getting Started

### Prerequisites

```bash
java -version    # Java 21+
mvn -version     # Maven 3.9+
```

### External Services (Free Tier)

| Service | Purpose | URL |
|---------|---------|-----|
| Neon.tech | PostgreSQL database | https://neon.tech |
| Upstash | Redis cache | https://upstash.com |

### Setup

**1. Clone the repo**
```bash
git clone https://github.com/ArnavTambe06/PayStream.git
cd PayStream
```

**2. Configure `application.properties`**
```properties
# Database
spring.datasource.url=jdbc:postgresql://<host>/neondb?sslmode=require&options=endpoint%3D<endpoint-id>
spring.datasource.username=<neon-username>
spring.datasource.password=<neon-password>

# Redis
spring.data.redis.url=rediss://default:<password>@<host>:6380

# JWT
jwt.secret=<minimum-256-bit-secret>
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Admin seed
app.admin.email=admin@paystream.com
app.admin.password=<your-admin-password>
app.admin.name=PayStream Admin
```

**3. Run**
```bash
mvn spring-boot:run
```

**4. Open Swagger**
```
http://localhost:8080/swagger-ui.html
```

---

## 📡 API Endpoints

### Auth — Public
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register + auto-create SAVINGS wallet |
| `POST` | `/api/auth/login` | Login + get JWT tokens |

### Wallets — Authenticated
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/wallets` | Get all my wallets |
| `POST` | `/api/wallets?accountType=CURRENT` | Create additional wallet |
| `GET` | `/api/wallets/{id}` | Get wallet by ID |

### Transactions — Authenticated
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/transactions/deposit` | Deposit funds |
| `POST` | `/api/transactions/withdraw` | Withdraw funds |
| `POST` | `/api/transactions/transfer` | P2P transfer |
| `GET` | `/api/transactions/wallet/{id}` | Transaction history |

### Payments — Authenticated
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/payments/initiate` | Initiate payment via gateway |
| `GET` | `/api/payments` | My payment history |
| `GET` | `/api/payments/{id}` | Payment details |

### Admin — `ROLE_ADMIN` only
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/dashboard` | System stats |
| `GET` | `/api/admin/users` | All users (paginated) |
| `PATCH` | `/api/admin/users/{id}/deactivate` | Deactivate user |
| `PATCH` | `/api/admin/users/{id}/reactivate` | Reactivate user |
| `GET` | `/api/admin/transactions` | All transactions |
| `GET` | `/api/admin/audit-logs` | Full audit trail |

### System
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Health + circuit breaker state |

---

## 📊 Database Schema

```
users
├── id (UUID PK)
├── full_name
├── email (UNIQUE)
├── password_hash
├── role (CUSTOMER | ADMIN)
├── is_active
└── created_at / updated_at

wallets
├── id (UUID PK)
├── user_id (FK → users)
├── account_type (SAVINGS | CURRENT)
├── balance (NUMERIC 19,4)   ← never FLOAT
├── currency
├── is_active
└── created_at / updated_at

transactions
├── id (UUID PK)
├── reference_id (PAY-XXXXXXXX)
├── type (DEPOSIT | WITHDRAWAL | TRANSFER_IN | TRANSFER_OUT)
├── status (PENDING | SUCCESS | FAILED | REVERSED)
├── amount (NUMERIC 19,4)
├── from_wallet_id / to_wallet_id (FK → wallets)
├── initiated_by (FK → users)
├── idempotency_key (UNIQUE)
└── created_at / updated_at

ledger_entries              ← double-entry bookkeeping
├── id (UUID PK)
├── transaction_id (FK)
├── wallet_id (FK)
├── entry_type (DEBIT | CREDIT)
├── amount
├── balance_before
├── balance_after
└── created_at

payment_requests
├── id (UUID PK)
├── provider (RAZORGATE | STRIPEGATE | MANUAL)
├── status (INITIATED | PROCESSING | SUCCESS | FAILED | REFUNDED)
├── idempotency_key (UNIQUE)
├── retry_count
└── created_at / updated_at

audit_logs
├── id (UUID PK)
├── user_id (FK)
├── action
├── entity_type / entity_id
├── details
└── created_at
```

---

## 🔐 Security Design

```
Request
  │
  ▼
RateLimiterFilter         ← Redis counter, 60 req/min/user
  │
  ▼
JwtAuthFilter             ← Validates Bearer token, sets SecurityContext
  │
  ▼
SecurityFilterChain       ← Route-level RBAC
  ├── /api/auth/**        → permitAll
  ├── /api/admin/**       → ROLE_ADMIN only
  └── everything else     → authenticated
  │
  ▼
@PreAuthorize             ← Method-level guard (double protection on admin)
```

**JWT Strategy:**
- Access token: 24h expiry
- Refresh token: 7d expiry
- Stateless — no server sessions
- Secret: minimum 256-bit HMAC key

---

## 💡 Key Engineering Decisions

**Why `BigDecimal` for money?**
`double` and `float` have floating-point precision errors. `BigDecimal(19,4)` guarantees exact arithmetic — critical when balances must reconcile to the paisa.

**Why pessimistic locking?**
If two concurrent withdrawals read the same balance before either writes, both succeed and the wallet goes negative. `SELECT FOR UPDATE` forces serial execution on the wallet row.

**Why lock in UUID order for transfers?**
Thread A locks wallet-1 then wallet-2. Thread B locks wallet-2 then wallet-1. Both wait forever. Locking in consistent UUID order across all threads eliminates this deadlock.

**Why Flyway over `ddl-auto=update`?**
`ddl-auto=update` is non-deterministic in production — it can silently drop columns or miss constraints. Flyway gives version-controlled, repeatable, reviewable migrations.

**Why idempotency keys on transfers?**
Networks fail. Clients retry. Without idempotency, a retry charges the user twice. The key ensures any number of retries produce exactly one financial effect.

---

## 🧪 Testing the API

Import the Postman collection or use Swagger UI directly:

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Arnav Tambe","email":"arnav@test.com","password":"Pass@1234"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"arnav@test.com","password":"Pass@1234"}'

# Get wallets (use token from login)
curl http://localhost:8080/api/wallets \
  -H "Authorization: Bearer <token>"
```

---

## 📁 Project Structure

```
src/main/java/com/paystream/api/
├── config/
│   ├── SecurityConfig.java       ← RBAC + JWT filter chain
│   ├── RedisConfig.java          ← Cache manager + TTL config
│   ├── SwaggerConfig.java        ← OpenAPI + JWT auth scheme
│   ├── RateLimiterFilter.java    ← Redis rate limiting
│   └── AdminSeeder.java          ← Seeds admin user on startup
├── controller/
│   ├── AuthController.java
│   ├── WalletController.java
│   ├── TransactionController.java
│   ├── PaymentController.java
│   └── AdminController.java
├── service/
│   ├── AuthService.java
│   ├── WalletService.java
│   ├── TransactionService.java   ← Core transaction engine
│   ├── PaymentOrchestratorService.java  ← Circuit breaker logic
│   ├── AdminService.java
│   └── CacheService.java
├── gateway/
│   ├── PaymentGateway.java       ← Strategy interface
│   ├── RazorGateProvider.java    ← Primary (70% success sim)
│   └── StripeGateProvider.java   ← Fallback (90% success sim)
├── entity/
│   ├── User.java
│   ├── Wallet.java
│   ├── Transaction.java
│   ├── LedgerEntry.java
│   ├── PaymentRequest.java
│   └── AuditLog.java
├── repository/
│   ├── UserRepository.java
│   ├── WalletRepository.java     ← findByIdWithLock (pessimistic)
│   ├── TransactionRepository.java
│   ├── LedgerEntryRepository.java
│   ├── PaymentRequestRepository.java
│   └── AuditLogRepository.java
├── dto/
│   ├── request/                  ← RegisterRequest, LoginRequest, etc.
│   └── response/                 ← ApiResponse<T>, WalletResponse, etc.
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthFilter.java
│   └── CustomUserDetailsService.java
└── exception/
    ├── PayStreamException.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── db/migration/
│   ├── V1__create_users_and_roles.sql
│   ├── V2__create_wallets.sql
│   ├── V3__create_transactions.sql
│   ├── V4__create_audit_logs.sql
│   └── V5__create_payment_requests.sql
└── application.properties
```

---

## 🗺️ Roadmap

- [x] Phase 1 — Auth + JWT + Wallets
- [x] Phase 2 — Transaction engine (deposit, withdraw, transfer)
- [x] Phase 3 — Payment gateway orchestration + circuit breaker
- [x] Phase 4 — Redis caching + rate limiting + admin APIs
- [x] Phase 5 — React frontend (Vercel)
- [x] Phase 6 — Docker + CI/CD + Railway deploy

---

## 🎤 Interview Talking Points

> *"PayStream is a production-grade payment system I built from scratch. Let me walk you through a few specific engineering decisions..."*

**On transactions:**
"Every money movement creates two ledger entries — a debit and a credit. The wallet balance is always derivable from the ledger, which gives me a complete audit trail and the ability to reconcile at any point. This is how real banking systems work."

**On concurrency:**
"I use pessimistic locking with SELECT FOR UPDATE on wallet rows. For transfers involving two wallets, I always acquire locks in consistent UUID order — lower UUID first — to prevent deadlocks. This is a classic distributed systems problem that trips up a lot of junior engineers."

**On resilience:**
"The payment orchestrator uses Resilience4j's circuit breaker. If the primary provider fails 50% of calls over a 10-call window, the circuit opens and all subsequent calls go directly to the fallback provider — no waiting for timeouts. After 10 seconds it enters HALF-OPEN and tests recovery."

**On caching:**
"I use cache-aside with per-cache TTL overrides based on data volatility. Wallet balances get 5 minutes, user profiles get 15 minutes. On any write that changes a balance, I explicitly evict the cache key — not relying on TTL expiry alone."

---

## 👨‍💻 Author

**Arnav Tambe** — Backend Engineer, Mumbai

- LinkedIn: [arnavtambe06](https://linkedin.com/in/arnavtambe06)
- GitHub: [ArnavTambe06](https://github.com/ArnavTambe06)
- Email: arnavtambe01@gmail.com

*Built phase by phase. Every pattern intentional. Every decision explainable.*
