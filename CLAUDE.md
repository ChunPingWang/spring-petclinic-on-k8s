# spring-petclinic-microservices-4-k8s Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-12-19

## Active Technologies

- Java 17 + Spring Boot 3.4.1, Spring Cloud 2024.0.0 (002-k8s-migration)
- Spring Cloud Contract 4.2.x for Design by Contract (002-k8s-migration)
- Micrometer, OpenTelemetry for observability (002-k8s-migration)

## Project Structure

```text
spring-petclinic-microservices/
├── spring-petclinic-customers-service/  # Owner, Pet APIs
├── spring-petclinic-vets-service/       # Vet APIs
├── spring-petclinic-visits-service/     # Visit APIs
├── spring-petclinic-api-gateway/        # API aggregation
├── spring-petclinic-genai-service/      # AI chatbot
└── k8s/                                 # Kubernetes manifests
```

## Commands

```bash
# Build all services
./mvnw clean package -DskipTests

# Build Docker images
./mvnw clean package -DskipTests -PbuildDocker

# Run contract tests
./mvnw clean test

# Run specific service contract tests
./mvnw clean test -pl spring-petclinic-customers-service

# Generate and install stubs
./mvnw clean install

# Deploy to Kubernetes
kubectl apply -k k8s/overlays/dev
```

## Code Style

- Java 17: Follow standard conventions
- Use Spring Cloud Contract Groovy DSL for contracts
- Base test classes follow package convention

## Recent Changes

- 002-k8s-migration: Added Spring Cloud Contract 4.2.x for Design by Contract
- 002-k8s-migration: Added K8s migration from Spring Cloud to native K8s

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
