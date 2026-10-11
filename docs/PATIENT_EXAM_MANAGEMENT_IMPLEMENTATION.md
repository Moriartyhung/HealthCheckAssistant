# V1 受檢者健檢項目管理實作紀錄

驗證日期：2026-10-09。本次在既有 PATIENT CRUD 上擴充安排管理，沿用 JdbcTemplate → Repository → Service → Controller。正式功能讀寫 Oracle，測試 doubles 只存在測試程式。API 格式與限制詳見 [API_CONTRACT.md](API_CONTRACT.md)。

## API 與介面

| Method | Endpoint | 行為 |
| --- | --- | --- |
| POST | /api/admin/patients | 建立病人與 examIds 安排，201 詳細資料 |
| GET | /api/admin/patients | 基本資料列表，保持既有格式 |
| GET | /api/admin/patients/{patientId} | 詳細安排、站點、狀態、完成時間、移除限制 |
| PUT | /api/admin/patients/{patientId} | 姓名與完整目標 examIds，200 詳細資料 |
| DELETE | /api/admin/patients/{patientId} | 無關聯才可刪除；關聯衝突 409 |
| GET | /api/admin/exams | 真實項目、站點、時段、前置項目清單 |

`/admin` 原進度總覽下方「受檢者資料管理」：上方新增／編輯表單提供多選項目，下方查詢與列表提供「查看安排」、「編輯」、「刪除」。明細顯示站點、完成狀態、完成時間及不可移除原因；刪除須確認。病歷號不可改，不提供狀態或時間編輯。清單載入失敗禁止儲存，MSW 模式停用管理功能。

POST、PUT 以同一交易更新 PATIENT 與 PATIENT_EXAM；新安排固定 PENDING、完成時間 null。PUT 省略或 null examIds 保留安排，[] 要求移除全部但仍受保護限制。重複／無效代碼 400；缺前置項目、循環、同站多項、受保護項目移除、外鍵衝突 409。修改鎖定病人與既有安排，條件 DELETE 及外鍵再次保護並行衝突。

已完成項目不能移除。確認碼沒有到期欄位，USED_AT IS NULL 視為有效；歷史確認碼也受既有外鍵保護，不透過安排管理刪除。EXAM_ITEM 可以有同站不同項目，但目前 WS1 要求一位病人每站只有一項，因此管理 API 拒絕同病人同站多項，不改 WS1 行為。

## 本階段新增檔案

- backend/src/main/java/com/healthcheckassistant/admin/ExamCatalogController.java
- backend/src/main/java/com/healthcheckassistant/admin/dto/ExamCatalogItem.java
- backend/src/main/java/com/healthcheckassistant/admin/dto/ManagedPatientExam.java
- backend/src/main/java/com/healthcheckassistant/admin/dto/PatientDetailResponse.java
- backend/src/test/java/com/healthcheckassistant/admin/PatientArrangementApiTest.java
- backend/src/test/java/com/healthcheckassistant/admin/PatientArrangementOracleIT.java
- frontend/src/pages/admin/PatientArrangement.test.tsx
- docs/PATIENT_EXAM_MANAGEMENT_IMPLEMENTATION.md

## 本階段修改檔案

- backend/src/main/java/com/healthcheckassistant/admin/PatientManagementRepository.java
- backend/src/main/java/com/healthcheckassistant/admin/PatientManagementService.java
- backend/src/main/java/com/healthcheckassistant/admin/PatientManagementController.java
- backend/src/main/java/com/healthcheckassistant/admin/dto/CreatePatientRequest.java
- backend/src/main/java/com/healthcheckassistant/admin/dto/UpdatePatientRequest.java
- backend/src/main/java/com/healthcheckassistant/common/error/GlobalExceptionHandler.java
- backend/src/test/java/com/healthcheckassistant/admin/PatientManagementApiTest.java
- backend/src/test/java/com/healthcheckassistant/patient/ApplicationSmokeTest.java
- frontend/src/api/patientManagementApi.ts
- frontend/src/api/patientManagementApi.test.ts
- frontend/src/pages/admin/PatientManagement.tsx
- frontend/src/pages/admin/PatientManagement.test.tsx
- frontend/src/hooks/queries.ts
- frontend/src/index.css
- docs/API_CONTRACT.md
- docs/PATIENT_CRUD_IMPLEMENTATION.md
- api-docs.json（從 Spring OpenAPI 實際回應產生）

以上依本階段起始狀態列出；工作目錄亦包含先前 PATIENT CRUD 尚未提交的檔案。未修改 schema／seed、原五支健檢 API 實作、兩個 Android App，未加入 V2。

## 已執行驗證

| 驗證 | 命令／範圍 | 結果 |
| --- | --- | --- |
| 後端測試及建置 | backend：`./mvnw.cmd '-Dopenapi.snapshot=../api-docs.json' package` | 158 tests，0 failures/errors/skipped，JAR BUILD SUCCESS |
| 前端測試 | frontend：`npm.cmd test` | 7 files、31 tests passed |
| 前端正式建置 | frontend：`npm.cmd run build` | TypeScript 與 Vite 成功 |
| Oracle opt-in 測試可用性 | backend：`./mvnw.cmd '-Dtest=PatientArrangementOracleIT,PatientManagementOracleIT' test` | 5 tests 全部 skipped，缺 DB_PASSWORD，沒有 Oracle 連線 |

15 個安排 API 測試使用真實 Controller／Service／Repository，以 JdbcTemplate 與交易管理器 doubles 隔離外部資料庫；涵蓋多項新增查詢、姓名及新增項目、移除未完成、已完成／有效與歷史確認碼拒絕、前置條件與循環、同站限制、重複與無效項目、禁止狀態時間寫入、新增及修改失敗回滾、外鍵競爭衝突、舊姓名更新相容性。這是交易邊界與行為測試，不代表 Oracle 實測。

7 個安排 UI 測試涵蓋清單新增、編輯與完成保護、安排明細、清單失敗禁止儲存、編輯時查看另一位病人的明細、有效 WS1 確認碼鎖定、缺前置項目阻止儲存。完整後端測試亦保留原五支 API、WS1／WS2 回歸與契約檢查。

## 尚未驗證與人工確認

- 未連線 Oracle；只檢查 repository 中的 DDL，未比對實際 schema、帳號權限、字元集、SQL、FOR UPDATE 與 WS1／WS2 並行情境。
- 未執行瀏覽器 → Spring Boot → Oracle 完整驗收。安全設定 DB_PASSWORD 後可執行上述兩組 OracleIT；只操作自身唯一測試病人，不更改既有完成狀態或參考表。
- 新 Oracle 案例需清單有足夠不同站點且符合前置條件的項目，否則明確跳過。CRUD 案例在測試交易回滾；故障案例透過真 HTTP 在第二筆插入前注入失敗，以 JDBC 驗證零殘留，最後只清理自身唯一測試病歷號。尚未實際執行。
- 若既有安排已有同站多項、缺前置項目或循環，完整安排更新會拒絕；舊版只改姓名仍相容。若受保護紀錄使安排無法調整，需人工釐清資料，不自動刪除確認碼或修正既有完成狀態。

目前沒有已知未修正的功能問題；真實 Oracle 整合及並行驗收仍待完成。
