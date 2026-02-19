# UPI Fraud Detection and Risk Scoring Microservices System

A secure UPI payment system with real-time fraud detection and dynamic risk scoring, built with Spring Boot microservices.

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 (or run via Docker: `docker-compose up -d`)
- Node.js 18+ (for frontend)

## Quick start

### 1. Start MySQL

```bash
docker-compose up -d
```

Or use an existing MySQL instance. Ensure root password is `root` or update `application.yml` in each service.

### 2. Build and run backend services

From the project root:

```bash
mvn clean install -DskipTests
```

Run each service in a separate terminal (or run all and the gateway last):

```bash
# Terminal 1 – Auth Service (port 8081)
cd auth-service && mvn spring-boot:run

# Terminal 2 – Fraud Detection Service (port 8083)
cd fraud-detection-service && mvn spring-boot:run

# Terminal 3 – UPI Payment Service (port 8082)
cd upi-payment-service && mvn spring-boot:run

# Terminal 4 – Admin Monitoring Service (port 8084)
cd admin-monitoring-service && mvn spring-boot:run

# Terminal 5 – API Gateway (port 8080)
cd api-gateway && mvn spring-boot:run
```

Start order: Auth and Fraud can be first; then Payment; then Admin; then Gateway.

### 3. Run frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Use the proxy so API calls go to the gateway at http://localhost:8080.

### 4. Default accounts

- **Admin:** UPI ID `admin@upi`, password `admin123`
- **User:** Register at http://localhost:5173/register (e.g. `user@paytm` / your password). New users get a demo balance of Rs 10,000.

## Architecture

| Service               | Port | Description                    |
|-----------------------|------|--------------------------------|
| API Gateway           | 8080 | Routes to all services         |
| Auth Service          | 8081 | JWT auth, registration, login |
| UPI Payment Service   | 8082 | Transactions, balance, history |
| Fraud Detection       | 8083 | Risk rules and scoring         |
| Admin Monitoring      | 8084 | Fraud logs and admin APIs      |

Flow: **Client → Gateway → Auth / UPI / Fraud / Admin**. Payment service calls Fraud service for risk evaluation before completing a transaction.

## API overview (via Gateway http://localhost:8080)

- `POST /api/auth/register` – Register
- `POST /api/auth/login` – Login (returns JWT)
- `GET /api/upi/balance` – Balance (Bearer token)
- `POST /api/upi/pay` – Initiate payment (Bearer token)
- `GET /api/upi/history` – Transaction history (Bearer token)
- `GET /api/admin/fraud-logs` – Fraud logs (Admin Bearer token)

## Documentation

- [FULL_APP.md](FULL_APP.md) – Full application and system documentation
- [PAYMENT.md](PAYMENT.md) – Payment and fraud flow documentation

## Technologies

- **Backend:** Java 17, Spring Boot 3.2, Spring Security, JWT (jjwt), JPA, MySQL
- **Frontend:** React 18, Vite, React Router
- **Deployment:** Docker (MySQL), Maven
