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

## 6. Design by Contract: Spring Cloud Contract

### Decision
導入 **Spring Cloud Contract** 實現 Consumer-Driven Contract (CDC) 測試模式。

### Rationale
- Spring Cloud Contract 是 Spring Cloud 生態系統原生方案，與 Spring Boot 3.4.1 完全相容
- 自動產生 Producer 測試和 Consumer 可用的 WireMock stubs
- 確保微服務間 API 契約一致性，防止破壞性變更
- 遵循 Constitution 中的 TDD 原則和 Contract Tests 要求

### Version Compatibility

| 元件 | 版本 |
|------|------|
| Spring Boot | 3.4.1 |
| Spring Cloud | 2024.0.0 (Moorgate) |
| Spring Cloud Contract | 4.2.x (由 BOM 管理) |

### Implementation

#### Producer Service Dependencies (customers, vets, visits)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-contract-verifier</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>spring-mock-mvc</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-contract-maven-plugin</artifactId>
            <extensions>true</extensions>
            <configuration>
                <testFramework>JUNIT5</testFramework>
                <packageWithBaseClasses>
                    org.springframework.samples.petclinic.{service}.contracts
                </packageWithBaseClasses>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### Consumer Service Dependencies (api-gateway)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### Contract DSL Example (Groovy)

```groovy
// src/test/resources/contracts/owners/shouldReturnOwnerById.groovy
import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return owner by ID"

    request {
        method GET()
        url '/owners/1'
        headers {
            accept(applicationJson())
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            firstName: $(consumer('George'), producer(regex('[a-zA-Z]+'))),
            lastName: $(consumer('Franklin'), producer(regex('[a-zA-Z]+'))),
            pets: []
        ])
    }
}
```

#### Contract Directory Structure

```
src/test/resources/contracts/
├── owners/           → OwnersBase.java
│   ├── shouldReturnOwnerById.groovy
│   ├── shouldCreateOwner.groovy
│   └── shouldReturn404WhenNotFound.groovy
├── pets/             → PetsBase.java
│   └── shouldReturnPetTypes.groovy
└── visits/           → VisitsBase.java
    └── shouldCreateVisit.groovy
```

#### Base Test Class Example

```java
package org.springframework.samples.petclinic.customers.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
public class OwnersBase {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private OwnerRepository ownerRepository;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);
        // Setup mock data for contract tests
        Owner owner = new Owner();
        owner.setId(1);
        owner.setFirstName("George");
        owner.setLastName("Franklin");
        when(ownerRepository.findById(1)).thenReturn(Optional.of(owner));
    }
}
```

### CDC Flow

```
1. Consumer 定義需求 → 2. 建立 Contract → 3. Producer 實作
                              ↓
                        4. 自動產生測試
                              ↓
                        5. 發布 Stubs (-stubs.jar)
                              ↓
                        6. Consumer 使用 Stubs 測試
```

### Contract Coverage Plan

| 服務 | 優先級 | Contracts 數量 |
|------|--------|----------------|
| customers-service | P1 | 12 (owners: 6, pets: 6) |
| visits-service | P1 | 6 |
| vets-service | P2 | 2 |
| api-gateway (consumer) | P2 | Uses stubs |

### Alternatives Considered

| Approach | 採用原因 |
|----------|---------|
| Spring Cloud Contract ✅ | Spring 原生，與 Boot 3.4 完全整合 |
| Pact | 需要額外 Pact Broker 基礎設施 |
| OpenAPI Validator | 僅驗證 schema，無法產生 stub |
| WireMock Standalone | 需要手動維護 stub，無法自動驗證 |

### CI/CD Integration

```yaml
# GitHub Actions workflow
name: Contract Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Contract Tests
        run: ./mvnw clean verify
      - name: Publish Stubs
        if: github.ref == 'refs/heads/main'
        run: ./mvnw deploy -DskipTests
```

### Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Contract 維護成本 | 僅針對關鍵 API 建立 contract |
| Base class 複雜 | 使用 package convention 自動對應 |
| Stub 版本不一致 | CI/CD 自動發布，開發使用 + 取最新 |

---

**Version**: 1.1 | **Status**: Research Complete (Updated with Spring Cloud Contract)
