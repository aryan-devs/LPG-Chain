# Redis + WebSocket Horizontal Scaling Architecture

This document describes the multi-instance horizontal scaling architecture for real-time vehicle GPS tracking in LPG-Chain.

---

## 1. Why Redis is Needed for Multiple Spring Boot Instances

When running multiple instances of the LPG-Chain backend (e.g., Instance 1 on port 8080 and Instance 2 on port 8081):

### Without Redis (Single-Node In-Memory Limitation):
- **Client A** connects via WebSocket/STOMP to **Instance 1**.
- **Client B** connects via WebSocket/STOMP to **Instance 2**.
- A delivery driver or GPS tracker ingests a location update through **Instance 1**.
- **Instance 1** broadcasts the event to its own local in-memory STOMP broker.
- **Problem**: **Client B** (connected to Instance 2) **never receives the tracking event** because Instance 2 has no awareness of the event ingested on Instance 1.

```
Without Redis:
GPS Update → [Instance 1] ──> [Client A] (Received)
             [Instance 2] ──> [Client B] (NEVER RECEIVED!)
```

### With Redis Pub/Sub (Shared Real-Time Event Distribution Layer):
- A location update ingested by **Instance 1** is persisted to MySQL and published to Redis channel `lpgchain:tracking:vehicle-location`.
- **Both Instance 1 and Instance 2** subscribe to `lpgchain:tracking:vehicle-location`.
- When Redis Pub/Sub delivers the event:
  - **Instance 1** receives the event and forwards it to its locally connected clients (Client A).
  - **Instance 2** receives the event and forwards it to its locally connected clients (Client B).
- Both clients receive the exact same telemetry event in real time.

```
With Redis Pub/Sub:
GPS Update
    ↓
[Instance 1] (Persists to MySQL + Writes latest cache to Redis)
    ↓
[Redis Pub/Sub Channel: lpgchain:tracking:vehicle-location]
    ├──> [Instance 1 Subscriber] ──> STOMP ──> [Client A]
    └──> [Instance 2 Subscriber] ──> STOMP ──> [Client B]
```

---

## 2. Layer Responsibilities

| Component | Responsibility | Persistence / Lifetime |
|---|---|---|
| **MySQL Database** | Permanent durable history of all `VehicleLocation` points. Source of truth for audit trails, compliance, and route history. | Durable disk storage |
| **Redis Cache** | Temporary cache for the latest vehicle GPS point (`vehicle:{vehicleId}:latest-location`) with configurable TTL (default 60s). Fast reads, fallback to MySQL on miss. | In-memory key-value with TTL |
| **Redis Pub/Sub** | Cross-instance real-time fanout broker. Distributes tracking events across all active Spring Boot nodes. | Ephemeral broadcast |
| **WebSocket / STOMP** | Client connectivity layer. Delivers events to authenticated web/mobile clients subscribed to `/topic/deliveries/{deliveryId}/tracking`. | Session-based |

---

## 3. Configuration Parameters

All multi-instance settings can be configured via environment variables:

| Variable | Property Name | Default | Purpose |
|---|---|---|---|
| `SERVER_PORT` | `server.port` | `8080` | HTTP and WebSocket server port |
| `INSTANCE_ID` | `lpgchain.redis.instance-id` | `lpg-chain-1` | Instance identifier for logs and observability |
| `REDIS_HOST` | `spring.data.redis.host` | `localhost` (`redis` in Docker) | Shared Redis server hostname |
| `REDIS_PORT` | `spring.data.redis.port` | `6379` | Shared Redis server port |
| `REDIS_PASSWORD` | `spring.data.redis.password` | *(empty)* | Optional Redis authentication |
| `REDIS_TRACKING_CHANNEL` | `lpgchain.redis.tracking-channel` | `lpgchain:tracking:vehicle-location` | Shared Pub/Sub channel |
| `REDIS_LATEST_LOCATION_TTL_SECONDS` | `lpgchain.redis.latest-location-ttl-seconds` | `60` | TTL for latest vehicle location |
| `REDIS_LISTENER_AUTO_STARTUP` | `lpgchain.redis.listener-auto-startup` | `true` | Background listener lifecycle control |

---

## 4. Loop and Duplicate Prevention

To prevent message echo loops (Redis → WebSocket → Redis):
- `VehicleTrackingRedisSubscriber` listens to Redis Pub/Sub and passes incoming events exclusively to `VehicleTrackingEventPublisher.publishLocationUpdate(...)`.
- `VehicleTrackingEventPublisher` forwards strictly to the local Spring STOMP messaging broker (`SimpMessagingTemplate.convertAndSend(...)`).
- A received Redis message is **never republished** to Redis. Redis Pub/Sub publishing is initiated **only** after MySQL transaction commitment during location ingestion.

---

## 5. Security & Authorization

- **Redis is Internal**: Redis is never exposed directly to external clients or browsers.
- **WebSocket ReBAC Enforcement**: All STOMP subscription requests to `/topic/deliveries/{deliveryId}/tracking` are intercepted by `WebSocketAuthChannelInterceptor`.
  - Customers can only subscribe to their own deliveries.
  - Distributors can only subscribe to deliveries of their own agency.
  - Admins retain system-wide visibility.
- **Client Ingestion Blocked**: Client STOMP `SEND` messages are rejected (`403 Forbidden`). REST endpoints remain the only authorized location ingestion path.

---

## 6. Local Verification with Two Spring Boot Instances

### Step 1: Start Redis and MySQL using Docker Compose
```bash
docker compose up -d redis mysql
```

### Step 2: Start Spring Boot Instance 1 (Port 8080)
In Terminal 1:
```bash
cd backend
$env:SERVER_PORT="8080"
$env:INSTANCE_ID="lpg-chain-1"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Step 3: Start Spring Boot Instance 2 (Port 8081)
In Terminal 2:
```bash
cd backend
$env:SERVER_PORT="8081"
$env:INSTANCE_ID="lpg-chain-2"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Step 4: Verify Both Instances Connect to the Same Redis
Observe console output on startup:
```text
[Instance 1] RedisMessageListenerContainer initialized for channel [lpgchain:tracking:vehicle-location] (autoStartup=true)
[Instance 2] RedisMessageListenerContainer initialized for channel [lpgchain:tracking:vehicle-location] (autoStartup=true)
```

### Step 5: Ingest a Location Point on Instance 1
Send a REST POST request to Instance 1 (`http://localhost:8080/api/tracking/vehicles/{vehicleId}/location`):
```bash
curl -X POST http://localhost:8080/api/tracking/vehicles/1/location \
  -H "Authorization: Bearer <ADMIN_OR_DISTRIBUTOR_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 18.5204,
    "longitude": 73.8567,
    "speed": 35.0,
    "heading": 180.0,
    "recordedAt": "2026-09-29T23:30:00"
  }'
```

### Step 6: Verify Event Reception Across Both Instances
- **Instance 1 Log**:
  ```text
  Instance [lpg-chain-1] received Redis tracking event for delivery #1 (vehicle=MH-12-AB-1234)
  ```
- **Instance 2 Log**:
  ```text
  Instance [lpg-chain-2] received Redis tracking event for delivery #1 (vehicle=MH-12-AB-1234)
  ```
- Connected WebSocket clients on **both** instances receive the tracking event.
