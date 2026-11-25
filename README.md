# ⚡ Zapiee

> **Scan. Order. Pay.** A multi-tenant, QR-based restaurant ordering SaaS built on a Spring Boot microservices architecture.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-RLS-blue)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-Events-black)
![Redis](https://img.shields.io/badge/Redis-Cache-red)
![Razorpay](https://img.shields.io/badge/Razorpay-Payments-0C2451)

---

## 📖 Overview

Zapiee lets restaurants go digital in minutes. Each table gets a QR code; diners scan it, browse the menu, place an order, and pay, all from their phone with no app install. Restaurants get a dashboard to manage menus, orders, and subscriptions.

The platform is built as a **multi-tenant SaaS**: many restaurants share one deployment while their data stays strictly isolated.

## ✨ Features

- **QR-based ordering**: table-specific QR codes open the restaurant's live menu
- **Geofencing**: QR scans are validated against the restaurant's location using the Haversine formula, so only on-premise diners can order
- **Rotating QR tokens**: prevents misuse of screenshotted or shared QR codes
- **Multi-tenancy**: schema-per-tenant isolation in PostgreSQL, reinforced with Row-Level Security
- **Subscription plans**: JSONB-backed feature flags with a Redis-cached quota enforcement library
- **Payments**: Razorpay integration with Razorpay Route (linked accounts), UPI checkout, and webhook-driven state management
- **Media management**: menu images, restaurant logos, and receipts stored via Cloudinary
- **Real-time order flow**: event-driven updates through Kafka

## 🏗️ Architecture

Zapiee is composed of **seven Spring Boot microservices** communicating through REST and Kafka events.

```
                    ┌──────────────┐
   Client  ───────► │ API Gateway  │
                    └──────┬───────┘
        ┌─────────┬────────┼─────────┬──────────┐
        ▼         ▼        ▼         ▼          ▼
     Menu      Order   Subscription  File/Media  ...
    Service   Service  & Payment    Service
        │         │     Service         │
        └─────────┴────────┬────────────┘
                           ▼
                  Kafka (event streaming)
                           │
              PostgreSQL  ·  Redis  ·  Cloudinary
```

> 🔧 Update the diagram and the service list below to match your exact service names.

### Services

| Service | Responsibility |
|---|---|
| **Menu Service** | Categories, items, pricing, availability |
| **Order Service** | Order lifecycle, table sessions, status updates |
| **Subscription & Payment Service** | Plans, upgrades, Razorpay Route, webhooks |
| **File/Media Service** | Cloudinary uploads; metadata-only storage in Postgres |
| **Tenant / Auth Service** | Tenant onboarding, authentication, JWT |
| **API Gateway** | Routing, auth, rate limiting |
| **Notification Service** | Order and payment event notifications |

### Distributed Patterns

- **Transactional Outbox**: reliable event publishing without dual-write problems
- **CQRS**: separate read and write models for high-traffic paths such as menu reads
- **Saga**: coordinating multi-service workflows such as subscription upgrade and payment
- **UpgradeContext pattern**: structured handling of subscription upgrade flows

### Multi-Tenancy Model

- **Schema-per-tenant** in PostgreSQL for strong logical isolation
- **Row-Level Security (RLS)** as a second line of defense
- Tenant context resolved per request and propagated across services

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language / Framework | Java, Spring Boot |
| Database | PostgreSQL (schema-per-tenant, RLS, JSONB) |
| Caching | Redis |
| Messaging | Apache Kafka |
| Payments | Razorpay (Route, Subscriptions, UPI) |
| Media | Cloudinary |
| Data Access | Spring Data JPA / Hibernate |
| Build | Maven |
| Frontend | React |

## 🚀 Getting Started

### Prerequisites

- Java
- Maven
- Docker & Docker Compose
- Razorpay test account
- Cloudinary account

### Run Locally

```bash
# Clone the repository
git clone https://github.com/shivamdhakad14/zapiee.git
cd zapiee

# Start infrastructure (PostgreSQL, Redis, Kafka)
docker compose up -d

# Build all services
mvn clean install

# Run a service (repeat for each)
cd menu-service
mvn spring-boot:run
```

### Environment Variables

Create a `.env` file or set these in each service's `application.yml`:

```properties
# Database
DB_URL=jdbc:postgresql://localhost:5432/zapiee
DB_USERNAME=
DB_PASSWORD=

# Redis / Kafka
REDIS_HOST=localhost
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Razorpay
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=

# Cloudinary
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=

# Security
JWT_SECRET=
```

## 📡 API Documentation

Each service exposes Swagger UI when running locally:

```
http://localhost:<port>/swagger-ui.html
```

## 🗂️ Project Structure

```
zapiee/
├── api-gateway/
├── menu-service/
├── order-service/
├── subscription-payment-service/
├── file-media-service/
├── tenant-auth-service/
├── notification-service/
├── docker-compose.yml
└── README.md
```

## 🔐 Security Highlights

- Tenant isolation via schema separation and PostgreSQL RLS
- Geofenced, rotating QR tokens
- Webhook signature verification for Razorpay events
- JWT-based authentication at the gateway

## 🗺️ Roadmap

- [ ] Kitchen display system
- [ ] Analytics dashboard for restaurant owners
- [ ] Multi-language menus
- [ ] Loyalty and coupon engine

## 🤝 Contributing

Contributions, issues, and feature requests are welcome. Open an issue first to discuss major changes.

## 👤 Author

**Shivam**
- GitHub: [@shivamdhakad14](https://github.com/shivamdhakad14)
- LinkedIn: [shivam-atoliya0014](https://linkedin.com/in/shivam-atoliya0014)
- Email: shivamdhakad0014@gmail.com

---

⭐ If you find this project interesting, consider giving it a star!
