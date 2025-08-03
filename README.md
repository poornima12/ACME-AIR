# ACME-AIR (Airline Reservation System)

This repository contains a Spring Boot application implementing the backend for a simplified airline reservation system. It demonstrates a contract-first API development approach using OpenAPI 3.0, PostgreSQL integration via Docker, and clean architecture principles **designed for production longevity and scalability**.

---

## ✅ APIs Implemented (2 out of 5 required)

**From the 5 required API endpoints, the following 2 have been fully implemented:**
- [x] **Flight Search API** - Search for available flights with comprehensive filtering
- [x] **Booking API** - Create and save passenger flight bookings with multi-passenger support

## ✅ Features Implemented

### 🏗️ Application Infrastructure
- [x] **Contract-first API design** using OpenAPI 3.0.0 with Swagger UI
- [x] **OpenAPI Code Generation** integrated for single source of truth
- [x] **Automated DTO generation** from OpenAPI contract (replacing manually implemented DTOs)
- [x] **Docker-based PostgreSQL** setup with Docker Compose
- [x] **Timezone-aware operations** using `ZonedDateTime` for search and booking
- [x] **Robust input validation** and structured error responses with custom exception handling
- [x] **JPA-based persistence layer** with Flight and Booking entities
- [x] **Clean package structure** and separation of concerns
- [x] **Containerization** with Dockerfile for CI
- [x] **Setup automation** with sample data for testing
- [x] **GitHub Actions** build validation pipeline on GitHub

### 🔍 Flight Search API Features
- [x] **Flight search capability** with timezone normalization and request validation
- [x] **Origin and destination filtering** with airport code validation
- [x] **Date-based search** with flexible date range support
- [x] **Passenger count filtering** to show only flights with sufficient capacity
- [x] **One-way trip support** with comprehensive flight details

### ✈️ Booking API Features
- [x] **Multi-passenger booking API** with comprehensive validation and edge case handling
- [x] **Atomic seat booking**: Lock selected seats, verify all available, then either book all together or fail with conflict if any unavailable
- [x] **Seat availability validation** with real-time conflict detection
- [x] **Payment data validation** (structural validation only, no processing)
- [x] **Booking confirmation** with unique booking reference generation
- [x] **EdgeCase scenarios** Double booking, seat unavailability

---

## 🧪 Testing Implementation

- [x] **Unit tests** for booking and flight service logic
- [x] **Component tests** for API endpoints using MockMvc
- [x] **Integration tests** using Test containers for edge cases
- [x] **OpenAPI contract validation** in component tests
- [x] **Edge case coverage** including validation failures and conflict scenarios

---

## 🚫 Features Not Yet Implemented

- [ ] Payment gateway integration (currently mocked with input validation only)
- [ ] User authentication/authorization system
- [ ] Caching for frequent search queries
- [ ] Full CI/CD deployment pipeline (build validation implemented, deployment pending)
- [ ] **File-based logging configuration** (currently console-only for development)
- [ ] **Round-trip search functionality** - requires complex flight combination logic and return journey optimization

---

## 🧠 Assumptions

1. This project is **backend-only** and does not include a frontend
2. Each booking request is **all-or-nothing** - partial bookings are rejected if any seat is unavailable
3. Payment data is accepted for **structural validation only** - no actual payment processing
4. **Testcontainers** provide realistic database integration testing without external dependencies
5. **Session ID is hardcoded** - production implementation requires proper session management post-authentication
6. **Seat locks don't auto-expire** - background scheduler needed to prevent indefinite seat holds
---

## 🧪 How to Run

### 🐳 Prerequisites

- Java 21
- Docker & Docker Compose
- Gradle 8+ (or use the Gradle Wrapper: `./gradlew`)

### ▶️ Quick start Application

```bash
# First time setup (Loads docker postgresDB + app + loads sample data)
./setup.sh

# Subsequent starts (no data loading)
docker-compose up -d

# API will be available at: http://localhost:8080
```

### 📊 API Documentation
➡️ **Swagger UI**: http://localhost:8080/swagger-ui/index.html  
➡️ **API Base**: http://localhost:8080/api/v1

### ✅ Run Tests

```bash
# Run all tests (unit + component + integration)
./gradlew test

# Run only unit tests
./gradlew test --tests "*Test"

# Run integration tests with Testcontainers
./gradlew test --tests "*IntegrationTest"
```

### 📈 Test Coverage Status
- ✅ **Unit tests**: Core business logic (booking, flight search, validation)
- ✅ **Component tests**: API endpoint behavior with MockMvc
- ✅ **Integration tests**: End-to-end scenarios with Testcontainers including:
    - Double booking prevention
    - Seat availability edge cases
    - Database transaction handling
- ✅ **Contract validation**: OpenAPI specification compliance

---

## 🚀 Deployment

### 🔄 CI/CD Status
- ✅ **GitHub Actions**: Build validation and testing
- 🚧 **Deployment pipeline**: Planned for future implementation

---

## 📁 Project Structure

```bash
src
├── main
│   ├── java
│   │   └── com.reservation
│   │       ├── controller/          # REST API controllers
│   │       ├── dto/                 # Generated from OpenAPI contract
│   │       ├── entity/              # JPA entities (Flight, Booking, BookingItem)
│   │       ├── repository/          # Data access layer
│   │       ├── service/             # Business logic
│   │       └── exception/           # Custom exception handling
│   └── resources
│       ├── application.yml          # Application configuration
│       └── api/
│           └── airline-reservation-api.yaml  # OpenAPI contract
└── test
    └── java/com.reservation
        ├── unit/                    # Unit tests
        ├── component/               # API component tests  
        └── integration/             # Testcontainers integration tests
```

---

## 📄 API Specification

- **Interactive Documentation**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI Contract**: `src/main/resources/api/airline-reservation-api.yaml`
- **Code Generation**: DTOs and interfaces generated from contract during build
- **API Versioning**: All endpoints under `/api/v1`

---

## 🔧 Development Workflow

1. **Contract-First**: Modify OpenAPI specification first
2. **Code Generation**: Build process generates DTOs automatically
3. **Implementation**: Implement business logic in services
4. **Testing**: Write unit, component, and integration tests
5. **Validation**: Swagger UI for manual testing

---

## 📌 Future Enhancements

- **Performance**: Implement caching for frequent flight searches
- **Resilience**: Add rate limiting and circuit breaker patterns
- **Security**: Implement JWT-based authentication and authorization
- **Monitoring**: Add application metrics and health checks
- **Deployment**: Complete CI/CD pipeline with staging and production environments
- **Documentation**: API usage examples and developer onboarding guide