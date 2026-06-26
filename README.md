# Event Ledger Microservices

## Overview

This project implements a simple event ledger using two independent Spring Boot microservices:

1. **Event Gateway Service**
2. **Account Service**

Both services run independently with their own in-memory H2 database and communicate through REST APIs.

---

# Architecture Overview

## Event Gateway Service

Responsibilities:

* Accepts client event requests.
* Performs request validation.
* Stores received events in its local H2 database.
* Implements idempotency by checking duplicate event IDs.
* Invokes the Account Service to apply transactions.
* Retrieves account balances from the Account Service.
* Propagates trace IDs across service calls.
* Implements resiliency using Retry and Circuit Breaker.

Main APIs

* POST `/events`
* GET `/events/{eventId}`
* GET `/events?account={accountId}`
* GET `/balance/{accountId}`
* GET `/health`

---

## Account Service

Responsibilities:

* Maintains account balances.
* Stores transaction history.
* Creates accounts automatically when they do not exist.
* Handles duplicate transaction requests.
* Computes account balances.

Main APIs

* POST `/accounts/{accountId}/transactions`
* GET `/accounts/{accountId}/balance`
* GET `/accounts/{accountId}`
* GET `/health`

---

# Service Interaction

```text
                Client
                  │
                  ▼
        Event Gateway Service
                  │
        REST API (HTTP)
                  │
                  ▼
          Account Service
                  │
            H2 Database
```

The Event Gateway stores event information locally and delegates balance computation to the Account Service.

Both services maintain independent databases and communicate only through REST APIs.

---

# Technologies

* Java 17
* Spring Boot
* Spring Web
* Spring Data JPA
* H2 Database
* Hibernate
* Bean Validation
* Micrometer Tracing / OpenTelemetry
* Resilience4j
* Spring Retry
* Spring Boot Actuator
* Swagger / OpenAPI
* JUnit 5
* Mockito
* MockMvc

---

# Project Structure

```
event-gateway/
account-service/
```

Each service is independently buildable and deployable.

---

# Setup Instructions

## Prerequisites

* Java 17
* Maven 3.9+
* Git

---

## Clone

```bash
git clone git@github.com:kiranmayi889/account-service.git
git clone git@github.com:kiranmayi889/event-gateway.git

git hub - https://github.com/kiranmayi889 

```

---

## Build (LINUX)

### Event Gateway

```bash
cd event-gateway
mvn clean install
```

### Account Service

```
cd account-service
mvn clean install
```

---

# Running the Services (LINUX)

## Start Account Service

```bash
cd account-service

mvn spring-boot:run
```

```bash
cd event-gateway

mvn spring-boot:run
```

---


## Build (STS)

import existing maven project
select event gateway and account service folders
after import -> run as maven build -> maven update -> build
start as spring boot application
```

---

Account Service Runs on

```
http://localhost:8081
```


Event Gateway Runs on 

```
http://localhost:8080
```

The Event Gateway communicates with the Account Service using:

```
http://localhost:8081
```

---

# Swagger

Event Gateway

```
http://localhost:8080/swagger-ui/index.html
api docs - http://localhost:8080/v3/api-docs
```

Account Service

```
http://localhost:8081/swagger-ui/index.html
api docs - http://localhost:8081/v3/api-docs
```

---

# H2 Console

Gateway

```
http://localhost:8080/h2-console
```

Account Service

```
http://localhost:8081/h2-console
```

---

# Running Tests

Run all tests

```bash
mvn test
```

Tests include:

* Unit Tests
* Controller Tests
* Integration Tests
* Trace Propagation Tests
* Resiliency Tests

---

# Trace Propagation

The Gateway generates or retrieves the current trace using Micrometer Tracing/OpenTelemetry.

The trace ID is propagated to the Account Service using the HTTP header:

```
X-Trace-Id
```

Both services include the trace ID in their structured logs.

---

# Observability

The application exposes metrics using Micrometer.

Custom metrics include:

* gateway.events.requests

* gateway.events.success

* gateway.events.failed

* gateway.events.duplicate

* account.transactions.requests

* account.transactions.success

* account.transactions.failed

* account.transactions.duplicate

Health endpoints are exposed through Spring Boot Actuator.
example: http://localhost:8080/actuator/metrics/gateway.events.requests - GET
---

# Resiliency Pattern

The Gateway uses **Retry** together with **Circuit Breaker**.

### Retry

Transient failures such as temporary network interruptions or short service outages are handled using Retry.

The Gateway retries failed Account Service requests before returning an error.

### Circuit Breaker

If repeated failures occur, the Circuit Breaker opens and temporarily stops sending requests to the Account Service.

Benefits:

* Prevents cascading failures.
* Reduces unnecessary network traffic.
* Allows the downstream service time to recover.
* Improves application responsiveness during outages.

Once the configured wait duration expires, the Circuit Breaker transitions to HALF_OPEN to determine whether the Account Service has recovered.

---

# Idempotency

Both services support idempotent processing.

Duplicate requests with the same Event ID are detected and ignored, preventing duplicate balance updates.

---

# Event Ordering

Events may arrive out of chronological order.

Transactions are stored independently of arrival order and are returned ordered by Event Timestamp.

Balances are computed correctly regardless of event arrival order.

---

# Logging

Structured JSON logging is implemented.

Each log entry includes:

* Timestamp
* Log Level
* Service Name
* Trace ID
* Message

This enables end-to-end request tracing across both services.

---

# Assumptions

* Event IDs are globally unique.
* Amounts are positive values.
* Supported transaction types are CREDIT and DEBIT.
* Each service owns its own database.
* Services communicate only through REST APIs.
