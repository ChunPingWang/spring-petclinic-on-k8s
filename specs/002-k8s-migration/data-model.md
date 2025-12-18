# Data Model: K8s 遷移 (Kubernetes Migration)

**Branch**: `002-k8s-migration` | **Date**: 2025-12-10

## Overview

本文件定義 K8s 遷移的資料模型，主要涵蓋 Kubernetes 資源結構。業務實體（Owner, Pet, Vet, Visit）保持不變。

---

## K8s Resource Model

### Namespace

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: petclinic
  labels:
    name: petclinic
    environment: production
```

### ConfigMap Structure

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: petclinic-config
  namespace: petclinic
data:
  # Database configuration
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/petclinic"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "update"

  # Service ports
  CUSTOMERS_SERVICE_PORT: "8081"
  VETS_SERVICE_PORT: "8083"
  VISITS_SERVICE_PORT: "8082"
  API_GATEWAY_PORT: "8080"
  GENAI_SERVICE_PORT: "8084"

  # Logging
  LOGGING_LEVEL_ROOT: "INFO"
  LOGGING_LEVEL_ORG_SPRINGFRAMEWORK: "INFO"

  # Tracing
  MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: "http://tracing-server:9411/api/v2/spans"
  MANAGEMENT_TRACING_SAMPLING_PROBABILITY: "1.0"
```

### Secret Structure

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: petclinic-secrets
  namespace: petclinic
type: Opaque
stringData:
  # Database credentials
  SPRING_DATASOURCE_USERNAME: "root"
  SPRING_DATASOURCE_PASSWORD: "petclinic"

  # GenAI API keys (optional)
  OPENAI_API_KEY: ""
  AZURE_OPENAI_KEY: ""
  AZURE_OPENAI_ENDPOINT: ""
```

---

## Service Definitions

### Business Services

| Service | Port | Replicas (dev) | Replicas (prod) | Memory |
|---------|------|----------------|-----------------|--------|
| customers-service | 8081 | 1 | 3 | 512Mi |
| vets-service | 8083 | 1 | 3 | 512Mi |
| visits-service | 8082 | 1 | 3 | 512Mi |
| api-gateway | 8080 | 1 | 2 | 512Mi |
| genai-service | 8084 | 1 | 2 | 512Mi |

### Infrastructure Services

| Service | Port | Replicas | Memory | Image |
|---------|------|----------|--------|-------|
| tracing-server | 9411 | 1 | 512Mi | openzipkin/zipkin |
| prometheus | 9090 | 1 | 256Mi | prom/prometheus |
| grafana | 3000 | 1 | 256Mi | grafana/grafana |

---

## Deployment Template

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${SERVICE_NAME}
  namespace: petclinic
  labels:
    app: ${SERVICE_NAME}
    version: "3.4.1"
spec:
  replicas: ${REPLICAS}
  selector:
    matchLabels:
      app: ${SERVICE_NAME}
  template:
    metadata:
      labels:
        app: ${SERVICE_NAME}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "${SERVICE_PORT}"
    spec:
      containers:
      - name: app
        image: springcommunity/${SERVICE_NAME}:latest
        ports:
        - containerPort: ${SERVICE_PORT}
          name: http
        envFrom:
        - configMapRef:
            name: petclinic-config
        - secretRef:
            name: petclinic-secrets
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: ${SERVICE_PORT}
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: ${SERVICE_PORT}
          initialDelaySeconds: 30
          periodSeconds: 5
```

---

## Service Template

```yaml
apiVersion: v1
kind: Service
metadata:
  name: ${SERVICE_NAME}
  namespace: petclinic
  labels:
    app: ${SERVICE_NAME}
spec:
  type: ClusterIP
  selector:
    app: ${SERVICE_NAME}
  ports:
  - port: ${SERVICE_PORT}
    targetPort: http
    protocol: TCP
    name: http
```

---

## Ingress Routes

| Path | Backend Service | Port |
|------|-----------------|------|
| /api/customer/* | api-gateway | 8080 |
| /api/vet/* | vets-service | 8083 |
| /api/visit/* | visits-service | 8082 |
| /api/genai/* | genai-service | 8084 |
| / | api-gateway | 8080 |

---

## Service Communication Matrix

```
┌──────────────────┐     ┌──────────────────┐
│   api-gateway    │────▶│ customers-service│
│     (8080)       │     │     (8081)       │
└────────┬─────────┘     └──────────────────┘
         │
         │               ┌──────────────────┐
         └──────────────▶│  visits-service  │
                         │     (8082)       │
                         └──────────────────┘

┌──────────────────┐
│   All Services   │────▶ tracing-server (9411)
└──────────────────┘

┌──────────────────┐
│   Prometheus     │────▶ All Services /actuator/prometheus
│     (9090)       │
└──────────────────┘
```

---

## Labels Convention

| Label | Purpose | Example |
|-------|---------|---------|
| `app` | Service name | `customers-service` |
| `version` | Application version | `3.4.1` |
| `component` | Component type | `business-service`, `infrastructure` |
| `environment` | Deployment environment | `dev`, `staging`, `prod` |
| `managed-by` | Management tool | `kustomize` |

---

## Resource Quotas (Production)

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: petclinic-quota
  namespace: petclinic
spec:
  hard:
    requests.cpu: "4"
    requests.memory: "8Gi"
    limits.cpu: "8"
    limits.memory: "16Gi"
    pods: "20"
```

---

## Contract Testing Data Model

### Overview

本節定義 Spring Cloud Contract 所需的 API 契約資料模型，涵蓋三個 Producer Services 和一個 Consumer Service。

---

### Contract Directory Structure

```
src/test/resources/contracts/
├── owners/                           # Customers Service - Owner API
│   ├── shouldReturnOwnerById.groovy
│   ├── shouldReturnAllOwners.groovy
│   ├── shouldCreateOwner.groovy
│   ├── shouldUpdateOwner.groovy
│   └── shouldReturn404WhenOwnerNotFound.groovy
├── pets/                             # Customers Service - Pet API
│   ├── shouldReturnPetTypes.groovy
│   ├── shouldReturnPetById.groovy
│   ├── shouldCreatePet.groovy
│   └── shouldUpdatePet.groovy
├── vets/                             # Vets Service
│   └── shouldReturnAllVets.groovy
└── visits/                           # Visits Service
    ├── shouldCreateVisit.groovy
    ├── shouldReturnVisitsForPet.groovy
    └── shouldReturnVisitsForMultiplePets.groovy
```

---

### Contract Data Models

#### Owner Contract Model

```json
{
  "id": 1,
  "firstName": "George",
  "lastName": "Franklin",
  "address": "110 W. Liberty St.",
  "city": "Madison",
  "telephone": "6085551023",
  "pets": [
    {
      "id": 1,
      "name": "Leo",
      "birthDate": "2020-01-01",
      "type": {
        "id": 1,
        "name": "cat"
      }
    }
  ]
}
```

**Field Matchers**:
| Field | Consumer Value | Producer Matcher |
|-------|----------------|------------------|
| id | 1 | `regex('[0-9]+')` |
| firstName | George | `regex('[a-zA-Z]+')` |
| lastName | Franklin | `regex('[a-zA-Z]+')` |
| telephone | 6085551023 | `regex('[0-9]{10}')` |
| birthDate | 2020-01-01 | `regex('\\d{4}-\\d{2}-\\d{2}')` |

#### OwnerRequest Contract Model

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "address": "123 Main St",
  "city": "Springfield",
  "telephone": "5551234567"
}
```

---

#### Pet Contract Model

```json
{
  "id": 1,
  "name": "Leo",
  "birthDate": "2020-01-01",
  "type": {
    "id": 1,
    "name": "cat"
  }
}
```

#### PetType Contract Model

```json
{
  "id": 1,
  "name": "cat"
}
```

**Available PetTypes**:
| ID | Name |
|----|------|
| 1 | cat |
| 2 | dog |
| 3 | lizard |
| 4 | snake |
| 5 | bird |
| 6 | hamster |

---

#### Vet Contract Model

```json
{
  "id": 1,
  "firstName": "James",
  "lastName": "Carter",
  "specialties": [
    {
      "id": 1,
      "name": "radiology"
    }
  ]
}
```

---

#### Visit Contract Model

```json
{
  "id": 1,
  "date": "2025-12-19",
  "description": "annual checkup",
  "petId": 1
}
```

#### Visits (Batch Response) Contract Model

```json
{
  "items": [
    {
      "id": 1,
      "date": "2025-12-19",
      "description": "annual checkup",
      "petId": 1
    }
  ]
}
```

---

### API Gateway Contract Model (Consumer)

API Gateway 作為 Consumer，使用其他服務的 stubs 進行測試。

#### OwnerDetails (Aggregated Response)

```json
{
  "id": 1,
  "firstName": "George",
  "lastName": "Franklin",
  "address": "110 W. Liberty St.",
  "city": "Madison",
  "telephone": "6085551023",
  "pets": [
    {
      "id": 1,
      "name": "Leo",
      "birthDate": "2020-01-01",
      "type": {
        "name": "cat"
      },
      "visits": [
        {
          "id": 1,
          "petId": 1,
          "date": "2025-12-19",
          "description": "annual checkup"
        }
      ]
    }
  ]
}
```

---

### Contract Test Base Classes

| Contract Directory | Base Class | Package |
|-------------------|------------|---------|
| owners/* | OwnersBase | `o.s.s.p.customers.contracts` |
| pets/* | PetsBase | `o.s.s.p.customers.contracts` |
| vets/* | VetsBase | `o.s.s.p.vets.contracts` |
| visits/* | VisitsBase | `o.s.s.p.visits.contracts` |

---

### Stub Artifact Coordinates

| Service | Group ID | Artifact ID | Classifier |
|---------|----------|-------------|------------|
| customers-service | `org.springframework.samples.petclinic.client` | `spring-petclinic-customers-service` | `stubs` |
| vets-service | `org.springframework.samples.petclinic.vets` | `spring-petclinic-vets-service` | `stubs` |
| visits-service | `org.springframework.samples.petclinic.visits` | `spring-petclinic-visits-service` | `stubs` |

---

**Version**: 1.1 | **Status**: Complete (Updated with Contract Testing)
