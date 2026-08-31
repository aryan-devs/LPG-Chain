# LPG-Nexus AI

> **Phase 1 — Project Foundation**

An intelligent LPG (Liquefied Petroleum Gas) chain management platform built with Spring Boot and React.  
This project is built **incrementally, phase by phase**, as a learning exercise.

---

## Purpose

LPG-Nexus AI aims to digitise and intelligentise the LPG supply chain — from distributors to customers — with features such as booking management, real-time tracking, anomaly detection, and AI-powered insights.

Phase 1 establishes the project skeleton only. No business logic is implemented yet.

---

## Technology Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17+ | Language |
| Spring Boot | 3.2.x | Application framework |
| Maven | 3.x | Build tool |
| Spring Web | — | REST API layer |
| Spring Data JPA | — | Database abstraction |
| Spring Security | — | Auth (prepared, not yet active) |
| H2 | — | In-memory DB (dev only) |
| Lombok | — | Boilerplate reduction |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 18.x | UI framework |
| TypeScript | 5.x | Type-safe JavaScript |
| Vite | 5.x | Build tool & dev server |
| Tailwind CSS | 3.x | Utility-first styling |
| Lucide React | — | Icon library |

---

## Folder Structure

```
LPG-Nexus-AI/
├── backend/                         # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/lpgnexus/
│   │   │   │   ├── LpgNexusApplication.java   # Entry point
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java    # Spring Security (open in Phase 1)
│   │   │   │   │   └── WebConfig.java         # CORS configuration
│   │   │   │   └── controller/
│   │   │   │       └── HealthController.java  # GET /api/health
│   │   │   └── resources/
│   │   │       └── application.properties     # App configuration
│   │   └── test/
│   │       └── java/com/lpgnexus/
│   │           └── LpgNexusApplicationTests.java
│   └── pom.xml                      # Maven dependencies
│
├── frontend/                        # React + Vite application
│   ├── src/
│   │   ├── App.tsx                  # Application shell
│   │   ├── main.tsx                 # React entry point
│   │   └── index.css                # Global styles + Tailwind
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts               # Vite + API proxy config
│   ├── tailwind.config.js
│   └── tsconfig.json
│
├── docs/                            # Documentation (future phases)
└── README.md
```

---

## How to Run

### Prerequisites
- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.x** — [Download](https://maven.apache.org/)
- **Node.js 18+** — [Download](https://nodejs.org/)

---

### Run the Backend

```bash
# Navigate to the backend directory
cd backend

# Build and run with Maven
mvn spring-boot:run
```

The backend starts at: **http://localhost:8080**

**Health check:**
```bash
curl http://localhost:8080/api/health
# Expected: {"status":"UP","service":"LPG-Nexus AI Backend"}
```

**H2 Console** (in-memory DB browser):  
http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:lpgnexusdb`

---

### Run the Frontend

```bash
# Navigate to the frontend directory
cd frontend

# Install dependencies (first time only)
npm install

# Start the Vite dev server
npm run dev
```

The frontend starts at: **http://localhost:5173**

> The Vite dev server proxies `/api/*` requests to `http://localhost:8080`,  
> so both backend and frontend must be running to see the status indicator.

---

## Phase Roadmap

| Phase | Description | Status |
|---|---|---|
| **Phase 1** | Project foundation & architecture | ✅ Complete |
| Phase 2 | Domain entities (Customer, Distributor, Booking) | ⏳ Planned |
| Phase 3 | Booking management API | ⏳ Planned |
| Phase 4 | Authentication & JWT security | ⏳ Planned |
| Phase 5 | Frontend pages & routing | ⏳ Planned |
| Phase 6 | AI features & anomaly detection | ⏳ Planned |
| Phase 7 | Real-time tracking (WebSocket) | ⏳ Planned |

---

## Notes

- Spring Security is on the classpath but **all endpoints are open** in Phase 1 (by design).
- H2 is used for development convenience. It will be replaced with PostgreSQL or MySQL in a later phase.
- The frontend `App.tsx` is an application **shell** only — no routing or complex UI yet.
