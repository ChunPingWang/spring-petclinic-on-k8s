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

**Version**: 1.0 | **Status**: Complete
