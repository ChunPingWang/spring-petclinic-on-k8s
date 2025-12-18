# Quickstart: K8s 部署指南

**Branch**: `002-k8s-migration` | **Date**: 2025-12-10

## Prerequisites

- Kubernetes cluster (minikube, kind, Docker Desktop K8s, or cloud provider)
- kubectl CLI installed
- NGINX Ingress Controller installed

### Install NGINX Ingress Controller

```bash
# For minikube
minikube addons enable ingress

# For kind
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# For other clusters
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.4/deploy/static/provider/cloud/deploy.yaml
```

---

## Quick Deploy

### 1. Build Docker Images (if needed)

```bash
# Build all images
./mvnw clean package -DskipTests -PbuildDocker
```

### 2. Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace petclinic

# Deploy all services (dev environment)
kubectl apply -k k8s/overlays/dev

# Or deploy to production
kubectl apply -k k8s/overlays/prod
```

### 3. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n petclinic

# Expected output:
# NAME                                 READY   STATUS    RESTARTS   AGE
# customers-service-xxx                1/1     Running   0          2m
# vets-service-xxx                     1/1     Running   0          2m
# visits-service-xxx                   1/1     Running   0          2m
# api-gateway-xxx                      1/1     Running   0          2m
# genai-service-xxx                    1/1     Running   0          2m
# tracing-server-xxx                   1/1     Running   0          2m
# prometheus-xxx                       1/1     Running   0          2m
# grafana-xxx                          1/1     Running   0          2m

# Check services
kubectl get svc -n petclinic

# Check ingress
kubectl get ingress -n petclinic
```

### 4. Configure Local Access

```bash
# Add to /etc/hosts (Linux/Mac) or C:\Windows\System32\drivers\etc\hosts (Windows)
echo "127.0.0.1 petclinic.local" | sudo tee -a /etc/hosts

# For minikube, get the IP
minikube ip
# Then add: <minikube-ip> petclinic.local
```

### 5. Access the Application

| Service | URL |
|---------|-----|
| Frontend UI | http://petclinic.local |
| Zipkin (Tracing) | http://petclinic.local/zipkin |
| Grafana (Monitoring) | http://petclinic.local/grafana |
| Prometheus | kubectl port-forward svc/prometheus 9090:9090 -n petclinic |

---

## Development Workflow

### Port Forwarding (for debugging)

```bash
# Access individual services directly
kubectl port-forward svc/customers-service 8081:8081 -n petclinic
kubectl port-forward svc/vets-service 8083:8083 -n petclinic
kubectl port-forward svc/visits-service 8082:8082 -n petclinic
kubectl port-forward svc/api-gateway 8080:8080 -n petclinic
```

### View Logs

```bash
# View logs for a specific service
kubectl logs -f deployment/customers-service -n petclinic

# View logs for all services
kubectl logs -f -l app=customers-service -n petclinic
```

### Update Configuration

```bash
# Edit ConfigMap
kubectl edit configmap petclinic-config -n petclinic

# Restart deployments to pick up changes
kubectl rollout restart deployment -n petclinic
```

### Scale Services

```bash
# Scale a specific service
kubectl scale deployment customers-service --replicas=3 -n petclinic

# Check horizontal pod autoscaler (if configured)
kubectl get hpa -n petclinic
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod events
kubectl describe pod <pod-name> -n petclinic

# Check logs
kubectl logs <pod-name> -n petclinic --previous
```

### Service Not Accessible

```bash
# Check service endpoints
kubectl get endpoints -n petclinic

# Test service connectivity from another pod
kubectl run curl --image=curlimages/curl -it --rm -- curl http://customers-service:8081/actuator/health
```

### Ingress Not Working

```bash
# Check ingress status
kubectl describe ingress petclinic-ingress -n petclinic

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

---

## Clean Up

```bash
# Delete all resources
kubectl delete -k k8s/overlays/dev

# Or delete namespace (removes everything)
kubectl delete namespace petclinic
```

---

## Environment-Specific Deployment

| Environment | Command | Replicas | Memory |
|-------------|---------|----------|--------|
| Development | `kubectl apply -k k8s/overlays/dev` | 1 | 256Mi |
| Staging | `kubectl apply -k k8s/overlays/staging` | 2 | 512Mi |
| Production | `kubectl apply -k k8s/overlays/prod` | 3 | 1Gi |

---

## Contract Testing Quickstart

### Overview

Spring Cloud Contract 實現 Design by Contract 模式，確保微服務間 API 契約一致性。

### Prerequisites

- Maven 3.8+
- Java 17
- 專案已包含 Spring Cloud 2024.0.0

---

### 1. Producer Service Setup (customers, vets, visits)

#### 1.1 Add Dependencies

在 Producer 服務的 `pom.xml` 加入：

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
                    org.springframework.samples.petclinic.customers.contracts
                </packageWithBaseClasses>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### 1.2 Create Contract Files

將 contract 檔案放置於 `src/test/resources/contracts/` 目錄：

```
src/test/resources/contracts/
├── owners/
│   ├── shouldReturnOwnerById.groovy
│   └── shouldCreateOwner.groovy
└── pets/
    └── shouldReturnPetTypes.groovy
```

範例 contract 檔案可從 `specs/002-k8s-migration/contracts/spring-cloud-contract/` 複製。

#### 1.3 Create Base Test Class

建立對應的 Base Test Class：

```java
package org.springframework.samples.petclinic.customers.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import java.util.Optional;

@SpringBootTest
public class OwnersBase {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private OwnerRepository ownerRepository;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);

        // Setup mock data
        Owner owner = new Owner();
        owner.setId(1);
        owner.setFirstName("George");
        owner.setLastName("Franklin");
        owner.setAddress("110 W. Liberty St.");
        owner.setCity("Madison");
        owner.setTelephone("6085551023");

        when(ownerRepository.findById(1)).thenReturn(Optional.of(owner));
        when(ownerRepository.findById(99999)).thenReturn(Optional.empty());
    }
}
```

#### 1.4 Run Contract Tests

```bash
# 產生測試並執行
./mvnw clean test -pl spring-petclinic-customers-service

# 產生 stubs jar
./mvnw clean install -pl spring-petclinic-customers-service

# 檢視產生的 stubs
ls target/stubs/
```

---

### 2. Consumer Service Setup (api-gateway)

#### 2.1 Add Dependencies

在 `spring-petclinic-api-gateway/pom.xml` 加入：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### 2.2 Create Consumer Test

```java
package org.springframework.samples.petclinic.api.boundary.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureStubRunner(
    ids = {
        "org.springframework.samples.petclinic.client:spring-petclinic-customers-service:+:stubs:8081",
        "org.springframework.samples.petclinic.visits:spring-petclinic-visits-service:+:stubs:8082"
    },
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class ApiGatewayContractTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldGetOwnerDetails() {
        webTestClient.get()
            .uri("/api/gateway/owners/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.firstName").isEqualTo("George")
            .jsonPath("$.lastName").isEqualTo("Franklin");
    }
}
```

#### 2.3 Run Consumer Tests

```bash
# 確保 producer stubs 已安裝到本地 Maven 倉庫
./mvnw clean install -pl spring-petclinic-customers-service,spring-petclinic-visits-service

# 執行 consumer tests
./mvnw clean test -pl spring-petclinic-api-gateway
```

---

### 3. Development Workflow

```
┌────────────────────────────────────────────────────────────────┐
│                    Contract Testing Workflow                    │
└────────────────────────────────────────────────────────────────┘

1. 定義 Contract
   └─> src/test/resources/contracts/*.groovy

2. 建立 Base Test Class
   └─> src/test/java/.../contracts/*Base.java

3. 執行 Producer Tests
   └─> mvn test (產生測試並驗證)

4. 發布 Stubs
   └─> mvn install (產生 -stubs.jar)

5. Consumer 使用 Stubs
   └─> @AutoConfigureStubRunner 載入 stubs

6. 執行 Consumer Tests
   └─> mvn test (驗證整合)
```

---

### 4. Common Commands

```bash
# 執行所有 contract tests
./mvnw clean verify

# 僅產生 contracts (不執行測試)
./mvnw spring-cloud-contract:generateTests

# 產生 stubs jar
./mvnw spring-cloud-contract:generateStubs

# 執行特定服務的 contract tests
./mvnw clean test -pl spring-petclinic-customers-service

# 清理並重新建置所有 stubs
./mvnw clean install -DskipTests=false
```

---

### 5. Troubleshooting

#### Base Class Not Found

```
Error: Cannot find base class for generated tests
```

**解決方案**: 確認 `packageWithBaseClasses` 配置正確，且 Base class 名稱符合目錄對應規則。

#### Stub Download Failed

```
Error: Could not find artifact with stubs
```

**解決方案**:
1. 執行 `./mvnw install` 在 producer 服務
2. 確認 stub coordinates 正確

#### Contract Test Failed

```
Error: Expected status 200 but was 404
```

**解決方案**: 檢查 Base class 的 mock 設定是否正確返回預期資料。

---

**Version**: 1.1 | **Status**: Complete (Updated with Contract Testing)
