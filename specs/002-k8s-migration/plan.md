# Implementation Plan: K8s 遷移 (Kubernetes Migration)

**Branch**: `002-k8s-migration` | **Date**: 2025-12-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-k8s-migration/spec.md`

## Summary

將 Spring PetClinic Microservices 從 Spring Cloud 基礎設施遷移到 Kubernetes 原生方案。主要工作包括：
1. 移除 Eureka Discovery Server，改用 K8s Service Discovery
2. 移除 Spring Cloud Config Server，改用 K8s ConfigMap/Secret
3. 移除 Admin Server，改用 Prometheus + Grafana（已存在於 docker-compose）
4. 重構 API Gateway，保留 API 聚合功能，路由改用 K8s Ingress
5. 保留 Tracing Server (Zipkin) 和 GenAI Service，部署到 K8s

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.4.1, Spring Cloud 2024.0.0 (將移除部分), Micrometer, OpenTelemetry
**Storage**: MySQL (production), HSQLDB (testing)
**Testing**: JUnit 5, AssertJ, Spring Boot Test
**Target Platform**: Kubernetes (minikube/kind/Docker Desktop K8s for local dev)
**Project Type**: Multi-module Maven microservices
**Performance Goals**: 部署時間 <5 分鐘，服務間延遲增加 <10%
**Constraints**: 保持現有 REST API 100% 相容，棕地專案原則
**Scale/Scope**: 6 個保留服務 + 2 個新增監控服務 (Prometheus/Grafana)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| 0. 棕地專案原則 | ✅ PASS | 遵循漸進式改進，保留所有 REST API 端點，使用 Strangler Fig 模式 |
| I. 程式碼品質 | ✅ PASS | 移除不需要的依賴，簡化配置 |
| II. TDD | ✅ PASS | 將為 K8s 配置撰寫整合測試 |
| III. BDD | ✅ PASS | 驗收場景已在 spec.md 定義 |
| IV. DDD | ✅ PASS | 業務邏輯不受影響，僅變更基礎設施層 |
| V. SOLID | ✅ PASS | 無違反 |
| VI. 六角形架構 | ✅ PASS | 變更僅在基礎設施層（K8s 配置） |
| VII. 依賴反轉 | ✅ PASS | 移除 Spring Cloud 依賴，改用環境變數配置 |
| Git Commit Policy | ✅ PASS | 將遵循 conventional commits，無 AI 標記 |

## Project Structure

### Documentation (this feature)

```text
specs/002-k8s-migration/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (K8s manifest 結構)
├── quickstart.md        # Phase 1 output (K8s 部署指南)
├── contracts/           # Phase 1 output (K8s YAML schemas)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
# Current microservices structure (to be modified)
spring-petclinic-admin-server/        # 將移除
spring-petclinic-api-gateway/         # 將重構（保留聚合功能）
spring-petclinic-config-server/       # 將移除
spring-petclinic-customers-service/   # 移除 Spring Cloud 依賴
spring-petclinic-discovery-server/    # 將移除
spring-petclinic-genai-service/       # 移除 Spring Cloud 依賴
spring-petclinic-vets-service/        # 移除 Spring Cloud 依賴
spring-petclinic-visits-service/      # 移除 Spring Cloud 依賴

# New K8s deployment structure (to be created)
k8s/
├── base/                             # 基礎配置
│   ├── namespace.yaml
│   ├── configmap.yaml               # 共用配置
│   └── secret.yaml                  # 敏感資訊
├── services/
│   ├── customers-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── vets-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── visits-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── api-gateway/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── genai-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   └── tracing-server/
│       ├── deployment.yaml
│       └── service.yaml
├── monitoring/
│   ├── prometheus/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── configmap.yaml
│   └── grafana/
│       ├── deployment.yaml
│       ├── service.yaml
│       └── configmap.yaml
├── ingress/
│   └── ingress.yaml                  # 統一入口路由
└── kustomization.yaml                # Kustomize 管理

# Docker configurations (existing, may need updates)
docker/
├── grafana/
└── prometheus/
```

**Structure Decision**: 採用 Kustomize 管理 K8s manifests，便於環境差異化配置（dev/staging/prod）。保留現有 Maven 多模組結構，僅移除不需要的模組。

## Complexity Tracking

> No violations requiring justification. The migration follows brownfield principles with incremental changes.

## Key Technical Decisions

### 1. Service Discovery Migration

**Current**: Eureka Server + Eureka Client
**Target**: K8s Service DNS
**Approach**:
- 移除 `spring-cloud-starter-netflix-eureka-client` 依賴
- 服務間呼叫改用 K8s Service DNS 名稱（如 `http://customers-service:8081`）
- 使用 Spring `RestClient` 或 `WebClient` 直接呼叫

### 2. Configuration Migration

**Current**: Spring Cloud Config Server + Git repository
**Target**: K8s ConfigMap + Secret
**Approach**:
- 移除 `spring-cloud-starter-config` 依賴
- 配置從 ConfigMap 掛載為環境變數或配置檔
- 敏感資訊（DB 密碼）存於 Secret

### 3. API Gateway Refactoring

**Current**: Spring Cloud Gateway (路由 + 聚合)
**Target**: 精簡版 Gateway (僅聚合) + K8s Ingress (路由)
**Approach**:
- 移除 Gateway 中的路由配置
- 保留 Owner Details 聚合 API（整合 Customers + Visits）
- 路由功能由 K8s Ingress 接管

### 4. Monitoring Migration

**Current**: Spring Boot Admin Server
**Target**: Prometheus + Grafana (已存在於 docker-compose)
**Approach**:
- 移除 Admin Server 模組
- 已有 `micrometer-registry-prometheus` 依賴
- 建立 K8s Deployment 給 Prometheus/Grafana

## Removed Components

| Component | Reason | K8s Replacement |
|-----------|--------|-----------------|
| spring-petclinic-discovery-server | Eureka → K8s Service | K8s Service DNS |
| spring-petclinic-config-server | Config Server → ConfigMap | K8s ConfigMap/Secret |
| spring-petclinic-admin-server | Admin → Prometheus | Prometheus + Grafana |

## Dependencies to Remove (per service)

```xml
<!-- Remove from each business service pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

## Next Steps

1. **Phase 0**: 完成 research.md - 研究 K8s 最佳實踐
2. **Phase 1**: 完成 data-model.md, contracts/, quickstart.md
3. **Phase 2**: 執行 `/speckit.tasks` 產生任務清單
