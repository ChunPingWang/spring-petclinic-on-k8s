# Feature Specification: K8s 遷移 (Kubernetes Migration)

**Feature Branch**: `002-k8s-migration`
**Created**: 2025-12-10
**Status**: Draft
**Input**: User description: "使用 K8s 功能，取代 Spring Cloud 功能。保留 Tracing Server、GenAI Service。Admin Server 改用 Prometheus + Grafana 取代"

## Clarifications

### Session 2025-12-10

- Q: Admin Server 在移除 Eureka 後如何發現微服務？ → A: 移除 Admin Server，改用 Prometheus + Grafana 作為監控方案

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 移除 Spring Cloud 服務發現，使用 K8s Service Discovery (Priority: P1)

作為維運團隊，我需要將服務發現機制從 Eureka 遷移到 Kubernetes 原生的 Service Discovery，以減少維護複雜度並充分利用 K8s 平台能力。

**Why this priority**: Eureka Discovery Server 是 Spring Cloud 架構的核心元件，移除它是遷移到 K8s 的首要步驟，其他元件依賴服務發現機制。

**Independent Test**: 可以在 K8s 環境中部署業務微服務，驗證服務間可透過 K8s Service DNS 名稱互相呼叫。

**Acceptance Scenarios**:

1. **Given** 業務微服務已部署到 K8s, **When** 一個服務呼叫另一個服務, **Then** 透過 K8s Service DNS 名稱成功連線
2. **Given** K8s 環境運行中, **When** 移除 Eureka Discovery Server, **Then** 業務微服務仍能正常運作
3. **Given** 多個服務實例運行中, **When** 透過 K8s Service 呼叫, **Then** 請求被負載均衡到各實例

---

### User Story 2 - 移除 Spring Cloud Config，使用 K8s ConfigMap/Secret (Priority: P1)

作為維運團隊，我需要將集中式配置管理從 Spring Cloud Config Server 遷移到 Kubernetes ConfigMap 和 Secret，以統一配置管理方式。

**Why this priority**: Config Server 是另一個核心 Spring Cloud 元件，遷移配置管理是架構簡化的關鍵步驟。

**Independent Test**: 可以驗證業務微服務能夠從 K8s ConfigMap 讀取配置，並在配置變更時正確載入。

**Acceptance Scenarios**:

1. **Given** 配置已存放在 K8s ConfigMap, **When** 業務微服務啟動, **Then** 正確讀取並套用配置
2. **Given** 敏感資訊存放在 K8s Secret, **When** 微服務需要資料庫密碼, **Then** 正確讀取 Secret 值
3. **Given** K8s 環境運行中, **When** 移除 Config Server, **Then** 業務微服務仍能正常啟動和運作

---

### User Story 3 - 移除 Spring Cloud Gateway 路由，使用 K8s Ingress (Priority: P2)

作為維運團隊，我需要將 API 路由從 Spring Cloud Gateway 遷移到 Kubernetes Ingress，以統一流量入口管理。

**Why this priority**: API Gateway 目前承擔路由和前端靜態資源服務，遷移後可簡化架構，但需要保留 API 聚合功能。

**Independent Test**: 可以透過 K8s Ingress 存取各業務微服務的 API 端點，並驗證路由規則正確。

**Acceptance Scenarios**:

1. **Given** K8s Ingress 已配置路由規則, **When** 存取 /api/customer/* 路徑, **Then** 請求被路由到 Customers Service
2. **Given** K8s Ingress 已配置, **When** 存取 /api/vet/* 路徑, **Then** 請求被路由到 Vets Service
3. **Given** K8s Ingress 已配置, **When** 存取 /api/visit/* 路徑, **Then** 請求被路由到 Visits Service
4. **Given** K8s Ingress 已配置, **When** 存取根路徑, **Then** 返回前端應用程式

---

### User Story 4 - 保留 API 聚合功能並重構 API Gateway (Priority: P2)

作為使用者，我需要能夠透過單一 API 呼叫取得 Owner 的完整資訊（包含 Pets 和 Visits），因此 API 聚合功能必須保留。

**Why this priority**: API 聚合是核心業務功能，不能因架構遷移而丟失。需要評估是保留精簡版 API Gateway 還是整合到其中一個業務服務。

**Independent Test**: 可以呼叫聚合 API，驗證返回完整的 Owner + Pets + Visits 資料。

**Acceptance Scenarios**:

1. **Given** 聚合服務運行中, **When** 呼叫 Owner 詳情 API, **Then** 返回包含 Pets 和 Visits 的完整資料
2. **Given** Visits Service 暫時無法連線, **When** 呼叫 Owner 詳情 API, **Then** 返回 Owner 和 Pets 資料，Visits 使用降級回應

---

### User Story 5 - 移除 Admin Server，部署 Prometheus + Grafana 監控方案 (Priority: P2)

作為維運團隊，我需要移除 Spring Boot Admin Server，改用 Kubernetes 原生的 Prometheus + Grafana 監控方案，以獲得更好的 K8s 生態系統整合。

**Why this priority**: Admin Server 依賴 Eureka 進行服務發現，移除 Eureka 後需要替代監控方案。Prometheus + Grafana 是 K8s 生態系統標準監控方案。

**Independent Test**: 可以驗證 Prometheus 能收集各微服務的 Actuator metrics，Grafana 能顯示監控儀表板。

**Acceptance Scenarios**:

1. **Given** Prometheus 已部署到 K8s, **When** 業務微服務運行, **Then** Prometheus 能抓取 /actuator/prometheus 端點的指標
2. **Given** Grafana 已部署並連接 Prometheus, **When** 存取 Grafana UI, **Then** 能查看所有服務的健康狀態和效能指標
3. **Given** Admin Server 已移除, **When** 業務微服務運行, **Then** 所有業務功能正常

---

### User Story 6 - 保留並遷移 Tracing Server 到 K8s (Priority: P2)

作為維運團隊，我需要保留 Tracing Server (Zipkin) 並將其部署到 K8s 環境，以繼續使用分散式追蹤功能。

**Why this priority**: Tracing Server 提供請求追蹤和效能分析能力，對於微服務除錯和效能優化非常重要。

**Independent Test**: 可以驗證 Tracing Server 在 K8s 環境中正常運行，並能收集追蹤資料。

**Acceptance Scenarios**:

1. **Given** Tracing Server 已部署到 K8s, **When** 業務微服務發送請求, **Then** 追蹤資料被正確收集
2. **Given** Tracing Server 運行中, **When** 存取 Zipkin UI, **Then** 能查看請求追蹤和服務依賴圖

---

### User Story 7 - 保留並遷移 GenAI Service 到 K8s (Priority: P2)

作為使用者，我需要保留 GenAI Service 並將其部署到 K8s 環境，以繼續使用 AI 聊天機器人功能。

**Why this priority**: GenAI Service 提供 AI 輔助功能，是使用者體驗的一部分。

**Independent Test**: 可以驗證 GenAI Service 在 K8s 環境中正常運行，並能回應使用者查詢。

**Acceptance Scenarios**:

1. **Given** GenAI Service 已部署到 K8s, **When** 使用者透過聊天介面發送問題, **Then** 收到 AI 回應
2. **Given** GenAI Service 運行中, **When** 查看前端介面, **Then** 聊天機器人入口正常顯示並可使用

---

### User Story 8 - 建立 Kubernetes 部署配置 (Priority: P1)

作為維運團隊，我需要為所有保留的微服務建立 Kubernetes 部署配置（Deployment、Service、ConfigMap、Secret），以實現 K8s 原生部署。

**Why this priority**: 這是實現 K8s 遷移的基礎工作，所有服務都需要 K8s manifest 才能部署。

**Independent Test**: 可以使用 kubectl apply 部署所有服務，並驗證服務正常啟動。

**Acceptance Scenarios**:

1. **Given** K8s manifest 已建立, **When** 執行 kubectl apply, **Then** 所有業務微服務成功部署
2. **Given** 服務已部署, **When** 檢查 Pod 狀態, **Then** 所有 Pod 處於 Running 狀態
3. **Given** 服務已部署, **When** 執行健康檢查, **Then** 所有服務回應 healthy

---

### User Story 9 - 更新 CI/CD 流程以支援 K8s 部署 (Priority: P3)

作為維運團隊，我需要更新建置和部署流程，以支援 K8s 環境的持續部署。

**Why this priority**: 自動化部署是維運效率的關鍵，但可以在核心遷移完成後再優化。

**Independent Test**: 可以觸發 CI/CD 流程，驗證映像檔建置和部署到 K8s 的完整流程。

**Acceptance Scenarios**:

1. **Given** 程式碼推送到 main 分支, **When** CI/CD 流程執行, **Then** 映像檔成功建置並推送到容器倉庫
2. **Given** 新映像檔可用, **When** 部署到 K8s, **Then** 服務使用新版本且無停機時間

---

### Edge Cases

- 當 K8s 叢集資源不足時，系統應如何處理 Pod 調度失敗
- 當 ConfigMap 配置錯誤時，服務啟動應有明確的錯誤訊息
- 當 Ingress 配置的後端服務不存在時，應返回適當的錯誤頁面
- 當服務間網路暫時中斷時，應有重試機制或降級策略
- 當滾動更新過程中新版本有問題時，應能自動回滾

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系統 MUST 移除 Eureka Discovery Server，改用 K8s Service Discovery
- **FR-002**: 系統 MUST 移除 Spring Cloud Config Server，改用 K8s ConfigMap 和 Secret
- **FR-003**: 系統 MUST 移除 Spring Cloud Gateway 的路由功能，改用 K8s Ingress
- **FR-004**: 系統 MUST 保留 API 聚合功能（Owner + Pets + Visits 聚合查詢）
- **FR-005**: 系統 MUST 移除 Admin Server，改用 Prometheus + Grafana 監控方案
- **FR-006**: 系統 MUST 保留 Tracing Server (Zipkin) 並部署到 K8s 環境
- **FR-007**: 系統 MUST 保留 GenAI Service 並部署到 K8s 環境
- **FR-008**: 系統 MUST 保留 Customers Service、Vets Service、Visits Service 三個核心業務服務
- **FR-009**: 系統 MUST 保留前端 UI 並確保能透過 K8s Ingress 存取
- **FR-010**: 系統 MUST 提供 K8s Deployment manifest 給每個保留的服務
- **FR-011**: 系統 MUST 提供 K8s Service manifest 給每個保留的服務
- **FR-012**: 系統 MUST 提供 K8s ConfigMap 來管理應用程式配置
- **FR-013**: 系統 MUST 提供 K8s Secret 來管理敏感資訊（資料庫密碼等）
- **FR-014**: 系統 MUST 提供 K8s Ingress 配置來管理外部流量路由
- **FR-015**: 業務微服務 MUST 移除對 Eureka Client 的依賴
- **FR-016**: 業務微服務 MUST 移除對 Spring Cloud Config Client 的依賴
- **FR-017**: 原有的 REST API 端點和行為 MUST NOT 被此遷移所影響

### Key Entities

- **Owner**: 寵物主人，核心業務實體（保留）
- **Pet**: 寵物，核心業務實體（保留）
- **PetType**: 寵物類型（保留）
- **Vet**: 獸醫（保留）
- **Specialty**: 獸醫專長（保留）
- **Visit**: 就診紀錄（保留）

### Components to Remove

- **Discovery Server (Eureka)**: 服務發現 → 由 K8s Service 取代
- **Config Server**: 集中式配置 → 由 K8s ConfigMap/Secret 取代
- **Admin Server**: Spring Boot Admin → 由 Prometheus + Grafana 取代

### Components to Refactor

- **API Gateway**: 保留 API 聚合功能，移除路由功能（由 K8s Ingress 取代）

### Components to Keep (with modifications)

- **Customers Service**: 移除 Spring Cloud 依賴
- **Vets Service**: 移除 Spring Cloud 依賴
- **Visits Service**: 移除 Spring Cloud 依賴
- **Tracing Server**: 保留 Zipkin，部署到 K8s
- **GenAI Service**: 保留，移除 Spring Cloud 依賴，部署到 K8s
- **UI (Frontend)**: 保留，透過 K8s Ingress 提供服務

### Components to Add

- **Prometheus**: 指標收集，抓取各服務的 /actuator/prometheus 端點
- **Grafana**: 監控儀表板視覺化，連接 Prometheus 資料來源

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 成功移除 3 個 Spring Cloud 基礎設施服務（Discovery Server、Config Server、Admin Server）
- **SC-002**: 保留的 6 個服務（Customers、Vets、Visits、API Gateway/聚合服務、Tracing Server、GenAI Service）可在 K8s 環境中正常運行
- **SC-002a**: Prometheus + Grafana 監控方案成功部署並能顯示各服務指標
- **SC-003**: 所有原有的 REST API 端點（建立/查詢/更新 Owner、Pet、Visit、Vet）功能正常，測試通過率 100%
- **SC-004**: 應用程式可透過 K8s Ingress 從外部存取
- **SC-005**: 使用 K8s ConfigMap 管理至少 3 種配置（資料庫連線、服務端口、應用程式設定）
- **SC-006**: 使用 K8s Secret 管理敏感資訊（至少包含資料庫密碼）
- **SC-007**: 部署時間（從 kubectl apply 到所有服務 Ready）不超過 5 分鐘
- **SC-008**: 服務間通訊延遲與遷移前相比增加不超過 10%
- **SC-009**: 成功移除 Spring Cloud 服務發現和配置管理相關程式碼
- **SC-010**: 部署配置檔案（K8s manifest）完整且可重複部署

## Assumptions

- Kubernetes 叢集已就緒並可供使用（本地開發可使用 minikube、kind 或 Docker Desktop K8s）
- 團隊具備基本的 Kubernetes 操作知識
- 現有的資料庫配置（MySQL 或 HSQLDB）可直接沿用
- 前端 UI 不需要大幅修改，只需更新 API endpoint 配置
- 保留的服務使用的 Spring Boot 版本支援無 Spring Cloud 運行
- CI/CD 環境支援 Kubernetes 部署（或可以手動部署進行驗證）
- 監控和日誌收集將由 K8s 生態系統工具提供（如 Prometheus、Grafana、ELK 等），不在本次遷移範圍內配置
