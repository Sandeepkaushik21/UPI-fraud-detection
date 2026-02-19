# UPI Fraud Detection and Risk Scoring Microservices System – Full Application Documentation

## 1. Introduction

The rapid growth of digital payment systems has increased the risk of financial fraud. UPI-based payment platforms are vulnerable to cyber threats such as phishing, rapid bot-based transactions, account takeovers, and unauthorized device usage.

This project implements a secure UPI payment system integrated with a real-time fraud detection and dynamic risk scoring engine using a Spring Boot microservices architecture. The system processes UPI transactions, evaluates risk in real time, and prevents or flags fraudulent activity before financial loss occurs.

## 2. Problem Statement

Digital UPI transactions face multiple cybersecurity threats:

- Rapid repeated transactions (bot activity)
- Unusual transaction amount spikes
- Unauthorized device-based transactions
- Suspicious IP address changes
- Multiple failed UPI PIN attempts
- Account takeover attempts

Many small and mid-scale systems lack a dynamic risk scoring mechanism to prevent fraud in real time. This project addresses the need for a secure UPI transaction processing system integrated with fraud detection and automated risk-based decision making.

## 3. Objectives

- Securely authenticate users using JWT-based authentication
- Process UPI payments safely and efficiently
- Detect fraudulent transactions using rule-based detection logic
- Calculate a dynamic risk score for each transaction
- Automatically block or flag high-risk transactions
- Provide an admin monitoring system for fraud tracking

## 4. System Architecture

The system uses a microservices architecture.

**Services:**

- API Gateway
- Authentication Service
- UPI Payment Service
- Fraud Detection Service
- Risk Scoring Engine (within Fraud Detection Service)
- Admin Monitoring Service

**Flow:**

```
Client -> API Gateway -> Auth Service (login/register)
Client -> API Gateway -> UPI Payment Service -> Fraud Detection -> Risk Engine -> Decision -> Response
Client -> API Gateway -> Admin Monitoring Service (fraud logs, dashboard)
```

Each service is independently deployable and communicates via REST APIs.

## 5. Module Description

### 5.1 Authentication Service (auth-service)

**Responsibilities:**

- User registration
- User login
- JWT token generation
- Password encryption using BCrypt
- Account lock after multiple failed login attempts

**Security:**

- Role-based access control (USER, ADMIN)
- Token validation for protected requests
- Seeded admin user: `admin@upi` / `admin123`

### 5.2 UPI Payment Service (upi-payment-service)

**Responsibilities:**

- Initiate UPI transaction
- Store transaction details
- Maintain transaction history
- Validate balance
- Track IP address and device ID
- Call Fraud Detection Service for risk evaluation
- Apply decision (approve, suspicious, block)

**Transaction statuses:**

- PENDING
- SUCCESS
- SUSPICIOUS
- BLOCKED
- FAILED

**Features:**

- Idempotency key support for duplicate prevention
- Per-user account with initial demo balance (e.g. Rs 10,000)
- JWT validation for all payment and history endpoints

### 5.3 Fraud Detection Service (fraud-detection-service)

This service evaluates every transaction against predefined security rules.

**Fraud rules (multi-layered defense):**

| Rule | Description | Risk points |
|------|-------------|-------------|
| Rapid Transactions | More than 3 transactions within 60 seconds | 30 |
| Amount Spike | Transaction amount greater than 5x user average | 25 |
| New Device | Transaction from a device not previously seen | 20 |
| IP Change | Transaction from a new IP in a short period | 15 |
| Failed PIN Attempts | More than 3 incorrect UPI PIN attempts | 40 |
| Impossible Travel | Haversine distance/time implies impossible speed (velocity/ATO) | 100 |
| Device Spoofing | Same deviceId but different SHA-256 hardware fingerprint | 40 |
| Unusual Time (UEBA) | Transaction outside user's typical active hours | 25 |

**Risk levels:**

- 0–30: Low risk
- 31–60: Medium risk
- 61+: High risk

**Decision engine:**

- **Low risk:** Transaction approved
- **Medium risk:** Transaction marked suspicious; user notified; admin logging enabled
- **High risk:** Transaction blocked; admin alerted; fraud logged

### 5.4 Risk Scoring Engine

Implemented inside the Fraud Detection Service. It sums risk points from triggered rules and returns total score, risk level, and decision (APPROVE / SUSPICIOUS / BLOCK). Fraud logs are persisted when a transaction is flagged.

### 5.5 Admin Monitoring Service (admin-monitoring-service)

**Responsibilities:**

- Expose fraud logs (aggregated from Fraud Detection Service)
- Dashboard endpoint
- JWT validation with ADMIN role only

## 6. Database Design

**Auth DB (users):**

- id (PK), name, upi_id, password, role, account_status, failed_login_attempts, locked_until, created_at

**Payment DB (accounts, upi_transactions):**

- accounts: id (PK), user_id, upi_id, balance, created_at
- upi_transactions: id (PK), user_id, receiver_upi_id, amount, status, ip_address, device_id, device_fingerprint, latitude, longitude, risk_score, idempotency_key, created_at

**Fraud DB (fraud_logs):**

- id (PK), transaction_id, user_id, fraud_reason, risk_points, total_risk_score, flagged_at

## 7. Security Mechanisms

- JWT-based authentication across services
- BCrypt password hashing
- Idempotency key for duplicate transaction prevention
- Account lock after multiple failed login attempts
- Transaction risk scoring and automated decisions
- Audit logging (fraud logs)
- Role-based access control (USER, ADMIN)
- **Multi-layered fraud defense:** Impossible Travel (Haversine/velocity), Device Fingerprinting (SHA-256), Behavioral time-profiling (UEBA)

## 8. Technologies Used

**Backend:** Java 17, Spring Boot 3.2, Spring Security, REST APIs, JPA, MySQL, JWT (jjwt)

**Frontend:** React 18, Vite, React Router

**Deployment:** Docker (MySQL), Maven

**Tools:** Postman (API testing), Maven (build)

## 9. Working Flow Example

1. User logs in and receives a JWT token.
2. User initiates a UPI transaction (receiver UPI ID, amount, optional device ID).
3. Transaction is stored as PENDING.
4. Payment service calls Fraud Detection Service with transaction context (amount, device, IP, recent tx count, etc.).
5. Fraud rules are executed; risk score is calculated.
6. Decision engine returns APPROVE, SUSPICIOUS, or BLOCK.
7. Transaction status is updated; balance is debited only when approved or suspicious.
8. If blocked or suspicious, fraud is logged; admin can view logs.

## 10. Future Enhancements

- Machine learning-based fraud prediction
- Real-time event streaming (e.g. Kafka)
- Behavioral analytics
- Geo-location based fraud analysis
- SMS/Email fraud alerts
- Dashboard with analytics charts

## 11. Conclusion

This project demonstrates how a UPI payment system can integrate real-time fraud detection and risk scoring to reduce financial crime. The system implements a **multi-layered defense**: rule-based scoring, Impossible Travel (geospatial/velocity), Device Fingerprinting (cryptographic hashing), and Behavioral time-profiling (UEBA). The microservices architecture supports scalability, maintainability, and production readiness while improving transaction security and providing administrative monitoring tools.
