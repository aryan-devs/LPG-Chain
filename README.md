# LPG-Chain — Intelligent LPG Supply Chain Management Platform

> **Comprehensive Full-Stack Implementation**  
> Unified Multi-Role Authentication, Google OAuth 2.0 / OpenID Connect, Cylinder Booking Lifecycle, Demo QR & Cash on Delivery (COD) Payments, Digital GST Invoices, Real-Time Fleet & Order Telemetry, Live Citizen KYC Verification, Grievance Management & Local Ollama AI Assistant.

---

## Quick Reference & Setup Guide

📖 **For detailed setup, architectural diagrams, environment variable reference, and testing steps, please read:**  
👉 **[SETUP_GUIDE.md](./SETUP_GUIDE.md)**

---

## 1. System Architecture

LPG-Chain integrates a resilient full-stack enterprise architecture built with zero external paid API dependencies:

- **Frontend (Client Layer)**: React 18, TypeScript, Vite, Tailwind CSS, Lucide icons, responsive crimson-red brand layout.
- **Backend (Service & Business Rules)**: Java 21/24, Spring Boot 3.2.5, Spring Security with Role-Based Access Control (ReBAC), JWT with password pepper defense-in-depth, STOMP WebSockets.
- **Database (Persistence Layer)**: MySQL 8.x relational persistence with Hibernate JPA entity mappings.
- **AI & Analytics (Intelligence Layer)**:
  - **Demand Forecasting**: Python FastAPI microservice with Scikit-learn Random Forest regression model.
  - **Conversational Assistant**: Local Ollama LLM (`llama3.2`) with sandboxed backend application tools and offline grounded database fallback.
- **Real-Time Telemetry**: STOMP over WebSocket broker with Redis Pub/Sub support.

---

## 2. Default Test Accounts

Development seed data initializes three distinct role identities for end-to-end verification:

| Role | Email | Password | Scope / Permissions |
|---|---|---|---|
| **Customer** | `customer@example.com` | `123456` | Domestic Citizen (Consumer #10001) |
| **Distributor** | `distributor@agency.example` | `123456` | South Delhi Agency (Agency #1) |
| **Admin** | `admin@lpgchain.example` | `Admin@123` | Platform-wide Administrator |

---

## 3. Core Feature Highlights

### 🛡️ Unified Authentication & Google OAuth 2.0
- Single sign-in modal for Customer, Distributor, and Admin roles.
- Role-based automatic routing to respective portals.
- Google OpenID Connect ID Token verification (`POST /api/auth/google`) using official Google token verifiers.
- Public registration creates `CUSTOMER` accounts only.

### 📦 Cylinder Booking Lifecycle & Server-Side Pricing
- Trusted server-side pricing: Domestic 14.2 kg (₹803.00 with ₹200.00 DBTL subsidy), Commercial 19 kg (₹1,650.00), Industrial 47.5 kg (₹3,950.00).
- Historical address locking for immutable audit tracking.
- Idempotent booking and payment creation.

### 💳 Demo QR & Cash on Delivery (COD) Payments
- **Demo QR**: Generates dynamic local UPI QR codes and provides an interactive "Simulate Successful Payment" button.
- **Cash on Delivery**: Remains `PENDING` until physical delivery handover. Customers are blocked from self-confirming (HTTP 403 Forbidden); only authorized distributors can confirm cash collection.
- **Digital GST Tax Invoices**: Downloadable and printable with itemized CGST/SGST breakdowns.

### 📄 Live Citizen KYC Verification
- Multi-document requirements: Aadhaar Card, Address Proof, Bank Passbook (DBTL), and Photo.
- Secure localized disk storage (`uploads/documents/{userId}/`) with sanitized filenames.
- Strict compliance rule: *"Your documents are up to date."* displays exclusively when all 4 required documents are approved.
- Administrative document verification and review queue with rejection reasons.

### 📢 Grievance / Complaint Management
- Customer ticket registration with category selection and optional booking attachment.
- National Emergency 1906 Gas Leak alert protocols.
- Administrative grievance investigation, status workflow, and resolution notes.

### 🤖 Local Ollama AI Assistant
- Operates locally via `http://localhost:11434` with zero paid API costs.
- Scoped application tools: `getMyBookings`, `getMyKycStatus`, `getMyComplaints`, `getDistributorQueue`, `getAdminMetrics`, `getDemandForecast`.
- Reliable offline fallback directly querying grounded database records if Ollama is not installed or running.

---

## 4. Quick Start

### 1. Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```

### 2. Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```

### 3. ML Demand Forecasting Service (FastAPI)
```bash
cd ml
python -m venv .venv
.\.venv\Scripts\Activate.ps1
uvicorn app.main:app --port 8000 --reload
```

---

## 5. Verification & Testing

```bash
# Backend Automated Integration Tests (8/8 Passed)
cd backend
mvn test -Dtest=MasterWorkflowIntegrationTest

# Frontend Test Suite (49/49 Passed) & Type Check
cd frontend
npm test -- --run
npx tsc --noEmit
npm run build
```
