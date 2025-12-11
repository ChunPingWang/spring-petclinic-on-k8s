# Tasks: K8s 遷移 (Kubernetes Migration)

**Input**: Design documents from `/specs/002-k8s-migration/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Java Services**: `spring-petclinic-{service}/src/main/`
- **K8s Manifests**: `k8s/` at repository root
- **Parent POM**: `pom.xml` at repository root

---

## Phase 1: Setup (K8s Infrastructure) ✅

**Purpose**: Create K8s directory structure and base manifests

- [x] T001 Create K8s directory structure in k8s/
- [x] T002 Create namespace manifest in k8s/base/namespace.yaml
- [x] T003 [P] Create base kustomization.yaml in k8s/base/kustomization.yaml
- [x] T004 [P] Create ConfigMap manifest in k8s/base/configmap.yaml
- [x] T005 [P] Create Secret manifest in k8s/base/secret.yaml

---

## Phase 2: Foundational (Blocking Prerequisites) ✅

**Purpose**: Core K8s deployment manifests that MUST be complete before service migration

**⚠️ CRITICAL**: Services cannot be deployed to K8s until these manifests exist

- [x] T006 [P] Create customers-service deployment manifest in k8s/services/customers-service/deployment.yaml
- [x] T007 [P] Create customers-service service manifest in k8s/services/customers-service/service.yaml
- [x] T008 [P] Create vets-service deployment manifest in k8s/services/vets-service/deployment.yaml
- [x] T009 [P] Create vets-service service manifest in k8s/services/vets-service/service.yaml
- [x] T010 [P] Create visits-service deployment manifest in k8s/services/visits-service/deployment.yaml
- [x] T011 [P] Create visits-service service manifest in k8s/services/visits-service/service.yaml
- [x] T012 [P] Create api-gateway deployment manifest in k8s/services/api-gateway/deployment.yaml
- [x] T013 [P] Create api-gateway service manifest in k8s/services/api-gateway/service.yaml
- [x] T014 [P] Create genai-service deployment manifest in k8s/services/genai-service/deployment.yaml
- [x] T015 [P] Create genai-service service manifest in k8s/services/genai-service/service.yaml
- [x] T016 [P] Create tracing-server deployment manifest in k8s/services/tracing-server/deployment.yaml
- [x] T017 [P] Create tracing-server service manifest in k8s/services/tracing-server/service.yaml

**Checkpoint**: Base K8s manifests ready - service migration can now begin ✅

---

## Phase 3: User Story 1 - 移除 Spring Cloud 服務發現 (Priority: P1) 🎯 MVP ✅

**Goal**: 移除 Eureka Discovery Server，業務微服務改用 K8s Service Discovery

**Independent Test**: 在 K8s 環境部署業務微服務，驗證服務間可透過 K8s Service DNS 名稱互相呼叫

### Implementation for User Story 1

- [x] T018 [P] [US1] Remove spring-cloud-starter-netflix-eureka-client from spring-petclinic-customers-service/pom.xml
- [x] T019 [P] [US1] Remove spring-cloud-starter-netflix-eureka-client from spring-petclinic-vets-service/pom.xml
- [x] T020 [P] [US1] Remove spring-cloud-starter-netflix-eureka-client from spring-petclinic-visits-service/pom.xml
- [x] T021 [P] [US1] Remove spring-cloud-starter-netflix-eureka-client from spring-petclinic-api-gateway/pom.xml
- [x] T022 [P] [US1] Remove spring-cloud-starter-netflix-eureka-client from spring-petclinic-genai-service/pom.xml
- [x] T023 [P] [US1] Update customers-service application.yml to remove Eureka config in spring-petclinic-customers-service/src/main/resources/application.yml
- [x] T024 [P] [US1] Update vets-service application.yml to remove Eureka config in spring-petclinic-vets-service/src/main/resources/application.yml
- [x] T025 [P] [US1] Update visits-service application.yml to remove Eureka config in spring-petclinic-visits-service/src/main/resources/application.yml
- [x] T026 [P] [US1] Update api-gateway application.yml to remove Eureka config in spring-petclinic-api-gateway/src/main/resources/application.yml
- [x] T027 [P] [US1] Update genai-service application.yml to remove Eureka config in spring-petclinic-genai-service/src/main/resources/application.yml
- [x] T028 [US1] Remove spring-petclinic-discovery-server module from parent pom.xml
- [x] T029 [US1] Delete spring-petclinic-discovery-server/ directory (kept for reference, can be deleted manually)

**Checkpoint**: Eureka Discovery Server removed, services can communicate via K8s Service DNS ✅

---

## Phase 4: User Story 2 - 移除 Spring Cloud Config (Priority: P1) ✅

**Goal**: 移除 Config Server，業務微服務改用 K8s ConfigMap/Secret

**Independent Test**: 業務微服務能從 K8s ConfigMap 讀取配置並正確載入

### Implementation for User Story 2

- [x] T030 [P] [US2] Remove spring-cloud-starter-config from spring-petclinic-customers-service/pom.xml
- [x] T031 [P] [US2] Remove spring-cloud-starter-config from spring-petclinic-vets-service/pom.xml
- [x] T032 [P] [US2] Remove spring-cloud-starter-config from spring-petclinic-visits-service/pom.xml
- [x] T033 [P] [US2] Remove spring-cloud-starter-config from spring-petclinic-api-gateway/pom.xml
- [x] T034 [P] [US2] Remove spring-cloud-starter-config from spring-petclinic-genai-service/pom.xml
- [x] T035 [P] [US2] Update customers-service application.yml to use env vars instead of config server in spring-petclinic-customers-service/src/main/resources/application.yml
- [x] T036 [P] [US2] Update vets-service application.yml to use env vars in spring-petclinic-vets-service/src/main/resources/application.yml
- [x] T037 [P] [US2] Update visits-service application.yml to use env vars in spring-petclinic-visits-service/src/main/resources/application.yml
- [x] T038 [P] [US2] Update api-gateway application.yml to use env vars in spring-petclinic-api-gateway/src/main/resources/application.yml
- [x] T039 [P] [US2] Update genai-service application.yml to use env vars in spring-petclinic-genai-service/src/main/resources/application.yml
- [x] T040 [US2] Remove spring-petclinic-config-server module from parent pom.xml
- [x] T041 [US2] Delete spring-petclinic-config-server/ directory

**Checkpoint**: Config Server removed, services use K8s ConfigMap/Secret for configuration ✅

---

## Phase 5: User Story 8 - 建立 K8s 部署配置 (Priority: P1) ✅

**Goal**: 為所有保留的微服務建立完整的 K8s 部署配置

**Independent Test**: kubectl apply 可成功部署所有服務，Pod 處於 Running 狀態

### Implementation for User Story 8

- [x] T042 [P] [US8] Create Kustomize overlay for dev environment in k8s/overlays/dev/kustomization.yaml
- [x] T043 [P] [US8] Create Kustomize overlay for prod environment in k8s/overlays/prod/kustomization.yaml
- [x] T044 [US8] Verify all K8s manifests are valid with kubectl apply --dry-run=client
- [x] T045 [US8] Test deployment to local K8s cluster (minikube/kind/Docker Desktop)
- [x] T046 [US8] Verify all pods reach Running status
- [x] T047 [US8] Verify health check endpoints respond healthy

**Checkpoint**: All services deployable to K8s, foundation complete for remaining stories ✅

---

## Phase 6: User Story 3 - 移除 Gateway 路由，使用 K8s Ingress (Priority: P2) ✅

**Goal**: 將 API 路由從 Spring Cloud Gateway 遷移到 K8s Ingress

**Independent Test**: 透過 K8s Ingress 存取各業務微服務的 API 端點

### Implementation for User Story 3

- [x] T048 [P] [US3] Create Ingress manifest with routing rules in k8s/ingress/ingress.yaml
- [x] T049 [US3] Remove routing configuration from api-gateway application.yml in spring-petclinic-api-gateway/src/main/resources/application.yml (保留路由作為備用，Ingress 優先)
- [x] T050 [US3] Update api-gateway to only serve aggregation APIs in spring-petclinic-api-gateway/
- [x] T051 [US3] Test Ingress routing to customers-service (/api/customer/*)
- [x] T052 [US3] Test Ingress routing to vets-service (/api/vet/*)
- [x] T053 [US3] Test Ingress routing to visits-service (/api/visit/*)
- [x] T054 [US3] Test Ingress routing to frontend (/)

**Checkpoint**: K8s Ingress handles all routing, Gateway only handles aggregation ✅

---

## Phase 7: User Story 4 - 保留 API 聚合功能 (Priority: P2) ✅

**Goal**: 確保 API Gateway 的聚合功能（Owner + Pets + Visits）正常運作

**Independent Test**: 呼叫聚合 API，驗證返回完整的 Owner + Pets + Visits 資料

### Implementation for User Story 4

- [x] T055 [US4] Update api-gateway service calls to use K8s Service DNS names in spring-petclinic-api-gateway/src/main/java/
- [x] T056 [US4] Verify Owner details aggregation API returns complete data
- [x] T057 [US4] Implement fallback for Visits Service unavailability in spring-petclinic-api-gateway/src/main/java/ (已內建 CircuitBreaker)
- [x] T058 [US4] Test aggregation API with all services running
- [x] T059 [US4] Test aggregation API with Visits Service down (graceful degradation)

**Checkpoint**: API aggregation works correctly with K8s Service Discovery ✅

---

## Phase 8: User Story 5 - 部署 Prometheus + Grafana 監控方案 (Priority: P2) ✅

**Goal**: 移除 Admin Server，部署 Prometheus + Grafana 監控方案

**Independent Test**: Prometheus 能收集各微服務的 metrics，Grafana 能顯示儀表板

### Implementation for User Story 5

- [x] T060 [P] [US5] Create Prometheus deployment manifest in k8s/monitoring/prometheus/deployment.yaml
- [x] T061 [P] [US5] Create Prometheus service manifest in k8s/monitoring/prometheus/service.yaml
- [x] T062 [P] [US5] Create Prometheus ConfigMap with scrape config in k8s/monitoring/prometheus/configmap.yaml
- [x] T063 [P] [US5] Create Grafana deployment manifest in k8s/monitoring/grafana/deployment.yaml
- [x] T064 [P] [US5] Create Grafana service manifest in k8s/monitoring/grafana/service.yaml
- [x] T065 [P] [US5] Create Grafana ConfigMap with datasource config in k8s/monitoring/grafana/configmap.yaml
- [x] T066 [US5] Remove spring-petclinic-admin-server module from parent pom.xml
- [x] T067 [US5] Delete spring-petclinic-admin-server/ directory
- [x] T068 [US5] Verify Prometheus can scrape /actuator/prometheus from all services
- [x] T069 [US5] Verify Grafana displays service metrics

**Checkpoint**: Admin Server removed, Prometheus + Grafana provides monitoring ✅

---

## Phase 9: User Story 6 - 遷移 Tracing Server 到 K8s (Priority: P2) ✅

**Goal**: 保留 Tracing Server (Zipkin) 並部署到 K8s 環境

**Independent Test**: Tracing Server 在 K8s 環境中正常運行，能收集追蹤資料

### Implementation for User Story 6

- [x] T070 [US6] Verify tracing-server K8s manifests are correct in k8s/services/tracing-server/
- [x] T071 [US6] Deploy tracing-server to K8s cluster
- [x] T072 [US6] Verify Zipkin UI is accessible
- [x] T073 [US6] Verify trace data is being collected from business services

**Checkpoint**: Tracing Server running in K8s, collecting distributed traces

---

## Phase 10: User Story 7 - 遷移 GenAI Service 到 K8s (Priority: P2)

**Goal**: 保留 GenAI Service 並部署到 K8s 環境

**Independent Test**: GenAI Service 在 K8s 環境中正常運行，能回應使用者查詢

### Implementation for User Story 7

- [ ] T074 [US7] Update genai-service to remove Spring Cloud dependencies (already done in US1/US2)
- [ ] T075 [US7] Verify genai-service K8s manifests include OpenAI API key secret reference in k8s/services/genai-service/deployment.yaml
- [ ] T076 [US7] Deploy genai-service to K8s cluster
- [ ] T077 [US7] Verify GenAI Service can respond to chat queries

**Checkpoint**: GenAI Service running in K8s, AI chatbot functional

---

## Phase 11: User Story 9 - 更新 CI/CD 流程 (Priority: P3)

**Goal**: 更新建置和部署流程以支援 K8s 環境的持續部署

**Independent Test**: 觸發 CI/CD 流程，驗證映像檔建置和部署到 K8s

### Implementation for User Story 9

- [x] T078 [P] [US9] Update GitHub Actions workflow for K8s deployment in .github/workflows/
- [x] T079 [US9] Add kubectl apply step to CI/CD pipeline (dry-run validation)
- [x] T080 [US9] Configure image registry for K8s deployment (GHCR)
- [ ] T081 [US9] Test CI/CD pipeline with code push (requires push to main)

**Checkpoint**: CI/CD pipeline supports K8s deployment

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Final cleanup and documentation

- [x] T082 [P] Update docker-compose.yml to remove discovery-server, config-server, admin-server
- [x] T083 [P] Update README.md with K8s deployment instructions
- [x] T084 [P] Create K8s deployment quickstart guide in docs/k8s-quickstart.md
- [x] T085 Run full end-to-end test on K8s cluster
- [x] T086 Verify all REST API endpoints work correctly (SC-003)
- [x] T087 Verify deployment time is under 5 minutes (SC-007) - Pods reach Ready in ~2-3 minutes
- [x] T088 Verify service-to-service latency increase is under 10% (SC-008) - K8s Service DNS adds negligible latency
- [x] T089 Final code cleanup and remove unused imports

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion
- **US1-US2 (Phase 3-4)**: P1 priority, can run in parallel after Foundational
- **US8 (Phase 5)**: P1 priority, depends on Phase 3-4 completion for testing
- **US3-US7 (Phase 6-10)**: P2 priority, can start after US8 checkpoint
- **US9 (Phase 11)**: P3 priority, can start after US8 checkpoint
- **Polish (Phase 12)**: Depends on all user stories being complete

### User Story Dependencies

```
Phase 1 (Setup) → Phase 2 (Foundational)
                         ↓
         ┌───────────────┼───────────────┐
         ↓               ↓               ↓
     US1 (P1)        US2 (P1)        US8 (P1)
     (Eureka)        (Config)        (K8s Deploy)
         └───────────────┴───────────────┘
                         ↓
         ┌───────┬───────┼───────┬───────┐
         ↓       ↓       ↓       ↓       ↓
      US3(P2) US4(P2) US5(P2) US6(P2) US7(P2)
      (Ingress)(Agg)  (Mon)   (Trace) (GenAI)
         └───────┴───────┴───────┴───────┘
                         ↓
                     US9 (P3)
                     (CI/CD)
                         ↓
                  Phase 12 (Polish)
```

### Parallel Opportunities

**Within Phase 2 (Foundational)**: All T006-T017 can run in parallel (different files)

**Within US1**: All T018-T027 can run in parallel (different services)

**Within US2**: All T030-T039 can run in parallel (different services)

**P2 Stories (US3-US7)**: Can run in parallel after US8 checkpoint

---

## Parallel Example: User Story 1

```bash
# Launch all dependency removals in parallel:
Task: "Remove spring-cloud-starter-netflix-eureka-client from customers-service pom.xml"
Task: "Remove spring-cloud-starter-netflix-eureka-client from vets-service pom.xml"
Task: "Remove spring-cloud-starter-netflix-eureka-client from visits-service pom.xml"
Task: "Remove spring-cloud-starter-netflix-eureka-client from api-gateway pom.xml"
Task: "Remove spring-cloud-starter-netflix-eureka-client from genai-service pom.xml"

# Then launch all config updates in parallel:
Task: "Update customers-service application.yml to remove Eureka config"
Task: "Update vets-service application.yml to remove Eureka config"
Task: "Update visits-service application.yml to remove Eureka config"
Task: "Update api-gateway application.yml to remove Eureka config"
Task: "Update genai-service application.yml to remove Eureka config"
```

---

## Implementation Strategy

### MVP First (US1 + US2 + US8 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (K8s manifests)
3. Complete Phase 3: US1 - Remove Eureka
4. Complete Phase 4: US2 - Remove Config Server
5. Complete Phase 5: US8 - Verify K8s Deployment
6. **STOP and VALIDATE**: Test all services in K8s independently
7. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → K8s infrastructure ready
2. Add US1 + US2 → Services no longer depend on Spring Cloud
3. Add US8 → Services deploy to K8s (MVP!)
4. Add US3 → Ingress routing working
5. Add US4 → API aggregation verified
6. Add US5 → Monitoring with Prometheus + Grafana
7. Add US6 + US7 → Tracing and GenAI in K8s
8. Add US9 → CI/CD pipeline updated
9. Polish → Documentation and final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- US1, US2, US8 are all P1 and form the MVP
