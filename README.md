# ACME-AIR (Airline Reservation System)

This repository contains a Spring Boot application implementing the backend for a simplified airline reservation system. It demonstrates a contract-first API development approach using OpenAPI 3.0, PostgreSQL integration via Docker, and clean architecture principles.

---

## ✅ Features Implemented

- [x] Contract-first API design using OpenAPI 3.0.0
- [x] Manually implemented DTOs and domain entities
- [x] Docker-based PostgreSQL setup
- [x] Basic flight search capability
- [x] Multi-passenger booking API with validation
- [x] Timezone-aware search and booking using `ZonedDateTime`
- [x] Input validation and structured error responses
- [x] JPA-based persistence layer
- [x] Clean package structure and separation of concerns

---

## 🚫 Features Not Yet Implemented

- [ ] Payment gateway integration (mocked with input validation)
- [ ] User authentication/authorization
- [ ] CI/CD integration (pipeline scaffold only)
- [ ] Full integration test suite using Testcontainers
- [ ] More unit test coverage
---

## 🧠 Assumptions

1. This project is backend-only and does not include a frontend.
2. Each booking request is all-or-nothing. Partial bookings (e.g., one seat locked) are rejected.
3. Payment data is accepted for structural validation, but no actual processing is performed.
4. Time handling uses ISO-8601 format wit

## 🧪 How to Run

### 🐳 Prerequisites

- Java 21
- Docker & Docker Compose
- Gradle 8+ (or use the Gradle Wrapper: `./gradlew`)

### ▶️ Start the Application

```bash
# Start PostgreSQL in Docker
docker-compose up -d

# Start the Spring Boot application
./mvnw spring-boot:run
```


### API Documentation:
➡️ http://localhost:8080/swagger-ui/index.html

### ✅ Run Tests
``` bash
./gradlew test
```
### Test Coverage
- Includes unit tests for core business logic

- Integration test support is scaffolded for future Testcontainers integration

### 🚀 Deployment
- The application can be containerized and deployed using Docker.

### Manual Steps
``` bash

# Build the app
./gradlew bootJar

# Build Docker image
docker build -t airline-reservation .

# Run container
docker run -p 8080:8080 airline-reservation
```
### Automation (Partial)
- Docker build supported
- CI/CD support via GitHub Actions can be integrated (not yet implemented)

### 🧪 Testing Considerations
- Unit tests verify booking, search, and validation logic

- Integration testing (e.g., database + API) is planned but not fully implemented

- End-to-end contract validation can be added using Swagger tools

### 📁 Project Structure
``` bash
src
├── main
│   ├── java
│   │   └── com.reservation
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── repository
│   │       ├── service
│   │       └── exception
│   └── resources
│       ├── application.yml
│       └── api/
└── test
    └── java/com.reservation
 ```

### 📄 API Specification
- Swagger UI: http://localhost:8080/swagger-ui/index.html

- OpenAPI YAML file: src/main/resources/api/airline-reservation-api.yaml

### 📌 Future Enhancements
- CD pipeline via GitHub Actions

- Caching frequent search queries
