# 📚 AuraLMS: Enterprise Library Management System

A robust, secure, and scalable **Enterprise Library Management System (LMS)** built with **Java 19**, **Spring Boot 3.x**, **Spring Security (JWT)**, **Spring Data JPA**, **OpenAPI 3.0**, and a modern **interactive Single Page Application (SPA)**.

---

## 🌟 Key Features

1. **Multi-Copy Physical Catalog & Unique Barcodes**:
   - 1-to-many relationship between `Book` metadata and physical `BookCopy` entities.
   - Auto-generated unique barcodes (e.g. `BC-9780132350884-01`).
   - Copy status tracking: `AVAILABLE`, `BORROWED`, `RESERVED`, `LOST`, `DAMAGED`.

2. **Circulation & Lending Lifecycle Engine**:
   - Policy-driven borrow validation: maximum loan limit (default 5), loan duration (default 14 days), and fine blocking threshold (default $10.00).
   - Fast check-in and check-out workflows.
   - 1-click renewal engine with waitlist conflict validation.

3. **Automated Overdue Fines Calculation**:
   $$\text{Fine} = (\text{Days Overdue}) \times (\text{Daily Fine Rate})$$
   - Instant overdue fine accrual and payment settlement workflows.
   - Staff fee waiver capability with audit rationale.

4. **FIFO Reservation & Waitlist Queue**:
   - First-In, First-Out queue for books with 0 available copies.
   - Auto-assignment of returned copies to the oldest waiting patron with a **48-hour hold window**.

5. **Automated Scheduled Background Jobs (`@Scheduled`)**:
   - Midnight and periodic scans to mark overdue loans, accrue daily fines, log 2-day return reminders, and expire uncollected 48-hour hold reservations.

6. **Role-Based Access Control (RBAC)**:
   - Stateless JWT Authentication with BCrypt password hashing.
   - Distinct roles: `MEMBER`, `LIBRARIAN`, and `ADMIN`.

7. **Interactive Web SPA UI Dashboard**:
   - Dark and Light mode themes with glassmorphism.
   - Instant role switching between demo accounts (`Admin`, `Librarian`, `Member`).
   - Real-time catalog search, circulation simulator, member loan portal, fine checkout modal, and admin analytics.

---

## 🔑 Pre-Configured Demo Accounts

| Role | Email | Password | Privileges |
|---|---|---|---|
| **ADMIN** | `admin@library.com` | `Admin@123` | Full access, analytics, user governance, policy sliders |
| **LIBRARIAN** | `librarian@library.com` | `Librarian@123` | Catalog curation, copy barcode status, check-in/out desk, fee waivers |
| **MEMBER** | `member@library.com` | `Member@123` | Catalog browsing, active loans, 1-click renewals, hold queue, fine payment |

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (Java 17, 19, or 21)
- **Maven** (A local wrapper `mvnw.cmd` is included)

### 1. Running Locally (with In-Memory H2 Database)
```bash
# Run backend tests
./mvnw.cmd test

# Launch Spring Boot application
./mvnw.cmd spring-boot:run
```
- **Web SPA UI**: [http://localhost:8080/](http://localhost:8080/)
- **Swagger / OpenAPI 3.0 Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **H2 In-Memory Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:lmsdb`
  - User: `sa`
  - Password: *(leave blank)*

### 2. Running with Docker & PostgreSQL
```bash
docker-compose up --build
```

---

## 🔌 Core REST API Endpoints

### 🔐 Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/register` - Sign up new member account
- `POST /api/v1/auth/login` - Authenticate and obtain JWT Bearer token
- `GET /api/v1/auth/me` - Retrieve current user profile and loan summary

### 📚 Catalog & Inventory (`/api/v1/books`)
- `GET /api/v1/books` - Search & filter catalog (`?search=`, `?category=`, `?availableOnly=true`)
- `GET /api/v1/books/{id}` - Book details with physical copies and barcode statuses
- `POST /api/v1/books` - Add new book title and generate copies *(Librarian/Admin)*
- `PUT /api/v1/books/{id}` - Update book metadata *(Librarian/Admin)*
- `DELETE /api/v1/books/{id}` - Delete title from catalog *(Admin only)*
- `POST /api/v1/books/{id}/copies` - Add copies to book *(Librarian/Admin)*
- `PUT /api/v1/books/copies/{copyId}/status` - Update copy condition *(Librarian/Admin)*

### 🔄 Circulation Desk (`/api/v1/circulation`)
- `POST /api/v1/circulation/borrow` - Issue book copy to user
- `POST /api/v1/circulation/return` - Return copy, calculate fines, and trigger FIFO hold queue
- `POST /api/v1/circulation/renew/{borrowRecordId}` - Extend due date (14 days)
- `GET /api/v1/circulation/my-loans` - Current user loans
- `GET /api/v1/circulation/active` - All active loans *(Librarian/Admin)*
- `GET /api/v1/circulation/overdue` - All overdue loans *(Librarian/Admin)*

### ⏳ Waitlists & Holds (`/api/v1/reservations`)
- `POST /api/v1/reservations` - Place hold on unavailable book
- `POST /api/v1/reservations/{id}/cancel` - Cancel active hold
- `GET /api/v1/reservations/my-reservations` - User's active holds
- `GET /api/v1/reservations/book/{bookId}` - FIFO queue for book

### 💰 Fines & Payments (`/api/v1/fines`)
- `GET /api/v1/fines/my-fines` - User fine balance & history
- `GET /api/v1/fines/all` - All library fines *(Librarian/Admin)*
- `POST /api/v1/fines/{fineId}/pay` - Settle fine online
- `POST /api/v1/fines/{fineId}/waive` - Waive fine with rationale *(Librarian/Admin)*

### 📊 Admin & Analytics (`/api/v1/admin`)
- `GET /api/v1/admin/analytics/dashboard` - Real-time statistics, top books, and revenue
- `GET /api/v1/admin/users` - User directory
- `PUT /api/v1/admin/users/{id}/status` - Toggle ACTIVE / SUSPENDED status
- `GET /api/v1/admin/policies` & `PUT /api/v1/admin/policies` - Update loan limits & fine rates
- `POST /api/v1/admin/scheduler/trigger` - Manually trigger background jobs
