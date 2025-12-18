# Implementation Plan: Spring Cloud Contract for Design by Contract

**Branch**: `002-k8s-migration` | **Date**: 2025-12-19 | **Spec**: [spec.md](./spec.md)
**Input**: User request to add Spring Cloud Contract for Design by Contract pattern

## Summary

導入 Spring Cloud Contract 實現 Consumer-Driven Contract (CDC) 測試模式，確保微服務間 API 契約一致性。涵蓋 customers-service、vets-service、visits-service 作為 Producer，api-gateway 作為 Consumer。

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.4.1
**Primary Dependencies**: Spring Cloud 2024.0.0, Spring Cloud Contract 4.2.x
**Storage**: N/A (Contract Testing 專注於 API 契約)
**Testing**: JUnit 5, Spring Boot Test, RestAssured MockMvc
**Target Platform**: Kubernetes (K8s)
**Project Type**: Microservices (多模組 Maven 專案)
**Performance Goals**: Contract 測試執行時間 < 30 秒/服務
**Constraints**: 須與現有測試架構整合，不影響業務功能
**Scale/Scope**: 4 個微服務，約 20 個 API 端點

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原則 | 狀態 | 說明 |
|------|------|------|
| 棕地專案原則 | PASS | 使用增量方式加入 contract tests，不影響現有功能 |
| TDD 原則 | PASS | Contract Tests 遵循 TDD 要求 |
| Contract Tests 要求 | PASS | 實現 Constitution 中定義的 contract tests 要求 |
| SOLID 原則 | PASS | 使用 package convention 實現關注點分離 |

## Project Structure

### Documentation (this feature)

```text
specs/002-k8s-migration/
├── plan.md              # 本文件
├── research.md          # Phase 0: Spring Cloud Contract 技術研究
├── data-model.md        # Phase 1: Contract Data Models
├── quickstart.md        # Phase 1: Contract Testing 快速入門
├── contracts/           # Phase 1: Contract 定義範例
│   └── spring-cloud-contract/
│       ├── customers-service/
│       │   ├── owners/
│       │   └── pets/
│       ├── vets-service/
│       │   └── vets/
│       └── visits-service/
│           └── visits/
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
spring-petclinic-customers-service/
├── src/test/resources/contracts/
│   ├── owners/                    # Owner API contracts
│   └── pets/                      # Pet API contracts
└── src/test/java/.../contracts/
    ├── OwnersBase.java           # Base class for owner contracts
    └── PetsBase.java             # Base class for pet contracts

spring-petclinic-vets-service/
├── src/test/resources/contracts/
│   └── vets/                      # Vet API contracts
└── src/test/java/.../contracts/
    └── VetsBase.java             # Base class for vet contracts

spring-petclinic-visits-service/
├── src/test/resources/contracts/
│   └── visits/                    # Visit API contracts
└── src/test/java/.../contracts/
    └── VisitsBase.java           # Base class for visit contracts

spring-petclinic-api-gateway/
└── src/test/java/.../
    └── ApiGatewayContractTest.java  # Consumer contract tests
```

**Structure Decision**: 採用 Spring Cloud Contract 標準結構，contracts 放置於 `src/test/resources/contracts/`，Base classes 放置於對應的 contracts package。

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | - | - |

## Implementation Phases

### Phase 0: Research (Completed)
- 研究 Spring Cloud Contract 與 Spring Boot 3.4.1 相容性
- 定義 Maven 依賴配置
- 設計 Contract 目錄結構

### Phase 1: Design & Contracts (Completed)
- 更新 data-model.md 包含 Contract Data Models
- 建立 Groovy DSL Contract 範例
- 更新 quickstart.md 包含 Contract Testing 指南

### Phase 2: Implementation (Pending - /speckit.tasks)
- 更新各服務 pom.xml 加入依賴
- 複製 contracts 到各服務目錄
- 建立 Base Test Classes
- 執行驗證測試
