# Feature Specification: 提升測試涵蓋率 (Improve Test Coverage)

**Feature Branch**: `001-improve-test-coverage`
**Created**: 2025-12-10
**Status**: Draft
**Input**: User description: "原有功能不變動，但是要增加測試案例，正反向都要有，提升測試涵蓋率。加入 BDD 的情境測試案例"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customers Service 正向測試 (Priority: P1)

作為開發團隊，我需要為 Customers Service 增加完整的正向測試案例，驗證 Owner 和 Pet 的 CRUD 操作在正常情況下的行為正確。

**Why this priority**: Customers Service 是核心業務服務之一，Owner 和 Pet 是系統最重要的實體，必須優先確保其正向路徑的穩定性。

**Independent Test**: 可以獨立執行 Customers Service 的單元測試和整合測試，驗證所有正向 CRUD 操作返回預期結果。

**Acceptance Scenarios**:

1. **Given** 一個有效的 Owner 資料, **When** 建立新的 Owner, **Then** 系統成功儲存並返回 Owner ID
2. **Given** 一個存在的 Owner ID, **When** 查詢該 Owner, **Then** 系統返回完整的 Owner 資訊
3. **Given** 一個存在的 Owner, **When** 更新 Owner 資訊, **Then** 系統成功更新並返回更新後的資料
4. **Given** 一個存在的 Owner, **When** 為該 Owner 新增 Pet, **Then** 系統成功建立 Pet 並關聯至該 Owner
5. **Given** 一個存在的 Pet, **When** 查詢該 Pet, **Then** 系統返回完整的 Pet 資訊包含類型

---

### User Story 2 - Customers Service 反向測試 (Priority: P1)

作為開發團隊，我需要為 Customers Service 增加完整的反向測試案例，驗證系統在異常輸入或錯誤情況下的處理行為正確。

**Why this priority**: 反向測試與正向測試同等重要，確保系統在邊界情況和錯誤輸入下能正確處理，避免系統崩潰。

**Independent Test**: 可以獨立執行 Customers Service 的錯誤處理測試，驗證所有反向案例返回適當的錯誤訊息。

**Acceptance Scenarios**:

1. **Given** 一個不存在的 Owner ID, **When** 查詢該 Owner, **Then** 系統返回 404 Not Found 錯誤
2. **Given** 一個缺少必填欄位的 Owner 資料, **When** 建立 Owner, **Then** 系統返回 400 Bad Request 錯誤
3. **Given** 一個不存在的 Pet ID, **When** 查詢該 Pet, **Then** 系統返回 404 Not Found 錯誤
4. **Given** 一個無效的 Pet Type, **When** 建立 Pet, **Then** 系統返回適當的驗證錯誤
5. **Given** 一個格式錯誤的 Owner ID, **When** 查詢 Owner, **Then** 系統返回 400 Bad Request 錯誤

---

### User Story 3 - Vets Service 正向測試 (Priority: P2)

作為開發團隊，我需要為 Vets Service 增加正向測試案例，驗證獸醫資料查詢功能正常運作。

**Why this priority**: Vets Service 功能相對簡單（主要是讀取操作），但仍需確保核心查詢功能正確。

**Independent Test**: 可以獨立執行 Vets Service 的單元測試，驗證獸醫列表和詳細資訊查詢返回預期結果。

**Acceptance Scenarios**:

1. **Given** 系統中有獸醫資料, **When** 查詢所有獸醫, **Then** 系統返回完整的獸醫列表
2. **Given** 系統中有獸醫資料, **When** 查詢單一獸醫詳情, **Then** 系統返回該獸醫的完整資訊包含專長

---

### User Story 4 - Vets Service 反向測試 (Priority: P2)

作為開發團隊，我需要為 Vets Service 增加反向測試案例，驗證錯誤處理行為正確。

**Why this priority**: 確保 Vets Service 在異常情況下能正確回應。

**Independent Test**: 可以獨立執行 Vets Service 的錯誤處理測試。

**Acceptance Scenarios**:

1. **Given** 一個不存在的獸醫 ID, **When** 查詢該獸醫, **Then** 系統返回 404 Not Found 錯誤
2. **Given** 一個格式錯誤的獸醫 ID, **When** 查詢獸醫, **Then** 系統返回 400 Bad Request 錯誤

---

### User Story 5 - Visits Service 正向測試 (Priority: P2)

作為開發團隊，我需要為 Visits Service 增加正向測試案例，驗證就診紀錄的 CRUD 操作正常運作。

**Why this priority**: Visits Service 管理重要的就診紀錄，需要確保資料操作正確。

**Independent Test**: 可以獨立執行 Visits Service 的單元測試，驗證就診紀錄的建立和查詢功能。

**Acceptance Scenarios**:

1. **Given** 一個有效的 Pet ID 和就診資料, **When** 建立就診紀錄, **Then** 系統成功儲存並返回紀錄 ID
2. **Given** 一個有就診紀錄的 Pet, **When** 查詢該 Pet 的就診紀錄, **Then** 系統返回所有相關的就診紀錄

---

### User Story 6 - Visits Service 反向測試 (Priority: P2)

作為開發團隊，我需要為 Visits Service 增加反向測試案例，驗證錯誤處理行為正確。

**Why this priority**: 確保 Visits Service 在異常情況下能正確回應。

**Independent Test**: 可以獨立執行 Visits Service 的錯誤處理測試。

**Acceptance Scenarios**:

1. **Given** 一個不存在的 Pet ID, **When** 建立就診紀錄, **Then** 系統返回適當的錯誤訊息
2. **Given** 一個缺少必填欄位的就診資料, **When** 建立就診紀錄, **Then** 系統返回 400 Bad Request 錯誤
3. **Given** 一個不存在的就診紀錄 ID, **When** 查詢該紀錄, **Then** 系統返回 404 Not Found 錯誤

---

### User Story 7 - API Gateway 正向測試 (Priority: P3)

作為開發團隊，我需要為 API Gateway 增加正向測試案例，驗證服務路由和聚合功能正常運作。

**Why this priority**: API Gateway 是系統入口，但其測試依賴於底層服務，優先級略低於核心業務服務。

**Independent Test**: 可以獨立執行 API Gateway 的整合測試（使用 mock 服務）。

**Acceptance Scenarios**:

1. **Given** 底層服務正常運作, **When** 請求 Owner 詳情（含 Pets 和 Visits）, **Then** 系統返回聚合後的完整資料
2. **Given** 底層服務正常運作, **When** 請求獸醫列表, **Then** 系統正確路由並返回結果

---

### User Story 8 - API Gateway 反向測試與熔斷 (Priority: P3)

作為開發團隊，我需要為 API Gateway 增加反向測試案例，特別是服務降級和熔斷機制的測試。

**Why this priority**: 確保系統在部分服務失敗時仍能正常運作。

**Independent Test**: 可以獨立執行 API Gateway 的熔斷測試（模擬服務失敗）。

**Acceptance Scenarios**:

1. **Given** Visits Service 無法連線, **When** 請求 Owner 詳情, **Then** 系統執行熔斷邏輯並返回部分資料（不含 Visits）
2. **Given** 底層服務超時, **When** 請求服務, **Then** 系統在合理時間內返回降級回應
3. **Given** 一個無效的 API 請求, **When** 發送請求, **Then** 系統返回適當的錯誤訊息

---

### User Story 9 - BDD 情境測試：寵物主人管理 (Priority: P1)

作為開發團隊，我需要使用 BDD 框架（如 Cucumber）為寵物主人管理功能撰寫可執行的情境測試，以自然語言描述業務行為並實現自動化驗證。

**Why this priority**: BDD 情境測試是連接業務需求與技術實現的橋樑，確保系統行為符合業務預期，並作為活文檔供所有利害關係人閱讀。

**Independent Test**: 可以獨立執行 BDD 情境測試，驗證寵物主人管理的完整業務流程。

**Acceptance Scenarios**:

```gherkin
Feature: 寵物主人管理
  作為一名診所員工
  我希望能夠管理寵物主人的資料
  以便追蹤寵物和他們的主人

  Scenario: 成功建立新的寵物主人
    Given 系統處於正常運作狀態
    When 我提交以下寵物主人資料：
      | firstName | lastName | address       | city    | telephone   |
      | John      | Doe      | 123 Main St   | Taipei  | 0912345678  |
    Then 系統應該成功建立寵物主人
    And 返回的寵物主人資料應該包含系統指派的 ID

  Scenario: 查詢寵物主人詳細資訊
    Given 系統中存在一位名為 "George Franklin" 的寵物主人
    When 我查詢該寵物主人的詳細資訊
    Then 系統應該返回完整的寵物主人資料
    And 資料應該包含所有關聯的寵物資訊

  Scenario: 無法建立缺少必填欄位的寵物主人
    Given 系統處於正常運作狀態
    When 我提交缺少姓名的寵物主人資料
    Then 系統應該拒絕該請求
    And 返回適當的錯誤訊息說明缺少必填欄位
```

---

### User Story 10 - BDD 情境測試：寵物管理 (Priority: P1)

作為開發團隊，我需要使用 BDD 框架為寵物管理功能撰寫可執行的情境測試。

**Why this priority**: 寵物是系統的核心實體，BDD 測試確保寵物相關操作符合業務規則。

**Independent Test**: 可以獨立執行 BDD 情境測試，驗證寵物管理的完整業務流程。

**Acceptance Scenarios**:

```gherkin
Feature: 寵物管理
  作為一名診所員工
  我希望能夠管理寵物的資料
  以便追蹤每隻寵物的就診紀錄

  Scenario: 為寵物主人新增寵物
    Given 系統中存在一位 ID 為 1 的寵物主人
    When 我為該主人新增以下寵物資料：
      | name    | birthDate  | type |
      | Lucky   | 2020-01-15 | dog  |
    Then 系統應該成功建立寵物
    And 該寵物應該關聯至指定的寵物主人

  Scenario: 查詢寵物的完整資訊
    Given 系統中存在一隻名為 "Leo" 的寵物
    When 我查詢該寵物的詳細資訊
    Then 系統應該返回寵物的完整資料
    And 資料應該包含寵物類型和出生日期

  Scenario: 無法為不存在的寵物主人新增寵物
    Given 系統中不存在 ID 為 9999 的寵物主人
    When 我嘗試為該主人新增寵物
    Then 系統應該拒絕該請求
    And 返回 404 錯誤表示找不到該寵物主人
```

---

### User Story 11 - BDD 情境測試：就診紀錄管理 (Priority: P2)

作為開發團隊，我需要使用 BDD 框架為就診紀錄管理功能撰寫可執行的情境測試。

**Why this priority**: 就診紀錄是診所的重要業務資料，需要確保記錄的準確性和完整性。

**Independent Test**: 可以獨立執行 BDD 情境測試，驗證就診紀錄管理的完整業務流程。

**Acceptance Scenarios**:

```gherkin
Feature: 就診紀錄管理
  作為一名診所員工
  我希望能夠記錄寵物的就診紀錄
  以便追蹤寵物的健康狀況

  Scenario: 為寵物新增就診紀錄
    Given 系統中存在一隻 ID 為 1 的寵物
    When 我為該寵物新增以下就診紀錄：
      | date       | description        |
      | 2025-12-10 | 年度健康檢查       |
    Then 系統應該成功建立就診紀錄
    And 該紀錄應該關聯至指定的寵物

  Scenario: 查詢寵物的所有就診紀錄
    Given 系統中存在一隻有多次就診紀錄的寵物
    When 我查詢該寵物的就診紀錄
    Then 系統應該返回所有相關的就診紀錄
    And 紀錄應該按照日期排序

  Scenario: 無法為不存在的寵物新增就診紀錄
    Given 系統中不存在 ID 為 9999 的寵物
    When 我嘗試為該寵物新增就診紀錄
    Then 系統應該拒絕該請求
    And 返回適當的錯誤訊息
```

---

### User Story 12 - BDD 情境測試：獸醫查詢 (Priority: P2)

作為開發團隊，我需要使用 BDD 框架為獸醫查詢功能撰寫可執行的情境測試。

**Why this priority**: 確保獸醫資料查詢功能符合業務需求。

**Independent Test**: 可以獨立執行 BDD 情境測試，驗證獸醫查詢功能。

**Acceptance Scenarios**:

```gherkin
Feature: 獸醫查詢
  作為一名診所員工或寵物主人
  我希望能夠查詢獸醫資訊
  以便選擇適合的獸醫為寵物看診

  Scenario: 查詢所有獸醫列表
    Given 系統中存在多位獸醫
    When 我查詢所有獸醫
    Then 系統應該返回完整的獸醫列表
    And 每位獸醫資料應該包含其專長資訊

  Scenario: 依專長篩選獸醫
    Given 系統中存在專長為 "radiology" 的獸醫
    When 我查詢專長為放射科的獸醫
    Then 系統應該僅返回具有該專長的獸醫
```

---

### User Story 13 - BDD 情境測試：端對端用戶旅程 (Priority: P3)

作為開發團隊，我需要使用 BDD 框架撰寫端對端用戶旅程測試，驗證完整的業務流程。

**Why this priority**: 端對端測試確保所有服務協作正常，但依賴於所有服務的正常運作。

**Independent Test**: 需要所有微服務運行才能執行此測試。

**Acceptance Scenarios**:

```gherkin
Feature: 完整用戶旅程
  作為一名診所員工
  我希望能夠完成從建立主人到記錄就診的完整流程
  以便高效地服務客戶

  Scenario: 新客戶首次帶寵物就診的完整流程
    Given 系統處於正常運作狀態
    When 我建立一位新的寵物主人 "Jane Smith"
    And 我為該主人新增一隻名為 "Buddy" 的狗
    And 我為該寵物建立一筆就診紀錄
    Then 所有資料應該正確儲存
    And 查詢該主人時應該顯示寵物和就診紀錄

  Scenario: 查詢現有客戶的完整資訊
    Given 系統中存在一位有寵物和就診紀錄的主人
    When 我查詢該主人的完整資訊
    Then 系統應該返回主人資料
    And 包含所有寵物資訊
    And 包含每隻寵物的所有就診紀錄
```

---

### Edge Cases

- 當資料庫連線中斷時，系統應返回 503 Service Unavailable
- 當請求參數包含 SQL 注入嘗試時，系統應正確處理並拒絕
- 當同時處理大量並發請求時，系統應維持正常回應
- 當 Pet 的 Owner 被刪除時，系統應正確處理關聯資料
- 當 Visit 日期為未來日期或過去太久的日期時，系統應如何處理
- BDD 情境測試應能在隔離環境中執行，不影響生產資料

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系統 MUST 為每個微服務的核心業務邏輯提供正向測試案例
- **FR-002**: 系統 MUST 為每個微服務的錯誤處理提供反向測試案例
- **FR-003**: 測試案例 MUST 涵蓋所有 REST API 端點的正向和反向路徑
- **FR-004**: 測試案例 MUST 驗證資料驗證邏輯（必填欄位、格式檢查）
- **FR-005**: 測試案例 MUST 驗證錯誤回應格式和 HTTP 狀態碼
- **FR-006**: API Gateway 測試 MUST 涵蓋熔斷機制和服務降級行為
- **FR-007**: 所有測試 MUST 可獨立執行，不依賴外部服務或資料庫狀態
- **FR-008**: 測試 MUST 遵循 TDD 紅-綠-重構循環原則
- **FR-009**: 原有功能 MUST NOT 被測試案例的新增所影響
- **FR-010**: 系統 MUST 為核心業務功能提供 BDD 情境測試（Gherkin 格式）
- **FR-011**: BDD 情境測試 MUST 使用領域語言撰寫，非技術利害關係人可閱讀
- **FR-012**: BDD 情境測試 MUST 可作為自動化測試執行
- **FR-013**: BDD Feature 檔案 MUST 涵蓋寵物主人管理、寵物管理、就診紀錄、獸醫查詢等核心功能
- **FR-014**: BDD 情境 MUST 包含正向和反向測試場景

### Key Entities

- **Owner**: 寵物主人，包含姓名、地址、電話等基本資訊，可擁有多個 Pet
- **Pet**: 寵物，包含名稱、出生日期、類型，屬於一個 Owner，可有多次 Visit
- **PetType**: 寵物類型，預定義的類型（如狗、貓、鳥等）
- **Vet**: 獸醫，包含姓名和專長列表
- **Specialty**: 獸醫專長（如外科、牙科等）
- **Visit**: 就診紀錄，包含日期、描述，關聯至特定 Pet

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 每個微服務的測試涵蓋率達到至少 80% 的程式碼行數
- **SC-002**: 所有 REST API 端點都有至少一個正向測試和一個反向測試
- **SC-003**: 所有測試案例在 CI/CD 流程中可在 5 分鐘內完成執行
- **SC-004**: 測試執行成功率達到 100%（所有測試通過）
- **SC-005**: API Gateway 熔斷機制測試覆蓋至少 3 種故障場景
- **SC-006**: 所有測試可在無外部依賴的環境下獨立執行
- **SC-007**: 至少 4 個核心功能模組具備完整的 BDD Feature 檔案
- **SC-008**: 每個 BDD Feature 至少包含 3 個情境（正向、反向、邊界）
- **SC-009**: BDD 情境測試 100% 可自動化執行
- **SC-010**: BDD Feature 檔案可作為業務規格文件供非技術人員閱讀

## Assumptions

- 現有功能已經正常運作，測試案例用於驗證和保護現有行為
- 使用 JUnit 5 作為測試框架（符合專案現有技術棧）
- 使用 Mockito 進行單元測試的依賴隔離
- 使用 Spring Boot Test 進行整合測試
- 測試資料使用內嵌資料庫或 mock 資料，不依賴外部資料庫
- BDD 情境測試使用 Cucumber-JVM 或類似框架實現
- Feature 檔案使用繁體中文撰寫，以符合團隊溝通習慣
- BDD Step Definitions 將對應到現有的 REST API 端點
