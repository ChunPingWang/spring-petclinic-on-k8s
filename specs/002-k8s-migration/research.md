# Research: K8s 遷移 (Kubernetes Migration)

**Branch**: `002-k8s-migration` | **Date**: 2025-12-10 | **Plan**: [plan.md](./plan.md)

## Executive Summary

本文件研究 Spring Boot 微服務從 Spring Cloud 遷移到 Kubernetes 原生方案的最佳實踐。涵蓋五個關鍵遷移主題。

---

## 1. Service Discovery: Eureka → K8s Service DNS

### Decision
使用 **Kubernetes Service DNS** 進行服務間通訊。

### Rationale
- K8s 提供原生服務發現，無額外成本
- 服務建立時自動註冊到 K8s DNS
- 消除 Eureka 叢集維護開銷
- 減少記憶體消耗（Eureka 需要 512MB/instance）

### Implementation
```yaml
# 服務間呼叫使用 K8s Service DNS 名稱
# customers-service → http://vets-service:8083/api/vet
```

```java
// 直接使用 RestTemplate 呼叫 K8s Service
restTemplate.getForObject(
    "http://customers-service:8081/owners/{id}",
    Owner.class, id);
```

### Alternatives Considered
| Approach | 採用原因 |
|----------|---------|
| K8s Service DNS ✅ | 無依賴、原生、簡單 |
| Spring Cloud Kubernetes DiscoveryClient | 額外依賴，過於複雜 |
| Service Mesh (Istio) | 學習曲線陡峭，非必要 |

---

## 2. Configuration: Config Server → ConfigMap/Secret

### Decision
使用 **K8s ConfigMap** 存放非敏感配置，**K8s Secret** 存放敏感資料。

### Rationale
- ConfigMap 是 K8s 原生資源，支援 RBAC
- Secret 可在 etcd 中加密
- 消除 Config Server 單點故障風險
- Spring Boot 3.4+ 原生支援 K8s 整合

### Implementation
```yaml
# k8s/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: petclinic-config
data:
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql:3306/petclinic"
  LOGGING_LEVEL_ROOT: "INFO"
---
apiVersion: v1
kind: Secret
metadata:
  name: petclinic-secrets
type: Opaque
stringData:
  SPRING_DATASOURCE_PASSWORD: "petclinic"
  OPENAI_API_KEY: "sk-..."
```

### Alternatives Considered
| Approach | 採用原因 |
|----------|---------|
| ConfigMap + Secret ✅ | K8s 原生，無額外設施 |
| Spring Cloud Kubernetes Config | Bootstrap.yml 已棄用 |
| HashiCorp Vault | 過度複雜 |

---

## 3. API Gateway: Spring Cloud Gateway → K8s Ingress

### Decision
使用 **NGINX Ingress Controller** 處理路由，保留 **精簡版 API Gateway** 僅處理聚合。

### Rationale
- K8s Ingress 提供宣告式 HTTP 路由
- 移除 Gateway 512MB+ 記憶體開銷
- 將路由（基礎設施）與聚合（業務）分離
- NGINX Ingress Controller 穩定可靠

### Implementation
```yaml
# k8s/ingress/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: petclinic-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: petclinic.local
    http:
      paths:
      - path: /api/customer
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
      - path: /api/vet
        pathType: Prefix
        backend:
          service:
            name: vets-service
            port:
              number: 8083
```

### Alternatives Considered
| Approach | 採用原因 |
|----------|---------|
| NGINX Ingress ✅ | 穩定、廣泛採用（至 2026 年） |
| Gateway API | 尚未成熟，2026+ 考慮 |
| 保留 Spring Cloud Gateway | 維持複雜度，不建議 |

---

## 4. Monitoring: Admin Server → Prometheus + Grafana

### Decision
移除 Spring Boot Admin Server，部署 **Prometheus + Grafana** 到 K8s。

### Rationale
- 消除 Admin Server 512MB 開銷
- Prometheus 是 CNCF 標準監控方案
- Grafana 提供優秀的視覺化
- 專案已有 `micrometer-registry-prometheus` 依賴
- docker-compose 中已有 Prometheus + Grafana 配置

### Implementation
```yaml
# Prometheus annotations for service discovery
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: "/actuator/prometheus"
    prometheus.io/port: "8081"
```

### Alternatives Considered
| Approach | 採用原因 |
|----------|---------|
| Prometheus + Grafana ✅ | 開源、CNCF 標準 |
| Datadog / New Relic | SaaS 成本 |
| 保留 Admin Server | 記憶體開銷，不建議 |

---

## 5. K8s Manifest Structure: Kustomize

### Decision
使用 **Kustomize** 組織 K8s manifests，採用 **base/overlay 模式**。

### Rationale
- Kustomize 是 kubectl 原生（無需額外工具）
- 支援 DRY 原則
- 便於環境差異化配置（dev/staging/prod）
- YAML 原生，無需學習模板語言

### Implementation
```
k8s/
├── bases/
│   ├── kustomization.yaml
│   └── services/
│       ├── customers-service/
│       ├── vets-service/
│       └── ...
├── overlays/
│   ├── dev/
│   │   └── kustomization.yaml
│   └── prod/
│       └── kustomization.yaml
└── monitoring/
    ├── prometheus/
    └── grafana/
```

### Alternatives Considered
| Approach | 採用原因 |
|----------|---------|
| Kustomize ✅ | kubectl 原生，YAML 基礎 |
| Helm Charts | 過度複雜，非必要 |
| Plain YAML | 難以維護 |

---

## Dependencies to Remove

從每個業務服務移除：
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Service DNS 解析失敗 | 使用 FQDN: service.namespace.svc.cluster.local |
| Secret 意外提交 Git | .gitignore, Sealed Secrets |
| Prometheus 抓取失敗 | 驗證 annotations，使用 port-forward 測試 |
| Ingress Controller 未安裝 | 部署時包含 NGINX Ingress |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| 部署時間 | < 5 分鐘 |
| 服務延遲增加 | < 10% |
| API 相容性 | 100% |
| 監控覆蓋率 | 100% 服務 |

---

**Version**: 1.0 | **Status**: Research Complete
