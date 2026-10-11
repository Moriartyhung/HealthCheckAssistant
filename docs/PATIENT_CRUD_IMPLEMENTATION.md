# V1 PATIENT CRUD 實作與驗證紀錄

此檔保留基本 CRUD 階段的歷史結果。後續健檢安排擴充及最新驗證請見 [PATIENT_EXAM_MANAGEMENT_IMPLEMENTATION.md](PATIENT_EXAM_MANAGEMENT_IMPLEMENTATION.md)。

日期：2026-10-09（Asia/Taipei）。範圍僅 PATIENT 資料管理，沿用既有 Spring Boot JdbcTemplate → Repository → Service → Controller 與 React /admin。

## API 與操作入口

| Method | Endpoint | 成功回應 |
| --- | --- | --- |
| POST | /api/admin/patients | 201 `{patientId, patientName}`，含 Location |
| GET | /api/admin/patients | 200 受檢者陣列，病歷號排序 |
| GET | /api/admin/patients/{patientId} | 200 `{patientId, patientName}` |
| PUT | /api/admin/patients/{patientId} | 200 `{patientId, patientName}`；body 僅 patientName |
| DELETE | /api/admin/patients/{patientId} | 200 `{message:"受檢者已刪除"}` |

Web：管理後台 `/admin`，原進度總覽下方的「受檢者資料管理」。上方新增／編輯表單、中間刪除確認、下方病歷號查詢與列表；每列有編輯及刪除按鈕，並可顯示全部。

病歷號不可修改；PUT 含 patientId 或未知欄位回 400。字串型別、空白、控制字元、字元及 Oracle BYTE 長度皆驗證；姓名與新增病歷號移除前後空白。病歷號拒絕路徑分隔符、分號及相對路徑。新增先查重，並處理唯一鍵並行衝突為 409。不存在回 404。

刪除是單一參數化 DELETE，以 NOT EXISTS 同時檢查 PATIENT_EXAM 和 CHECK_CONFIRMATION，Oracle 外鍵另保護並行插入關聯；被引用回 409，不使用 CASCADE。

正式程式使用現有 API client 與 Oracle datasource，未加入替代資料、管理 Mock handler 或密碼。MSW 模式下管理頁停用，管理 API adapter 也會在 fetch 前拒絕操作。自動化測試中的 JDBC／fetch doubles 僅用於隔離外部服務，不代表 Oracle 驗收。

## 實際執行結果

| 驗證 | 指令（在對應目錄） | 實際結果 |
| --- | --- | --- |
| 後端完整測試與契約快照 | backend：`./mvnw.cmd -Dopenapi.snapshot=../api-docs.json test` | 143 tests，0 failures、0 errors、0 skipped；BUILD SUCCESS |
| 後端 JAR 建置 | backend：`./mvnw.cmd -DskipTests package` | BUILD SUCCESS；此步跳過測試，測試結果來自上一列 |
| 前端完整測試 | frontend：`npm test` | 6 test files，24 tests 全部通過 |
| 前端 TypeScript 與 production build | frontend：`npm run build` | 成功 |
| 差異格式 | 根目錄：`git diff --check` | 無差異格式錯誤 |
| 真 Oracle 整合測試 | `PatientManagementOracleIT` | 未執行；目前沒有 DB_PASSWORD |

新增一般後端測試 15 項（API 14、Repository 1），前端測試 7 項（管理介面 6、Mock 防護 1）。既有後端／前端測試也一起執行。Swagger 成功回應 schema、原五支 API 路徑及錯誤契約由 ApplicationSmokeTest 檢查。

測試先確認缺少路由／操作行為會失敗，再加入實作；補強測試曾找出病歷號路由歧義及 Swagger 成功 schema 缺漏，修正後完整回歸通過。沙箱內曾遇到 Maven 快取權限、JVM 啟動停滯及 Vitest 暫存檔 ENOENT；沙箱外重跑成功，以上列出的是最後成功執行結果。

## 尚未驗證

- 未實際連接 Oracle，未驗證實際資料庫帳號權限、字元集、SQL 執行及並行外鍵衝突。
- 未執行瀏覽器 → Spring Boot → Oracle 的完整 CRUD 驗收；Web 操作已以自動化介面測試驗證。
- 可在安全設定 DB_PASSWORD 後執行 `./mvnw.cmd -Dtest=PatientManagementOracleIT test`。共有三個 opt-in Oracle 測試，PATIENT 變更在測試交易中 rollback，不寫入其他五張表；關聯拒刪案例需要既有 PATIENT_EXAM，byte 案例需要資料庫字元集令測試姓名超過 100 bytes，不滿足條件會明確跳過。

目前沒有已知未修正的 CRUD 實作問題；Oracle 實測仍待完成。未修改 schema／seed、WS1／WS2、下一站推薦、既有進度查詢實作或兩個 Android App，也未加入 V2 功能。

## 變更檔案

| 狀態 | 檔案 | 用途 |
| --- | --- | --- |
| 新增 | [PatientManagementController.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/admin/PatientManagementController.java) | 五個 CRUD 操作與 Swagger |
| 新增 | [PatientManagementService.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/admin/PatientManagementService.java) | 驗證、查重、姓名更新、刪除拒絕 |
| 新增 | [PatientManagementRepository.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/admin/PatientManagementRepository.java) | Oracle 參數化 SQL |
| 新增 | [CreatePatientRequest.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/admin/dto/CreatePatientRequest.java) | 新增 DTO |
| 新增 | [UpdatePatientRequest.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/admin/dto/UpdatePatientRequest.java) | 只接受姓名的更新 DTO |
| 新增 | [PatientDeleteResponse.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/admin/dto/PatientDeleteResponse.java) | 刪除成功回應 |
| 新增 | [InvalidPatientDataException.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/common/error/InvalidPatientDataException.java) | 非法資料錯誤 |
| 修改 | [GlobalExceptionHandler.java](D:/HealthCheckAssistant/backend/src/main/java/com/healthcheckassistant/common/error/GlobalExceptionHandler.java) | 管理 API 的 400 訊息；保留既有錯誤訊息 |
| 新增 | [PatientManagementApiTest.java](D:/HealthCheckAssistant/backend/src/test/java/com/healthcheckassistant/admin/PatientManagementApiTest.java) | CRUD 全分層 API 與錯誤測試 |
| 新增 | [PatientManagementRepositoryTest.java](D:/HealthCheckAssistant/backend/src/test/java/com/healthcheckassistant/admin/PatientManagementRepositoryTest.java) | 欄位映射、排序、參數綁定 |
| 新增 | [PatientManagementOracleIT.java](D:/HealthCheckAssistant/backend/src/test/java/com/healthcheckassistant/admin/PatientManagementOracleIT.java) | 可回滾的真 Oracle opt-in 測試 |
| 修改 | [ApplicationSmokeTest.java](D:/HealthCheckAssistant/backend/src/test/java/com/healthcheckassistant/patient/ApplicationSmokeTest.java) | 新舊 API 契約及可選 Swagger 快照輸出 |
| 新增 | [patientManagementApi.ts](D:/HealthCheckAssistant/frontend/src/api/patientManagementApi.ts) | 真實管理 API client 與 Mock 防護 |
| 新增 | [patientManagementApi.test.ts](D:/HealthCheckAssistant/frontend/src/api/patientManagementApi.test.ts) | Mock 模式不得呼叫管理 API |
| 新增 | [PatientManagement.tsx](D:/HealthCheckAssistant/frontend/src/pages/admin/PatientManagement.tsx) | 管理介面與資料重新查詢 |
| 新增 | [PatientManagement.test.tsx](D:/HealthCheckAssistant/frontend/src/pages/admin/PatientManagement.test.tsx) | CRUD 按鈕操作與失敗狀態 |
| 修改 | [AdminPage.tsx](D:/HealthCheckAssistant/frontend/src/pages/admin/AdminPage.tsx) | 嵌入管理區塊 |
| 修改 | [queries.ts](D:/HealthCheckAssistant/frontend/src/hooks/queries.ts) | 管理列表快取 key |
| 修改 | [index.css](D:/HealthCheckAssistant/frontend/src/index.css) | 管理表單與確認區塊排版 |
| 修改 | [API_CONTRACT.md](D:/HealthCheckAssistant/docs/API_CONTRACT.md) | CRUD 契約、限制及測試方式 |
| 修改 | [api-docs.json](D:/HealthCheckAssistant/api-docs.json) | 由實際 Swagger 產生的契約快照 |
| 新增 | [PATIENT_CRUD_IMPLEMENTATION.md](D:/HealthCheckAssistant/docs/PATIENT_CRUD_IMPLEMENTATION.md) | 本次變更與驗證紀錄 |
