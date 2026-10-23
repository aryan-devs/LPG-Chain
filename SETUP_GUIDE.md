# LPG-Chain — Comprehensive Setup, Architecture & Operations Guide

> **Zero-Cost Production-Grade Architecture & College Prototype Integration**  
> Unified Multi-Role Authentication, Google OAuth 2.0 / OpenID Connect, Cylinder Booking, Demo QR & COD Payments, Tax Invoices, Real-Time Operations, Live KYC Verification, Platform Grievances & Local Ollama AI Assistant.

---

## Table of Contents
1. [System Requirements & Software Versions](#1-system-requirements--software-versions)
2. [Architecture Overview & Port Layout](#2-architecture-overview--port-layout)
3. [Database Configuration (MySQL)](#3-database-configuration-mysql)
4. [Startup Commands](#4-startup-commands)
   - [Spring Boot Backend](#41-spring-boot-backend)
   - [React TypeScript Frontend](#42-react-typescript-frontend)
   - [Python FastAPI Forecasting Engine](#43-python-fastapi-forecasting-engine)
5. [Authentication & Multi-Role Identity](#5-authentication--multi-role-identity)
   - [Role-Based Access Control (ReBAC)](#51-role-based-access-control-rebac)
   - [Default Development Credentials](#52-default-development-credentials)
   - [Customer-Only Public Registration](#53-customer-only-public-registration)
6. [Google OAuth 2.0 / OpenID Connect Setup](#6-google-oauth-20--openid-connect-setup)
7. [Local Ollama AI Assistant Setup](#7-local-ollama-ai-assistant-setup)
8. [Complete Cylinder Booking & Payment Lifecycle](#8-complete-cylinder-booking--payment-lifecycle)
   - [Server-Side Pricing & Booking Integrity](#81-server-side-pricing--booking-integrity)
   - [Demo QR Payment (Interactive Simulator)](#82-demo-qr-payment-interactive-simulator)
   - [Cash on Delivery (COD) Handover Protocol](#83-cash-on-delivery-cod-handover-protocol)
   - [Digital Invoices & Billing](#84-digital-invoices--billing)
9. [KYC & Document Verification Workflow](#9-kyc--document-verification-workflow)
10. [Grievance / Complaint Lifecycle](#10-grievance--complaint-lifecycle)
11. [Real-Time WebSocket & Redis Infrastructure](#11-real-time-websocket--redis-infrastructure)
12. [Testing & Verification](#12-testing--verification)
13. [Troubleshooting & Diagnostics](#13-troubleshooting--diagnostics)

---

## 1. System Requirements & Software Versions

| Component | Minimum Version | Recommended | Notes |
|---|---|---|---|
| **Java Development Kit (JDK)** | Java 17 | Java 21 LTS or 24 | Spring Boot 3.2.5 runtime |
| **Apache Maven** | 3.8+ | 3.9+ | Backend build & test orchestration |
| **Node.js & npm** | Node 18 LTS | Node 20+ LTS / npm 10+ | React 18 & Vite 5 build |
| **MySQL Server** | 8.0+ | 8.0 / 8.4 LTS | Persistent relational storage |
| **Python** | 3.10+ | 3.11 / 3.12 | FastAPI & Scikit-learn demand forecasting |
| **Ollama (Optional / Local AI)** | 0.3+ | Latest (ollama.com) | Free, local instruction LLM (`llama3.2`) |
| **Redis (Optional)** | 6.2+ | 7.x | High-throughput tracking pub/sub |

---

## 2. Architecture Overview & Port Layout

```
                  ┌──────────────────────────────────────────┐
                  │          React 18 + Vite (SPA)           │
                  │          http://localhost:5173           │
                  │   - Public Portal & Unified Auth         │
                  │   - Customer, Distributor & Admin Views  │
                  │   - Interactive Demo QR & Chat Assist    │
                  └─────────────┬────────────────────────────┘
                                │ REST APIs (Bearer JWT)
                                │ STOMP / WebSocket (/ws)
                                ▼
                  ┌──────────────────────────────────────────┐
                  │       Spring Boot Backend Service        │
                  │          http://localhost:8080           │
                  │   - Spring Security & ReBAC Interceptor  │
                  │   - Google OAuth 2.0 Verifier            │
                  │   - Pricing, Booking & Billing Service   │
                  │   - Document Storage & KYC Validator     │
                  │   - Ollama AI Application Tool Dispatcher│
                  └──┬───────────────┬────────────────────┬──┘
                     │               │                    │
          SQL Queries│      HTTP Call│                    │HTTP API
          JDBC       │      (Tool)   │                    │(Port 11434)
                     ▼               ▼                    ▼
     ┌───────────────────────┐ ┌──────────────────┐ ┌───────────────────┐
     │     MySQL Database    │ │ Python FastAPI   │ │ Local Ollama LLM  │
     │ localhost:3306        │ │ ML Forecasting   │ │ (llama3.2)        │
     │ db: lpg_chain         │ │ localhost:8000   │ │ Offline Tool LLM  │
     └───────────────────────┘ └──────────────────┘ └───────────────────┘
```

| Service | Port | Default URL |
|---|---|---|
| Frontend Client (Vite Dev Server) | `5173` | `http://localhost:5173` |
| Spring Boot Backend | `8080` | `http://localhost:8080` |
| Python FastAPI Forecasting Service | `8000` | `http://localhost:8000` |
| MySQL Relational Database | `3306` | `jdbc:mysql://localhost:3306/lpg_chain` |
| Local Ollama HTTP Daemon | `11434` | `http://localhost:11434` |
| Redis Server (Optional tracking) | `6379` | `localhost:6379` |

---

## 3. Database Configuration (MySQL)

Create the MySQL database before booting the backend:

```sql
CREATE DATABASE IF NOT EXISTS lpg_chain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, which automatically generates or alters schema tables without dropping existing customer, agency, or order records.

### Essential Entities Created/Managed:
- `users`: Core identity, password hash with pepper, consumer number, Google sub ID, role (`CUSTOMER`, `DISTRIBUTOR`, `ADMIN`).
- `customer_saved_addresses`: Saved citizen addresses with recipient phone and landmark tagging.
- `bookings`: Cylinder bookings with server-side unit prices, subsidy calculation, payment method (`DEMO_QR`, `COD`), and state machine transitions.
- `payments`: Persisted payment ledgers with idempotency tokens, reference keys, simulation flags, and COD collection timestamps.
- `complaints`: Citizen grievances tagged to bookings, agencies, status workflows, and administrative resolution notes.
- `customer_documents`: File metadata, disk storage paths, verification statuses (`REQUIRED`, `SUBMITTED`, `APPROVED`, `REJECTED`), and review audit trail.

---

## 4. Startup Commands

### 4.1 Spring Boot Backend
Open a terminal in the project directory:

```powershell
cd backend
$env:DB_URL="jdbc:mysql://localhost:3306/lpg_chain?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
mvn spring-boot:run
```

### 4.2 React TypeScript Frontend
Open a second terminal:

```powershell
cd frontend
npm install
npm run dev
```
The application will launch at `http://localhost:5173`.

### 4.3 Python FastAPI Forecasting Engine
Open a third terminal:

```powershell
cd ml
# If virtual environment is not yet created:
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install fastapi uvicorn scikit-learn numpy pandas
uvicorn app.main:app --port 8000 --reload
```

---

## 5. Authentication & Multi-Role Identity

### 5.1 Role-Based Access Control (ReBAC)
The application maintains **one unified login interface** for all users. The backend validates credentials, evaluates database roles, and returns a cryptographically signed JWT containing authorized claims.
- **`CUSTOMER`** $\rightarrow$ Automatically routed to the Customer Portal (`/portal/customer`). Restricted strictly to their own bookings, complaints, addresses, and KYC documents.
- **`DISTRIBUTOR`** $\rightarrow$ Routed to Distributor Portal (`/portal/distributor`). Restricted strictly to their agency's assigned delivery queues, allocations, and handover validations.
- **`ADMIN`** $\rightarrow$ Routed to Admin Portal (`/portal/admin`). Platform-wide oversight, citizen document approval queue, and grievance resolution.

### 5.2 Default Development Credentials
Provisioned automatically by `DevelopmentDataInitializer`:

| Role | Email | Password | Assigned Agency / Scope |
|---|---|---|---|
| **Customer** | `customer@example.com` | `123456` | Domestic Citizen (Consumer #10001) |
| **Distributor** | `distributor@agency.example` | `123456` | Agency #1 (South Delhi Agency) |
| **Admin** | `admin@lpgchain.example` | `Admin@123` | Platform-wide Administrator |

### 5.3 Customer-Only Public Registration
The public `/api/auth/register` endpoint accepts citizen sign-ups and **hardcodes the assigned role to `CUSTOMER`**. Privileged roles (`DISTRIBUTOR`, `ADMIN`) cannot be acquired through self-registration.

---

## 6. Google OAuth 2.0 / OpenID Connect Setup

LPG-Chain implements server-verified Google OpenID Connect:
1. **Frontend Google Button**: Initiates Google Identity Services (`@react-oauth/google` or official Google GSI).
2. **Backend Verification (`POST /api/auth/google`)**:
   - Accepts the Google Credential JWT (ID Token).
   - Validates signature against Google's public JWKS certificates using official `GoogleIdTokenVerifier`.
   - Validates audience matching `GOOGLE_CLIENT_ID` and token expiration.
   - Extracts stable `sub` identity.
   - Links or creates a `CUSTOMER` account. **Never grants Admin or Distributor roles via Google OAuth.**

### Configuring Google Cloud Console:
1. Navigate to [Google Cloud Console Credentials](https://console.cloud.google.com/apis/credentials).
2. Create an **OAuth 2.0 Client ID** (Web application).
3. Add Authorized JavaScript Origins:
   - `http://localhost:5173`
   - `http://localhost:3000`
4. Set in your `.env`:
   ```bash
   GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   ```
5. **Graceful Fallback**: If `GOOGLE_CLIENT_ID` is unconfigured, the system starts smoothly and informs users to use standard email/password authentication.

---

## 7. Local Ollama AI Assistant Setup

LPG-Chain features an integrated Conversational Assistant that operates with **zero paid API dependencies** using Ollama:

### 7.1 Installing Ollama
1. Download and install from [ollama.com](https://ollama.com/download).
2. Open PowerShell and pull a lightweight model suitable for standard developer laptops (16 GB RAM):
   ```powershell
   ollama run llama3.2
   ```
   *(Alternative lightweight models: `mistral`, `phi3`, `gemma2:2b`)*
3. Verify Ollama is responding locally:
   ```powershell
   curl http://localhost:11434/api/tags
   ```

### 7.2 Architecture & Sandboxed Tools
The LLM does **not** have direct SQL access. All user questions are processed through the backend `OllamaAssistantService`:
- It interprets user natural language and maps intent to **scoped application tools**:
  - `getMyBookings`: Fetches bookings for the authenticated caller only.
  - `getMyKycStatus`: Checks document submission and verification metrics.
  - `getMyComplaints`: Lists customer grievances.
  - `getDistributorQueue`: Authorizes agency queue inspection for distributors.
  - `getAdminMetrics`: Platform KPIs for administrators.
  - `getDemandForecast`: Calls the FastAPI machine learning model.
- **Offline Fallback**: If Ollama is not installed or offline, the service automatically executes grounded database retrieval and returns structured answers without crashing or failing.

---

## 8. Complete Cylinder Booking & Payment Lifecycle

### 8.1 Server-Side Pricing & Booking Integrity
All prices are calculated strictly on the backend:
- Domestic 14.2 kg: ₹803.00 (with eligible DBTL subsidy of ₹200.00)
- Commercial 19 kg: ₹1,650.00
- Industrial 47.5 kg: ₹3,950.00
- Saved delivery addresses are loaded and frozen at booking time to preserve historical delivery accuracy.

### 8.2 Demo QR Payment (Interactive Simulator)
- Demonstrates digital payment flows without external gateway fees or merchant account approvals.
- Generates a local dynamic UPI QR code linked to the exact booking reference and server amount.
- **"Simulate Successful Payment"** button triggers an authenticated call to `POST /api/payments/simulate`.
- Validates ownership, ensures idempotency, records transaction ID `DEMO-PAY-{uuid}`, and transitions booking status to `CONFIRMED`.

### 8.3 Cash on Delivery (COD) Handover Protocol
- When COD is selected, payment status remains `PENDING`.
- **ReBAC Restriction**: Customers are prohibited from confirming their own COD payments (HTTP 403 Forbidden).
- Only authorized **Distributors** or **Admins** can confirm cash collection upon physical delivery handover via `POST /api/payments/confirm-cod`.

### 8.4 Digital Invoices & Billing
- Tax bills are dynamically generated from persistent database records via `GET /api/payments/invoice/{bookingId}`.
- Displays GSTIN breakdown (CGST 2.5%, SGST 2.5%), cylinder serial references, and timestamped transaction logs.

---

## 9. KYC & Document Verification Workflow

LPG regulations require identity and address verification before domestic subsidized delivery:
- **Required Documents**:
  1. Aadhaar Card (Citizen Identity Proof)
  2. Ration Card / Electricity Bill (Address Verification)
  3. Bank Passbook / Cancelled Cheque (DBTL Direct Benefit Transfer)
  4. Passport Photo (Customer Identity)
- **Secure Storage**: Files are saved to `uploads/documents/{userId}/` using sanitized filenames preventing directory traversal.
- **Verification Rule**: The system strictly displays **"Your documents are up to date."** only when all required documents have been reviewed and marked `APPROVED` by an administrator.

---

## 10. Grievance / Complaint Lifecycle

Citizens can register complaints tagged to bookings or delivery issues:
- **Categories**: `BILLING_ISSUE`, `DELIVERY_DELAY`, `DAMAGED_CYLINDER`, `GAS_LEAK_URGENT`, `SERVICE_QUALITY`.
- **Urgent Gas Leak (Emergency 1906)**: Prominently displayed with immediate safety protocols.
- **Admin Management**: Administrators filter complaints, assign status (`OPEN`, `UNDER_INVESTIGATION`, `RESOLVED`, `CLOSED`), and attach resolution notes visible to the customer.

---

## 11. Real-Time WebSocket & Redis Infrastructure

- **STOMP Broker**: Configured at `/ws` with simple in-memory broker (or clustered Redis pub/sub).
- **Channels**:
  - `/topic/bookings/{customerId}`: Customer-specific booking updates.
  - `/topic/distributor/{agencyId}`: Agency dispatch order queue.
  - `/topic/admin/feed`: Administrative operational telemetry.
- **WebSocket Channel Interceptor**: Authenticates STOMP connect frames using Bearer JWT and validates destination authorization before subscribing.

---

## 12. Testing & Verification

### Running Full Automated Integration Suite:
Execute the comprehensive test suite verifying ReBAC isolation, Demo QR payments, COD authorization, KYC status logic, and AI assistant tool invocations:

```powershell
cd backend
mvn test -Dtest=MasterWorkflowIntegrationTest
```
*Expected Result: Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS).*

### Running Frontend Tests & Typecheck:
```powershell
cd frontend
npm test -- --run
npx tsc --noEmit
npm run build
```
*Expected Result: 49 passing tests across 8 test suites, 0 TypeScript diagnostic errors, production Vite bundle built.*

---

## 13. Troubleshooting & Diagnostics

| Symptom | Probable Cause | Resolution |
|---|---|---|
| `CommunicationsException: Connection refused` | MySQL service not running | Start MySQL service via `net start MySQL80` or Services Manager. |
| `Access denied for user 'root'@'localhost'` | Incorrect database credentials | Update `DB_USERNAME` and `DB_PASSWORD` in your environment or `.env`. |
| `Google OAuth button shows configuration message` | `GOOGLE_CLIENT_ID` not set | Configure Google Client ID or use development email/password accounts. |
| `Ollama AI returns grounded fallback responses` | Ollama service not running | Start Ollama via `ollama run llama3.2` or keep offline fallback active. |
| `Port 8080 already in use` | Previous backend instance still running | Run `Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process` in PowerShell. |
| `Frontend fails to connect to backend` | CORS or backend offline | Verify backend is serving at `http://localhost:8080/api/health`. |
