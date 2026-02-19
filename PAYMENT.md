# UPI Payment and Fraud Flow – Payment Module Documentation

## 1. Overview

This document describes the payment flow, fraud evaluation, and risk-based decisions in the UPI Fraud Detection system. It covers the UPI Payment Service, the Fraud Detection Service, and how they work together.

## 2. Payment Flow

### 2.1 High-level sequence

1. **Client** sends a payment request to the **API Gateway** (`POST /api/upi/pay`) with a valid JWT.
2. **API Gateway** forwards the request to the **UPI Payment Service**.
3. **UPI Payment Service:**
   - Resolves or creates the sender’s account and checks balance.
   - Checks idempotency key to avoid duplicate processing.
   - Gathers context for fraud: recent transaction count, known devices, known IPs, user average amount.
   - Calls **Fraud Detection Service** (`POST /api/fraud/evaluate`) with this context.
4. **Fraud Detection Service** runs rule checks and returns:
   - `totalRiskScore`
   - `riskLevel` (LOW / MEDIUM / HIGH)
   - `decision` (APPROVE / SUSPICIOUS / BLOCK)
   - `triggeredRules` (list of rules that fired).
5. **UPI Payment Service** applies the decision:
   - **APPROVE:** Debit balance, set status to SUCCESS.
   - **SUSPICIOUS:** Debit balance, set status to SUSPICIOUS, call Fraud Service to log.
   - **BLOCK:** Do not debit; set status to BLOCKED; call Fraud Service to log.
6. Response is returned to the client with status, message, and (if applicable) risk score and level.

### 2.2 Idempotency

- Optional `idempotencyKey` can be sent in the payment request.
- If a transaction with the same key already exists, the service rejects the request as duplicate.
- Prevents double debit when the client retries.

### 2.3 Balance and account

- Each user has an account (created on first payment or first balance check) with an initial demo balance (e.g. Rs 10,000).
- Payment is allowed only if `balance >= amount`.
- On APPROVE or SUSPICIOUS, balance is debited by the transaction amount.

## 3. Fraud Detection Rules

The Fraud Detection Service evaluates the request and context against these rules (multi-layered defense):

| Rule | Condition | Risk points |
|------|-----------|-------------|
| Rapid Transactions | 3 or more transactions in the last 60 seconds | 30 |
| Amount Spike | Amount > 5 × user’s average transaction amount | 25 |
| New Device | Current `deviceId` not seen in recent user transactions | 20 |
| IP Change | Current client IP not seen in recent user transactions | 15 |
| Failed PIN Attempts | `recentFailedPinAttempts >= 3` (from request context) | 40 |
| **Impossible Travel** | Required speed (Haversine distance / time) > max allowed (e.g. 1200 km/h) | 100 |
| **Device Spoofing** | Same `deviceId` but different hardware fingerprint (SHA-256 hash) | 40 |
| **Unusual Time (UEBA)** | Transaction outside user’s typical active hours (behavioral time-profiling) | 25 |

- Each rule that is satisfied adds its points to the total risk score.
- Total score is the sum of all triggered rule points.
- **Impossible Travel** uses a high point value (100) so it effectively forces a BLOCK (velocity / ATO detection).

## 4. Risk Levels and Decisions

**Risk level (by total score):**

- **LOW:** 0–30  
- **MEDIUM:** 31–60  
- **HIGH:** 61+

**Decision:**

- **LOW** → `APPROVE` – Transaction is completed normally.
- **MEDIUM** → `SUSPICIOUS` – Transaction is completed but flagged; user can be notified; admin logging is enabled.
- **HIGH** → `BLOCK` – Transaction is not completed; account can be temporarily restricted; admin is alerted.

## 5. API Contracts (payment and fraud)

### 5.1 Initiate payment (UPI Payment Service)

**Request:** `POST /api/upi/pay` (Bearer JWT)

```json
{
  "receiverUpiId": "receiver@paytm",
  "amount": 500.00,
  "deviceId": "optional-device-id",
  "deviceFingerprintInput": "UA|1024x768|Asia/Kolkata|en-IN",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "idempotencyKey": "optional-unique-key"
}
```

- `deviceFingerprintInput`: concatenation of User-Agent, screen resolution, timezone, language; backend hashes with SHA-256 (MessageDigest) for device fingerprinting.
- `latitude` / `longitude`: optional; used for Impossible Travel detection (Haversine).

**Response:**

```json
{
  "transactionId": 1,
  "status": "SUCCESS",
  "receiverUpiId": "receiver@paytm",
  "amount": 500.00,
  "riskScore": 0,
  "riskLevel": "LOW",
  "message": "Payment successful.",
  "createdAt": "2025-02-14T10:00:00Z"
}
```

Possible `status`: `SUCCESS`, `SUSPICIOUS`, `BLOCKED`, `FAILED`.

### 5.2 Fraud evaluation (Fraud Detection Service, internal)

**Request:** `POST /api/fraud/evaluate?recentTxCount=0&newDevice=false&newIp=false`

```json
{
  "userId": 1,
  "senderUpiId": "user@paytm",
  "receiverUpiId": "receiver@paytm",
  "amount": 500.00,
  "ipAddress": "192.168.1.1",
  "deviceId": "device-123",
  "recentFailedPinAttempts": 0,
  "knownDeviceIds": ["device-123"],
  "knownIpAddresses": ["192.168.1.1"],
  "userAverageTransactionAmount": 300.00
}
```

**Response:**

```json
{
  "totalRiskScore": 0,
  "riskLevel": "LOW",
  "decision": "APPROVE",
  "triggeredRules": []
}
```

### 5.3 Fraud logging (Fraud Detection Service, internal)

When the decision is SUSPICIOUS or BLOCK, the Payment Service calls:

**Request:** `POST /api/fraud/log`

```json
{
  "transactionId": 1,
  "userId": 1,
  "totalRiskScore": 45,
  "triggeredRules": [
    {
      "ruleName": "Amount Spike",
      "reason": "Transaction amount greater than 5x user average",
      "points": 25
    }
  ]
}
```

This persists entries in the `fraud_logs` table for admin monitoring.

## 6. Transaction History and Balance

- **Balance:** `GET /api/upi/balance` (Bearer JWT) returns `{ "balance": 10000.00 }`.
- **History:** `GET /api/upi/history?page=0&size=20` (Bearer JWT) returns a list of transactions with id, status, receiverUpiId, amount, riskScore, createdAt.

## 7. Security and context

- **JWT:** All payment and history endpoints require a valid JWT. User ID and UPI ID are taken from the token.
- **IP and device:** The Payment Service captures client IP (and optional device ID) and sends them to the Fraud Detection Service so that “new device” and “IP change” rules can be applied.
- **Secrets:** All services that validate or issue JWTs must use the same `jwt.secret` for consistency.

## 8. Advanced Fraud Layers (Multi-Layered Defense)

### 8.1 Impossible Travel Detection (Geospatial / Velocity)

- **Purpose:** Detect velocity attacks and account takeover (ATO) when the same user cannot physically be in two locations in the elapsed time.
- **Flow:** Payment request can include `latitude` and `longitude`. The UPI Payment Service retrieves the previous transaction’s location and timestamp. The Fraud Detection Service uses the **Haversine formula** (Java) to compute great-circle distance (km) between the two points and derives the required speed (distance / time). If required speed exceeds a configured maximum (e.g. 1200 km/h), the **Impossible Travel** rule triggers and the transaction is **BLOCKED** (100 risk points).
- **Cyber value:** Velocity-based fraud and ATO detection.

### 8.2 Advanced Device Fingerprinting (Identity Security)

- **Purpose:** Differentiate real hardware from spoofed `deviceId`. A simple `deviceId` can be faked; a hash of non-sensitive hardware/browser traits is harder to spoof.
- **Flow:** The frontend collects User-Agent, screen resolution, timezone, and language. These are concatenated and sent as `deviceFingerprintInput`. The backend hashes the string with **SHA-256** (Java `MessageDigest`) and stores/compares the hash. If the same `deviceId` is used but the **fingerprint hash is different**, the **Device Spoofing** rule triggers (40 risk points).
- **Cyber value:** Cryptographic hashing and hardware identification; spoof detection.

### 8.3 Behavioral Time-Profiling (UEBA)

- **Purpose:** Flag out-of-character behaviour based on when the user usually transacts (User and Entity Behavior Analytics).
- **Flow:** The Fraud Detection Service receives an `unusualTime` flag. The UPI Payment Service computes it from the user’s transaction history: it derives “active hours” (e.g. min/max hour of day from the last 20 transactions, with a small buffer). If the current request’s hour falls outside that window (e.g. user always transacts 9 AM–6 PM but now attempts at 3:30 AM), `unusualTime` is true and **Unusual Time (UEBA)** adds 25 risk points (can combine with Amount Spike for high-value night transfers).
- **Cyber value:** UEBA and anomaly detection.

## 9. Testing payment and fraud behaviour

- **Low risk:** Normal amount, same device and IP as before, no rapid transactions → APPROVE.
- **Amount spike:** Send an amount much larger than your recent average → possible SUSPICIOUS or BLOCK (25 points).
- **New device:** Send a different `deviceId` than used before → 20 points added.
- **Rapid transactions:** Send several payments within 60 seconds → 30 points per evaluation once count ≥ 3.
- **Impossible travel:** Send two payments with locations far apart and timestamps a few minutes apart (e.g. 1000 km in 10 min) → BLOCK (100 points).
- **Device spoofing:** Reuse a known `deviceId` but change browser/device so the fingerprint hash changes → 40 points.
- **Unusual time:** After building history in “active hours”, send a payment at an unusual hour (e.g. 3 AM) → 25 points (UEBA).
- **Admin:** Log in as `admin@upi` and use the Admin UI or `GET /api/admin/fraud-logs` to view flagged transactions and risk details.

This payment and fraud design forms a **multi-layered defense system**, is auditable, and is suitable for extension with more rules or ML-based scoring.
