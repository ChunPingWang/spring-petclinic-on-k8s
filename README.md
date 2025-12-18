# Spring PetClinic 微服務 - Kubernetes 版本

[![建置狀態](https://github.com/ChunPingWang/spring-petclinic-on-k8s/actions/workflows/maven-build.yml/badge.svg)](https://github.com/ChunPingWang/spring-petclinic-on-k8s/actions/workflows/maven-build.yml)
[![授權條款](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

這是 Spring PetClinic 微服務應用程式的 **Kubernetes 原生版本**。本專案展示如何在 Kubernetes 上部署具備生產環境等級的微服務架構，並採用現代雲原生技術。

> **與原版的主要差異**：Spring Cloud Config Server、Eureka Discovery Server 和 Admin Server 已被**移除**，改用 Kubernetes 原生解決方案（ConfigMap/Secret、K8s DNS 服務發現、Prometheus + Grafana）。

## 目錄

- [架構概覽](#架構概覽)
- [技術堆疊](#技術堆疊)
- [專案結構](#專案結構)
- [快速開始](#快速開始)
  - [環境需求](#環境需求)
  - [部署到 Kubernetes（建議方式）](#部署到-kubernetes建議方式)
  - [使用 Docker Compose 執行](#使用-docker-compose-執行)
  - [本機執行（不使用 Docker）](#本機執行不使用-docker)
- [Kubernetes 部署指南](#kubernetes-部署指南)
- [CI/CD 流水線](#cicd-流水線)
- [監控與可觀測性](#監控與可觀測性)
- [Design by Contract（契約式設計）](#design-by-contract契約式設計)
- [GenAI 聊天機器人整合](#genai-聊天機器人整合)
- [API 參考文件](#api-參考文件)
- [疑難排解](#疑難排解)

---

## 架構概覽

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
   │  :9411    │    追蹤           │   :3306   │                   │   API     │
   └───────────┘                   └───────────┘                   └───────────┘
         ▲
         │ 指標
   ┌─────┴─────┐         ┌───────────┐
   │Prometheus │────────►│  Grafana  │
   │   :9090   │         │   :3000   │
   └───────────┘         └───────────┘
```

### 服務通訊方式

| 通訊類型 | 技術 |
|---------|------|
| 服務發現 | Kubernetes DNS（例如：`http://customers-service:8081`）|
| 組態管理 | Kubernetes ConfigMap + Secret |
| 負載平衡 | Kubernetes Service (ClusterIP) |
| 外部存取 | NGINX Ingress Controller |
| 斷路器 | Resilience4j |
| 分散式追蹤 | Micrometer + Zipkin |

---

## 技術堆疊

| 類別 | 技術 |
|------|------|
| **程式語言** | Java 17 |
| **框架** | Spring Boot 3.4.1、Spring Cloud 2024.0.0 |
| **API 閘道** | Spring Cloud Gateway |
| **契約測試** | Spring Cloud Contract 4.2.x |
| **資料庫** | MySQL 8.4.5 / HSQLDB（記憶體內）|
| **容器執行環境** | Docker、containerd |
| **容器編排** | Kubernetes（Kind、Minikube、EKS、GKE、AKS）|
| **組態管理** | Kustomize |
| **Ingress** | NGINX Ingress Controller |
| **分散式追蹤** | Micrometer Tracing + Zipkin |
| **監控** | Prometheus + Grafana |
| **AI/LLM** | Spring AI + OpenAI / Azure OpenAI |
| **CI/CD** | GitHub Actions |

---

## 專案結構

```
spring-petclinic-on-k8s/
├── spring-petclinic-api-gateway/       # API 閘道（連接埠 8080）
├── spring-petclinic-customers-service/ # 客戶與寵物管理（連接埠 8081）
├── spring-petclinic-visits-service/    # 就診紀錄管理（連接埠 8082）
├── spring-petclinic-vets-service/      # 獸醫師管理（連接埠 8083）
├── spring-petclinic-genai-service/     # AI 聊天機器人服務（連接埠 8084）
├── k8s/                                # Kubernetes 資源清單
│   ├── base/                           # 基礎資源（namespace、configmap、secret）
│   ├── services/                       # 服務部署
│   │   ├── api-gateway/
│   │   ├── customers-service/
│   │   ├── vets-service/
│   │   ├── visits-service/
│   │   ├── genai-service/
│   │   ├── mysql/
│   │   └── tracing-server/
│   ├── monitoring/                     # 可觀測性堆疊
│   │   ├── prometheus/
│   │   └── grafana/
│   ├── ingress/                        # Ingress 設定
│   └── overlays/                       # Kustomize 覆蓋層
│       ├── dev/                        # 開發環境
│       ├── staging/                    # 預備環境
│       └── prod/                       # 正式環境
├── docker-compose.yml                  # Docker 本機開發
├── .github/workflows/                  # CI/CD 流水線
│   └── maven-build.yml
└── docs/                               # 文件
    └── k8s-quickstart.md
```

---

## 快速開始

### 環境需求

- **Java 17+** 與 Maven 3.8+
- **Docker**（用於建置映像檔）
- **kubectl** CLI 並已設定好
- **Kind** 或其他 Kubernetes 叢集
- （選用）**OpenAI API Key**（供 GenAI 服務使用）

### 部署到 Kubernetes（建議方式）

#### 1. 建立 Kind 叢集

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

#### 2. 安裝 NGINX Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

#### 3. 建置 Docker 映像檔

```bash
# 建置所有服務映像檔
./mvnw clean spring-boot:build-image -pl spring-petclinic-customers-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-vets-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-visits-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-api-gateway -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-genai-service -DskipTests
```

#### 4. 載入映像檔到 Kind

```bash
kind load docker-image petclinic/customers-service:latest --name petclinic
kind load docker-image petclinic/vets-service:latest --name petclinic
kind load docker-image petclinic/visits-service:latest --name petclinic
kind load docker-image petclinic/api-gateway:latest --name petclinic
kind load docker-image petclinic/genai-service:latest --name petclinic
```

#### 5. 部署到 Kubernetes

```bash
# 使用開發環境覆蓋層部署
kubectl apply -k k8s/overlays/dev

# 等待所有 Pod 就緒
kubectl wait --namespace petclinic \
  --for=condition=ready pod \
  --all \
  --timeout=300s
```

#### 6. 設定 /etc/hosts

```bash
echo "127.0.0.1 petclinic.local" | sudo tee -a /etc/hosts
```

#### 7. 存取應用程式

| 服務 | 網址 |
|------|------|
| 前端 UI | http://petclinic.local/ |
| 客戶 API | http://petclinic.local/api/customer/owners |
| 獸醫 API | http://petclinic.local/api/vet/vets |
| 就診 API | http://petclinic.local/api/visit/pets/visits?petId=1 |
| Zipkin 追蹤 | http://petclinic.local/zipkin |
| Grafana | http://petclinic.local/grafana（admin/admin）|

如需存取 Prometheus，請使用 port-forward：
```bash
kubectl port-forward -n petclinic svc/prometheus 9090:9090
# 存取 http://localhost:9090
```

### 使用 Docker Compose 執行

```bash
# 建置映像檔
./mvnw clean spring-boot:build-image -DskipTests

# 啟動所有服務
docker compose up -d

# 檢查狀態
docker compose ps
```

存取網址：http://localhost:8080

### 本機執行（不使用 Docker）

每個服務都可以獨立執行：

```bash
# 在不同的終端機視窗分別啟動各服務
cd spring-petclinic-customers-service && ../mvnw spring-boot:run
cd spring-petclinic-vets-service && ../mvnw spring-boot:run
cd spring-petclinic-visits-service && ../mvnw spring-boot:run
cd spring-petclinic-api-gateway && ../mvnw spring-boot:run
```

| 服務 | 網址 |
|------|------|
| API 閘道（前端）| http://localhost:8080 |
| 客戶服務 | http://localhost:8081 |
| 就診服務 | http://localhost:8082 |
| 獸醫服務 | http://localhost:8083 |
| GenAI 服務 | http://localhost:8084 |

---

## Kubernetes 部署指南

### Kustomize 覆蓋層

本專案使用 **Kustomize** 進行環境特定的組態設定：

| 覆蓋層 | 說明 | 指令 |
|--------|------|------|
| `dev` | 開發環境（1 個副本，較低資源）| `kubectl apply -k k8s/overlays/dev` |
| `staging` | 預備環境（2 個副本）| `kubectl apply -k k8s/overlays/staging` |
| `prod` | 正式環境（3 個副本，較高資源）| `kubectl apply -k k8s/overlays/prod` |

### 快速啟動/停止腳本

本專案提供便利的腳本來快速啟動或停止所有服務：

```bash
# 停止所有服務（將副本數縮放至 0）
./scripts/k8s-stop.sh

# 啟動所有服務（將副本數恢復至 1）
./scripts/k8s-start.sh

# 指定副本數啟動
REPLICAS=2 ./scripts/k8s-start.sh

# 指定不同的 namespace
NAMESPACE=petclinic-staging ./scripts/k8s-start.sh
```

| 腳本 | 說明 |
|------|------|
| `scripts/k8s-start.sh` | 啟動所有服務，等待 Pod 就緒 |
| `scripts/k8s-stop.sh` | 停止所有服務（保留部署配置）|

### 常用 kubectl 指令

```bash
# 檢查 Pod 狀態
kubectl get pods -n petclinic

# 檢視日誌
kubectl logs -n petclinic deployment/customers-service

# 重新啟動服務
kubectl rollout restart deployment/customers-service -n petclinic

# 擴展服務
kubectl scale deployment/customers-service --replicas=3 -n petclinic

# 刪除所有資源
kubectl delete -k k8s/overlays/dev
```

### 健康檢查

所有服務都提供健康檢查端點：

```bash
# 存活探測
curl http://localhost:8081/actuator/health/liveness

# 就緒探測
curl http://localhost:8081/actuator/health/readiness
```

---

## CI/CD 流水線

本專案使用 **GitHub Actions** 進行 CI/CD：

### 工作流程：`.github/workflows/maven-build.yml`

| 觸發條件 | 執行動作 |
|----------|----------|
| 推送到 `main` | 建置、測試、驗證 K8s 資源清單、建置並推送 Docker 映像檔 |
| 對 `main` 發起 Pull Request | 建置、測試、驗證 K8s 資源清單 |

### 流水線階段

1. **建置與測試**：`mvn -B package`
2. **驗證 K8s 資源清單**：
   ```bash
   kubectl kustomize k8s/overlays/dev > /tmp/k8s-dev.yaml
   kubectl apply --dry-run=client -f /tmp/k8s-dev.yaml
   ```
3. **建置 Docker 映像檔**：使用 Spring Boot 的 `build-image` 外掛
4. **推送到 GHCR**：將映像檔推送到 GitHub Container Registry（僅限 main 分支）

### 容器映像檔倉庫

映像檔發布至：`ghcr.io/<owner>/petclinic/<service-name>:latest`

---

## 監控與可觀測性

### 分散式追蹤（Zipkin）

所有服務都已整合 Micrometer Tracing：

- 存取 Zipkin UI：http://petclinic.local/zipkin
- 跨微服務的追蹤關聯
- 請求延遲分析

### 指標監控（Prometheus + Grafana）

**Prometheus** 收集所有服務的指標：
- JVM 指標（記憶體、GC、執行緒）
- HTTP 請求指標
- 自訂業務指標

**Grafana** 儀表板位於 http://petclinic.local/grafana：
- 預設帳號密碼：admin/admin
- 已預先設定 Prometheus 資料來源

### 自訂指標

| 服務 | 指標 |
|------|------|
| customers-service | `petclinic.owner`、`petclinic.pet` |
| visits-service | `petclinic.visit` |

---

## Design by Contract（契約式設計）

本專案採用 **Spring Cloud Contract** 實現 Consumer-Driven Contract (CDC) 測試模式，確保微服務間 API 契約的一致性，防止因 API 變更導致的整合問題。

### 什麼是 Design by Contract？

Design by Contract 是一種軟體設計方法，透過明確定義組件之間的「契約」（Contract），確保：
- **Producer**（API 提供者）承諾提供符合契約的回應
- **Consumer**（API 消費者）期望收到符合契約的回應
- 任何破壞契約的變更都會在 CI/CD 階段被自動偵測

### 服務角色

| 服務 | 角色 | 說明 |
|------|------|------|
| customers-service | Producer | 提供 Owner、Pet、PetType API |
| vets-service | Producer | 提供 Vet API |
| visits-service | Producer | 提供 Visit API |
| api-gateway | Consumer | 呼叫其他服務進行資料聚合 |

### 契約涵蓋範圍

本專案定義了以下 API 契約：

**Customers Service（12 個契約）**
- `GET /owners/{id}` - 取得單一飼主
- `GET /owners` - 取得所有飼主
- `POST /owners` - 新增飼主
- `PUT /owners/{id}` - 更新飼主
- `GET /owners/{id}/pets/{petId}` - 取得寵物詳情
- `POST /owners/{id}/pets` - 新增寵物
- `GET /petTypes` - 取得所有寵物類型

**Vets Service（1 個契約）**
- `GET /vets` - 取得所有獸醫師

**Visits Service（3 個契約）**
- `POST /owners/*/pets/{petId}/visits` - 新增就診紀錄
- `GET /owners/*/pets/{petId}/visits` - 取得寵物就診紀錄
- `GET /pets/visits?petId=...` - 批次取得多隻寵物的就診紀錄

### 契約測試工作流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Consumer-Driven Contract Testing                      │
└─────────────────────────────────────────────────────────────────────────┘

1. 定義契約（Contract Definition）
   └─> src/test/resources/contracts/*.groovy

2. Producer 驗證（Producer Verification）
   └─> 自動產生測試，驗證 API 符合契約

3. 產生 Stubs（Stub Generation）
   └─> 產生 WireMock stubs 供 Consumer 使用

4. Consumer 測試（Consumer Testing）
   └─> 使用 stubs 進行整合測試
```

### 快速開始

#### 1. 執行 Producer 契約測試

```bash
# 執行 customers-service 契約測試
./mvnw clean test -pl spring-petclinic-customers-service

# 執行 vets-service 契約測試
./mvnw clean test -pl spring-petclinic-vets-service

# 執行 visits-service 契約測試
./mvnw clean test -pl spring-petclinic-visits-service
```

#### 2. 產生並安裝 Stubs

```bash
# 產生 stubs 並安裝到本地 Maven 倉庫
./mvnw clean install -pl spring-petclinic-customers-service,spring-petclinic-vets-service,spring-petclinic-visits-service
```

#### 3. 執行 Consumer 契約測試

```bash
# 執行 api-gateway 契約測試（使用 stubs）
./mvnw clean test -pl spring-petclinic-api-gateway
```

### 契約 DSL 範例

以下是 `GET /owners/{id}` 的契約定義範例（Groovy DSL）：

```groovy
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
            address: $(consumer('110 W. Liberty St.'), producer(regex('.+'))),
            city: $(consumer('Madison'), producer(regex('[a-zA-Z ]+'))),
            telephone: $(consumer('6085551023'), producer(regex('[0-9]{10}'))),
            pets: []
        ])
    }
}
```

### 專案結構

```
spring-petclinic-customers-service/
├── src/test/resources/contracts/
│   ├── owners/                          # Owner API 契約
│   │   ├── shouldReturnOwnerById.groovy
│   │   ├── shouldCreateOwner.groovy
│   │   └── shouldReturn404WhenNotFound.groovy
│   └── pets/                            # Pet API 契約
│       └── shouldReturnPetTypes.groovy
└── src/test/java/.../contracts/
    ├── OwnersBase.java                  # Owner 契約測試基礎類別
    └── PetsBase.java                    # Pet 契約測試基礎類別
```

### CI/CD 整合

契約測試已整合到 CI/CD 流水線中：

1. **Pull Request 階段**：執行所有 Producer 契約測試
2. **合併到 main 階段**：產生並發布 stubs
3. **部署前驗證**：Consumer 使用最新 stubs 執行測試

### 進一步閱讀

- [Spring Cloud Contract 官方文件](https://docs.spring.io/spring-cloud-contract/reference/)
- [Consumer-Driven Contracts 介紹](https://martinfowler.com/articles/consumerDrivenContracts.html)
- [本專案契約定義](specs/002-k8s-migration/contracts/spring-cloud-contract/)

---

## GenAI 聊天機器人整合

GenAI 服務提供自然語言介面：

### 查詢範例

- 「列出所有飼主」
- 「有沒有專長外科的獸醫？」
- 「幫飼主 Betty 新增一隻名叫 Max 的狗」
- 「哪些飼主有養貓？」

### 設定

**離線模式**（Kubernetes 部署的預設值）：
```yaml
genai:
  offline-mode: true  # 所有請求都回傳「服務暫時離線」
```

**線上模式**（需要 OpenAI API Key）：
```bash
export OPENAI_API_KEY="your_api_key_here"
# 或使用 Azure OpenAI：
export AZURE_OPENAI_ENDPOINT="https://your_resource.openai.azure.com"
export AZURE_OPENAI_KEY="your_api_key_here"
```

---

## API 參考文件

### 客戶服務（連接埠 8081）

| 方法 | 端點 | 說明 |
|------|------|------|
| GET | `/owners` | 列出所有飼主 |
| GET | `/owners/{id}` | 依 ID 取得飼主 |
| POST | `/owners` | 新增飼主 |
| PUT | `/owners/{id}` | 更新飼主 |
| GET | `/owners/{id}/pets` | 取得飼主的寵物 |
| POST | `/owners/{id}/pets` | 為飼主新增寵物 |

### 獸醫服務（連接埠 8083）

| 方法 | 端點 | 說明 |
|------|------|------|
| GET | `/vets` | 列出所有獸醫師 |

### 就診服務（連接埠 8082）

| 方法 | 端點 | 說明 |
|------|------|------|
| GET | `/pets/visits?petId={id}` | 取得寵物的就診紀錄 |
| POST | `/owners/{ownerId}/pets/{petId}/visits` | 新增就診紀錄 |

### GenAI 服務（連接埠 8084）

| 方法 | 端點 | 說明 |
|------|------|------|
| POST | `/chatclient` | 傳送聊天訊息 |

---

## 疑難排解

### Pod 停留在 Pending 狀態

```bash
# 檢查映像檔是否已載入
docker images | grep petclinic
kind load docker-image <image-name> --name petclinic
```

### Pod 處於 CrashLoopBackOff 狀態

```bash
kubectl logs -n petclinic <pod-name>
kubectl describe pod -n petclinic <pod-name>
```

### Ingress 無法運作

```bash
# 確認 Ingress Controller 正在執行
kubectl get pods -n ingress-nginx

# 檢查 Ingress 資源
kubectl describe ingress -n petclinic

# 確認 /etc/hosts 設定
cat /etc/hosts | grep petclinic
```

### 資料庫連線問題

```bash
# 檢查 MySQL 是否正在執行
kubectl get pods -n petclinic | grep mysql
kubectl logs -n petclinic deployment/mysql
```

---

## 貢獻指南

1. Fork 本專案
2. 建立功能分支：`git checkout -b feature/my-feature`
3. 提交變更：`git commit -m '新增我的功能'`
4. 推送到分支：`git push origin feature/my-feature`
5. 發起 Pull Request

---

## 授權條款

本專案採用 Apache License 2.0 授權 - 詳見 [LICENSE](LICENSE) 檔案。

---

## 致謝

- 原版 [Spring PetClinic Microservices](https://github.com/spring-petclinic/spring-petclinic-microservices)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Kubernetes](https://kubernetes.io/)
