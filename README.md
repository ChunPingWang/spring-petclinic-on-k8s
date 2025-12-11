# Spring PetClinic Microservices on Kubernetes

[![Build Status](https://github.com/ChunPingWang/spring-petclinic-on-k8s/actions/workflows/maven-build.yml/badge.svg)](https://github.com/ChunPingWang/spring-petclinic-on-k8s/actions/workflows/maven-build.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A **Kubernetes-native** version of the Spring PetClinic microservices application. This project demonstrates how to deploy a production-ready microservices architecture on Kubernetes with modern cloud-native technologies.

> **Key Changes from Original**: Spring Cloud Config Server, Eureka Discovery Server, and Admin Server have been **removed** in favor of Kubernetes-native solutions (ConfigMap/Secret, K8s DNS Service Discovery, Prometheus + Grafana).

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Deploy to Kubernetes (Recommended)](#deploy-to-kubernetes-recommended)
  - [Run with Docker Compose](#run-with-docker-compose)
  - [Run Locally without Docker](#run-locally-without-docker)
- [Kubernetes Deployment Guide](#kubernetes-deployment-guide)
- [CI/CD Pipeline](#cicd-pipeline)
- [Monitoring and Observability](#monitoring-and-observability)
- [GenAI Chatbot Integration](#genai-chatbot-integration)
- [API Reference](#api-reference)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
                         ┌─────────────────────────────────────────┐
                         │         NGINX Ingress Controller        │
                         │           petclinic.local               │
                         └───────────────────┬─────────────────────┘
                                             │
         ┌───────────────┬───────────────────┼───────────────┬───────────────┐
         │               │                   │               │               │
         ▼               ▼                   ▼               ▼               ▼
   ┌───────────┐   ┌───────────┐   ┌─────────────┐   ┌───────────┐   ┌───────────┐
   │    API    │   │ Customers │   │    Vets     │   │  Visits   │   │  GenAI    │
   │  Gateway  │   │  Service  │   │   Service   │   │  Service  │   │  Service  │
   │   :8080   │   │   :8081   │   │    :8083    │   │   :8082   │   │   :8084   │
   └─────┬─────┘   └─────┬─────┘   └──────┬──────┘   └─────┬─────┘   └─────┬─────┘
         │               │                │               │               │
         │               └────────────────┼───────────────┘               │
         │                                │                               │
         ▼                                ▼                               ▼
   ┌───────────┐                   ┌───────────┐                   ┌───────────┐
   │  Zipkin   │◄──────────────────│   MySQL   │                   │  OpenAI   │
   │  :9411    │    Tracing        │   :3306   │                   │   API     │
   └───────────┘                   └───────────┘                   └───────────┘
         ▲
         │ Metrics
   ┌─────┴─────┐         ┌───────────┐
   │Prometheus │────────►│  Grafana  │
   │   :9090   │         │   :3000   │
   └───────────┘         └───────────┘
```

### Service Communication

| Communication Type | Technology |
|-------------------|------------|
| Service Discovery | Kubernetes DNS (e.g., `http://customers-service:8081`) |
| Configuration | Kubernetes ConfigMap + Secret |
| Load Balancing | Kubernetes Service (ClusterIP) |
| External Access | NGINX Ingress Controller |
| Circuit Breaker | Resilience4j |
| Distributed Tracing | Micrometer + Zipkin |

---

## Technology Stack

| Category | Technology |
|----------|------------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.4.1, Spring Cloud 2024.0.0 |
| **API Gateway** | Spring Cloud Gateway |
| **Database** | MySQL 8.4.5 / HSQLDB (in-memory) |
| **Container Runtime** | Docker, containerd |
| **Orchestration** | Kubernetes (Kind, Minikube, EKS, GKE, AKS) |
| **Configuration Management** | Kustomize |
| **Ingress** | NGINX Ingress Controller |
| **Distributed Tracing** | Micrometer Tracing + Zipkin |
| **Monitoring** | Prometheus + Grafana |
| **AI/LLM** | Spring AI + OpenAI / Azure OpenAI |
| **CI/CD** | GitHub Actions |

---

## Project Structure

```
spring-petclinic-on-k8s/
├── spring-petclinic-api-gateway/       # API Gateway (port 8080)
├── spring-petclinic-customers-service/ # Customers & Pets management (port 8081)
├── spring-petclinic-visits-service/    # Visit records management (port 8082)
├── spring-petclinic-vets-service/      # Veterinarians management (port 8083)
├── spring-petclinic-genai-service/     # AI Chatbot service (port 8084)
├── k8s/                                # Kubernetes manifests
│   ├── base/                           # Base resources (namespace, configmap, secret)
│   ├── services/                       # Service deployments
│   │   ├── api-gateway/
│   │   ├── customers-service/
│   │   ├── vets-service/
│   │   ├── visits-service/
│   │   ├── genai-service/
│   │   ├── mysql/
│   │   └── tracing-server/
│   ├── monitoring/                     # Observability stack
│   │   ├── prometheus/
│   │   └── grafana/
│   ├── ingress/                        # Ingress configuration
│   └── overlays/                       # Kustomize overlays
│       ├── dev/                        # Development environment
│       ├── staging/                    # Staging environment
│       └── prod/                       # Production environment
├── docker-compose.yml                  # Local development with Docker
├── .github/workflows/                  # CI/CD pipeline
│   └── maven-build.yml
└── docs/                               # Documentation
    └── k8s-quickstart.md
```

---

## Quick Start

### Prerequisites

- **Java 17+** and Maven 3.8+
- **Docker** (for building images)
- **kubectl** CLI configured
- **Kind** or other Kubernetes cluster
- (Optional) **OpenAI API Key** for GenAI service

### Deploy to Kubernetes (Recommended)

#### 1. Create Kind Cluster

```bash
cat <<EOF | kind create cluster --name petclinic --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
EOF
```

#### 2. Install NGINX Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

#### 3. Build Docker Images

```bash
# Build all service images
./mvnw clean spring-boot:build-image -pl spring-petclinic-customers-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-vets-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-visits-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-api-gateway -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-genai-service -DskipTests
```

#### 4. Load Images into Kind

```bash
kind load docker-image petclinic/customers-service:latest --name petclinic
kind load docker-image petclinic/vets-service:latest --name petclinic
kind load docker-image petclinic/visits-service:latest --name petclinic
kind load docker-image petclinic/api-gateway:latest --name petclinic
kind load docker-image petclinic/genai-service:latest --name petclinic
```

#### 5. Deploy to Kubernetes

```bash
# Deploy using dev overlay
kubectl apply -k k8s/overlays/dev

# Wait for all pods to be ready
kubectl wait --namespace petclinic \
  --for=condition=ready pod \
  --all \
  --timeout=300s
```

#### 6. Configure /etc/hosts

```bash
echo "127.0.0.1 petclinic.local" | sudo tee -a /etc/hosts
```

#### 7. Access the Application

| Service | URL |
|---------|-----|
| Frontend UI | http://petclinic.local/ |
| Customers API | http://petclinic.local/api/customer/owners |
| Vets API | http://petclinic.local/api/vet/vets |
| Visits API | http://petclinic.local/api/visit/pets/visits?petId=1 |
| Zipkin Tracing | http://petclinic.local/zipkin |
| Grafana | http://petclinic.local/grafana (admin/admin) |

For Prometheus, use port-forward:
```bash
kubectl port-forward -n petclinic svc/prometheus 9090:9090
# Access http://localhost:9090
```

### Run with Docker Compose

```bash
# Build images
./mvnw clean spring-boot:build-image -DskipTests

# Start all services
docker compose up -d

# Check status
docker compose ps
```

Access at http://localhost:8080

### Run Locally without Docker

Each service can run standalone:

```bash
# Start each service in separate terminals
cd spring-petclinic-customers-service && ../mvnw spring-boot:run
cd spring-petclinic-vets-service && ../mvnw spring-boot:run
cd spring-petclinic-visits-service && ../mvnw spring-boot:run
cd spring-petclinic-api-gateway && ../mvnw spring-boot:run
```

| Service | URL |
|---------|-----|
| API Gateway (Frontend) | http://localhost:8080 |
| Customers Service | http://localhost:8081 |
| Visits Service | http://localhost:8082 |
| Vets Service | http://localhost:8083 |
| GenAI Service | http://localhost:8084 |

---

## Kubernetes Deployment Guide

### Kustomize Overlays

The project uses **Kustomize** for environment-specific configurations:

| Overlay | Description | Command |
|---------|-------------|---------|
| `dev` | Development (1 replica, lower resources) | `kubectl apply -k k8s/overlays/dev` |
| `staging` | Staging (2 replicas) | `kubectl apply -k k8s/overlays/staging` |
| `prod` | Production (3 replicas, higher resources) | `kubectl apply -k k8s/overlays/prod` |

### Useful kubectl Commands

```bash
# Check pod status
kubectl get pods -n petclinic

# View logs
kubectl logs -n petclinic deployment/customers-service

# Restart a service
kubectl rollout restart deployment/customers-service -n petclinic

# Scale a service
kubectl scale deployment/customers-service --replicas=3 -n petclinic

# Delete all resources
kubectl delete -k k8s/overlays/dev
```

### Health Checks

All services expose health endpoints:

```bash
# Liveness probe
curl http://localhost:8081/actuator/health/liveness

# Readiness probe
curl http://localhost:8081/actuator/health/readiness
```

---

## CI/CD Pipeline

The project uses **GitHub Actions** for CI/CD:

### Workflow: `.github/workflows/maven-build.yml`

| Trigger | Actions |
|---------|---------|
| Push to `main` | Build, Test, Validate K8s manifests, Build & Push Docker images |
| Pull Request to `main` | Build, Test, Validate K8s manifests |

### Pipeline Stages

1. **Build & Test**: `mvn -B package`
2. **Validate K8s Manifests**:
   ```bash
   kubectl kustomize k8s/overlays/dev > /tmp/k8s-dev.yaml
   kubectl apply --dry-run=client -f /tmp/k8s-dev.yaml
   ```
3. **Build Docker Images**: Using Spring Boot's `build-image` plugin
4. **Push to GHCR**: Push images to GitHub Container Registry (on main branch)

### Container Registry

Images are published to: `ghcr.io/<owner>/petclinic/<service-name>:latest`

---

## Monitoring and Observability

### Distributed Tracing (Zipkin)

All services are instrumented with Micrometer Tracing:

- Access Zipkin UI: http://petclinic.local/zipkin
- Trace correlation across all microservices
- Request latency analysis

### Metrics (Prometheus + Grafana)

**Prometheus** scrapes metrics from all services:
- JVM metrics (memory, GC, threads)
- HTTP request metrics
- Custom business metrics

**Grafana** dashboards available at http://petclinic.local/grafana:
- Default credentials: admin/admin
- Pre-configured Prometheus datasource

### Custom Metrics

| Service | Metrics |
|---------|---------|
| customers-service | `petclinic.owner`, `petclinic.pet` |
| visits-service | `petclinic.visit` |

---

## GenAI Chatbot Integration

The GenAI service provides a natural language interface:

### Example Queries

- "List all owners"
- "Are there any vets that specialize in surgery?"
- "Add a dog named Max for owner Betty"
- "Which owners have cats?"

### Configuration

**Offline Mode** (default for K8s deployment):
```yaml
genai:
  offline-mode: true  # Returns "服務暫時離線" for all requests
```

**Online Mode** (requires OpenAI API key):
```bash
export OPENAI_API_KEY="your_api_key_here"
# Or for Azure OpenAI:
export AZURE_OPENAI_ENDPOINT="https://your_resource.openai.azure.com"
export AZURE_OPENAI_KEY="your_api_key_here"
```

---

## API Reference

### Customers Service (port 8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/owners` | List all owners |
| GET | `/owners/{id}` | Get owner by ID |
| POST | `/owners` | Create new owner |
| PUT | `/owners/{id}` | Update owner |
| GET | `/owners/{id}/pets` | Get owner's pets |
| POST | `/owners/{id}/pets` | Add pet to owner |

### Vets Service (port 8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/vets` | List all veterinarians |

### Visits Service (port 8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/pets/visits?petId={id}` | Get visits for a pet |
| POST | `/owners/{ownerId}/pets/{petId}/visits` | Create visit |

### GenAI Service (port 8084)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/chatclient` | Send chat message |

---

## Troubleshooting

### Pods stuck in Pending

```bash
# Check if images are loaded
docker images | grep petclinic
kind load docker-image <image-name> --name petclinic
```

### Pods in CrashLoopBackOff

```bash
kubectl logs -n petclinic <pod-name>
kubectl describe pod -n petclinic <pod-name>
```

### Ingress not working

```bash
# Verify Ingress controller is running
kubectl get pods -n ingress-nginx

# Check Ingress resource
kubectl describe ingress -n petclinic

# Verify /etc/hosts entry
cat /etc/hosts | grep petclinic
```

### Database connection issues

```bash
# Check if MySQL is running
kubectl get pods -n petclinic | grep mysql
kubectl logs -n petclinic deployment/mysql
```

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m 'Add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- Original [Spring PetClinic Microservices](https://github.com/spring-petclinic/spring-petclinic-microservices)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Kubernetes](https://kubernetes.io/)
