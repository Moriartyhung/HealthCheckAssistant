# Backend v1 — Phase 1–5

提供 Patient Exams GET、Next Exam GET、WS1 POST、WS2 POST 與 Admin Progress GET，使用 JdbcTemplate 存取既有 Oracle 19c `HEALTH_CHECK`。5 / 5 main APIs implemented；Frontend 已預設 Real API mode，真 Oracle 瀏覽器驗收狀態見 PROJECT_STATUS 的 Integration Phase。沒有 JPA 或 Security。

## 環境與版本

Java 17+；java -version 顯示 23.0.1，但 Maven Wrapper 依 JAVA_HOME 實際以 Java 17.0.12 編譯 release 17（本機沒有 Java 21）。Spring Boot 3.5.9、Spring Web/JDBC/Bean Validation、Oracle ojdbc11、springdoc-openapi 2.8.14、Spring Boot Test。官方 Maven Wrapper 3.3.4 下載 Maven 3.9.11；首次執行需網路。`mvn -version` 本機不可用，使用 Wrapper。

## 設定與啟動

在同一個 PowerShell session 執行；密碼由本機安全提示輸入，不寫進檔案或命令歷史：

```powershell
cd D:\HealthCheckAssistant_V1_Latest\backend
$env:DB_URL = 'jdbc:oracle:thin:@localhost:1521:orcl'
$env:DB_USERNAME = 'HEALTH_CHECK'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Oracle password' -AsSecureString)).Password
.\mvnw.cmd spring-boot:run
```

預設 port 8080，可使用 `SERVER_PORT` 更改。DB_URL、DB_USERNAME 有上述預設值；DB_PASSWORD 必填。Oracle listener 若實際為 service name，確認後才改成 `jdbc:oracle:thin:@//localhost:1521/orcl`。本程式不建表、不執行 Schema/seed。

`.env.example` 只是設定範例，Spring Boot 不會自動載入 `.env`。本機設定檔、target 與密碼檔由 `.gitignore` 排除。

## 測試與編譯

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Service 與 Controller/Repository 測試使用 mock，不需 Oracle 或 DB_PASSWORD。Repository 測試涵蓋 bind parameter、排序 SQL、NULL/TIMESTAMP 轉換；不取代真實 Oracle integration test。

## Swagger 與 Patient API

- Swagger UI：http://localhost:8080/swagger-ui/index.html
- OpenAPI：http://localhost:8080/v3/api-docs

Swagger 的 Patient Exams GET 可輸入 P001 / P999 並 Execute。也可在另一個 PowerShell 執行：

```powershell
$result = Invoke-RestMethod 'http://localhost:8080/api/patients/P001/exams'
$result | ConvertTo-Json -Depth 5
# 原始 seed P001 有 5 項；completedCount/status/completedAt 以 DB 現況為準。
try { Invoke-RestMethod 'http://localhost:8080/api/patients/P999/exams' } catch {
    $_.Exception.Response.StatusCode
    $_.ErrorDetails.Message
}
```

unknown patient：404 `{"message":"找不到受檢者"}`；空白/過長 ID：400 `{"message":"請求參數不正確"}`；未預期錯誤：500 `{"message":"服務暫時無法使用"}`。SQL 在 Repository，使用 bind parameter，項目按 examId 排序。

completedAt：DB NULL → JSON null；Oracle TIMESTAMP → ISO 8601 本地日期時間，例如 `2026-10-01T10:30:00`。原欄位無時區，因此不添加 Z 或臆測 UTC。

## 驗證狀態

請見根目錄 PROJECT_STATUS.md 的 Backend Phase 1。沒有 DB_PASSWORD 時，只能完成單元測試與 Swagger 啟動驗證；P001/P999 真實 Oracle 結果必須待本機設定密碼後取得。

16 tests / package 通過。Oracle Driver 正常載入；完整 context Swagger 測試通過。JAR 在 18080 啟動，Swagger UI 與 OpenAPI 實際 HTTP 200，未知路徑 404 / 不支援方法 405，臨時服務已停止。
啟動使用非真實密碼佔位值僅驗證 Swagger，未呼叫 DB API；不代表 Oracle 連線成功。

## Backend Phase 2 - WS1

新增 `POST /api/ws1/check`，沿用前述 Java、環境變數、Wrapper 與啟動方式。既有 Patient Exams GET 保持可用；Frontend 仍啟用 MSW。

Swagger 會顯示 `ws1-controller`，可輸入：

```json
{"patientId":"P001","stationId":"S002"}
```

也可在 PowerShell 執行：

```powershell
$body = @{ patientId = 'P001'; stationId = 'S002' } | ConvertTo-Json
Invoke-RestMethod 'http://localhost:8080/api/ws1/check' -Method Post -ContentType 'application/json' -Body $body | ConvertTo-Json -Depth 5
```

請求缺欄位、空白、null、超長或非字串代碼、JSON 格式錯誤：HTTP 400 `{"message":"請提供受檢者與站點代碼"}`。未知患者／站點：HTTP 404，訊息分別為「找不到受檢者」「找不到站點」。

業務判斷順序：患者存在 → 站點存在 → 安排此站 → 尚未完成 → 時段 → 前置項目全部完成 → INSERT confirmation。同患者／站點意外存在多項檢查會記錄設定錯誤並回 generic 500，不自行挑第一筆。

未安排、已完成、時段關閉、前置未完成均為 HTTP 200 / allowed=false / code1=null，remainingExams 保留全部 PENDING 並按 EXAM_ID 排序。允許時回 examId/examName、UUID code1，remainingExams 排除本項；在 `@Transactional` Service 中寫入 CHECK_CONFIRMATION（CREATED_AT=SYSTIMESTAMP、USED_AT=NULL），**不更新 PATIENT_EXAM**。

時間：注入 `Clock.system(ZoneId.of("Asia/Taipei"))`。DB HH:mm 嚴格轉 LocalTime，`START_TIME <= now <= END_TIME` 包含相等邊界，保留時鐘秒／奈秒精度（例如 16:00:00 可通過，16:00:01 已超過 16:00）。測試使用 fixed Clock，production 沒有測試時鐘切換。

前置檢查使用 NOT EXISTS completed patient row；前置項目未安排给該患者，也視為未滿足。

### 真實 Oracle smoke test

若 8080 已有 Phase 1 服務，在**原本已設定密碼的 PowerShell session** 停止舊服務並重新執行 `./mvnw.cmd spring-boot:run`。OpenAPI 應同時列出 Patient Exams GET 與 WS1 POST。

本次初始查詢舊服務：P001 HTTP 200 / totalCount 5 / completedCount 0。Phase 2 真實 smoke 狀態見 PROJECT_STATUS；單元測試不代替 Oracle 實測。

人工测试若已超過 S002 08:00–16:00，HTTP 200 / allowed=false /「不在允許檢查時間（08:00–16:00）」是正確結果。不要改 DB 時段、系統時間或跳過判斷；allowed path 已由固定時間測試驗證。

開放時段若產生 code1，可自行在 Oracle 查（bind 該次 token）：

```sql
SELECT CODE1, PATIENT_ID, EXAM_ID, CREATED_AT, USED_AT
FROM CHECK_CONFIRMATION WHERE CODE1 = :code1;
```

應有正確 patient/exam、USED_AT=NULL。確認碼保留，不清空表；不執行尚未完成的 WS2。

### 測試與限制

Phase 2 完整 `mvnw.cmd test` / `package`：59 tests，0 failures/errors/skipped。涵蓋固定時間、兩端邊界、拒絕流程、驗證、Repository bind/mapping、交易提交／回滾、Phase 1 回歸、Swagger 與台北 Clock。全部不需真 Oracle 或 DB_PASSWORD。

Prototype v1 不限制同 patient/exam 同時有多個未使用 code1；重複 WS1 會生成新的持久化 UUID。code1 尚未實作到期或消耗。WS2、Next Exam、Admin Progress 尚未實作；下一階段 Backend Phase 3 - WS2。


### 最新 Oracle smoke 結果

2026-10-01 21:18:36 台北時間，重啟後的 8080 真實 DB API：P001/S002、P001/S005 正常時段拒絕 HTTP 200 / code1=null；P002/S005 正常未安排拒絕；未知患者／站點 404，{} 400。Phase 1 P001 仍 5 項、完成 0。
Oracle smoke test 成功驗證 business denial。allowed path 由 fixed Clock unit tests 驗證；真實 confirmation INSERT 因晚間關站未產生，可於開放時段額外檢查。

## Backend Phase 3 - WS2

新增 `POST /api/ws2/complete`，body：

```json
{"code1":"由真正 WS1 取得的確認碼"}
```

成功 HTTP 200：`{"success":true,"message":"檢查完成狀態已更新"}`。

| 情況 | HTTP | message |
| --- | --- | --- |
| 缺少、空白、非字串、超長 code1 或無效 JSON | 400 | 請提供 code1 |
| 無效確認碼 | 404 | 無效的確認碼，請重新判斷 |
| 確認碼已使用／claim 被其他 request 搶先取得 | 409 | 確認碼已使用，請勿重複提交 |
| 確認碼未用但 exam 已完成／其他 token 搶先完成 | 409 | 此檢查已完成 |
| DB 或資料設定異常 | 500 | 服務暫時無法使用 |

`@Transactional` Service：讀 confirmation exists/unused → 讀 patient/exam PENDING → conditional claim `WHERE CODE1=? AND USED_AT IS NULL` → conditional complete `WHERE PATIENT_ID=? AND EXAM_ID=? AND STATUS='PENDING'`。兩筆更新均檢查 affected rows=1；0 rows throw Runtime ConflictException，交易回滾。

USED_AT 與 COMPLETED_AT 都使用 Oracle SYSTIMESTAMP；exam STATUS 與 COMPLETED_AT 同一 SQL 更新，符合既有 constraint。若第二步更新失敗，前面的 token claim 也回滾；另一個未用 token 若指向已完成 exam，不消耗該 token。

WS2 不重查時段、前置或站點，不使用 WS1 business Clock，也沒有 token expiration。避免 WS1 合法進站後，完成時因關站而被拒絕。

### Swagger／PowerShell smoke

原持有 DB_PASSWORD 的 session 重啟 `./mvnw.cmd spring-boot:run`；Swagger 會新增 `ws2-controller`。沒有可用真實 code1 時至少測：

```powershell
$body = @{ code1 = 'INVALID-CODE-TEST' } | ConvertTo-Json
try {
    Invoke-RestMethod 'http://localhost:8080/api/ws2/complete' -Method Post -ContentType 'application/json' -Body $body
} catch {
    $_.Exception.Response.StatusCode
    $_.ErrorDetails.Message
}
```

應為 404 /「無效的確認碼，請重新判斷」。本次實際結果见 PROJECT_STATUS Phase 3。

### 手動真實 Oracle E2E

一般 `mvnw.cmd test` 不連 Oracle。新增的 `Ws2OracleSmokeIT` **不在一般 test/package 的命名匹配中**，只在你於已設定 DB_URL/DB_USERNAME/DB_PASSWORD 的 PowerShell 明確執行時測真 Oracle：

```powershell
.\mvnw.cmd '-Dtest=Ws2OracleSmokeIT' test
```

此測試啟動臨時隨機 HTTP port，測 invalid-code 404，接著查最新 `USED_AT IS NULL` 的既有 token。若沒有 token，E2E 明確 skipped；不插入 fake confirmation。若最新 token 指向已完成 exam，驗證 409 且不消耗，success E2E 明確 skipped。

若最新 token 對應 PENDING exam，測試會**真正完成該 exam 並永久消耗這個 token**，驗證 DB STATUS/COMPLETED_AT/USED_AT、Patient API completedCount+1 與相同 code1 replay 409；沒有自動還原。此手動測試目前只是已編譯的工具，是否實際執行以 PROJECT_STATUS 為準。

也可自行在 SQL Developer 先查：

```sql
SELECT CODE1, PATIENT_ID, EXAM_ID, CREATED_AT, USED_AT
FROM CHECK_CONFIRMATION
WHERE USED_AT IS NULL
ORDER BY CREATED_AT DESC;
```

沒有 token 時，在下一個允許檢查時段用真正 WS1 取得碼，再呼叫 WS2；不要修改時段或造碼。完成後確認該 PATIENT_EXAM STATUS='COMPLETED' / COMPLETED_AT 非空，以及該 confirmation USED_AT 非空；重送同碼應 409。成功資料保留。

### 驗證與下一步

普通 test/package：99 tests，0 failures/errors/skipped。WS2 tests 涵蓋成功、400/404/409/500、used/complete guards、claim/exam races、conditional SQL、交易提交與回滾、共用 parser/錯誤處理回歸。Phase 1/2 tests 保留；OpenAPI 現有三支 endpoint。

以上為 Phase 3 當時紀錄。Next Exam 現已於 Phase 4 完成；Admin Progress 與 Frontend real API integration 待後續階段，Frontend 繼續 MSW。


### Phase 3 最新真實驗證

2026-10-01 21:39:55 台北時間，新版8080：invalid-code Oracle path已驗證（404 / 無效的確認碼，請重新判斷），WS2 {} 400 / 請提供 code1；WS1與Patient回歸正常，P001 5項/完成0。Swagger HTTP200，三個paths。
success Oracle path與重送409待合法code1；success path已由automated tests驗證。本次沒有直接讀完整unused list或執行manualIT，未假設整張表沒有未用碼，未造碼或還原資料。


## Backend Phase 4 - Next Exam

API：GET /api/patients/{patientId}/next。沿用 PatientRepository 全部項目查詢；按 STATUS=PENDING、時段、前置條件篩選，EXAM_ID ASC 取第一筆。只執行 SELECT，不產生 code1 或改變完成狀態。

WS1 與 Next 共用 ExamEligibility 及 ExamPrerequisiteQueries。前置條件必須有該患者 STATUS=COMPLETED 的 PATIENT_EXAM；未安排的前置項目也會阻擋。Production 沿用 Asia/Taipei Clock，HH:mm 嚴格解析為 LocalTime，開始與結束相等邊界包含；保留秒／奈秒精度，例如 11:00:01 晚於 11:00 結束時間。

| 條件 | HTTP | nextExam | message |
| --- | --- | --- | --- |
| 有 eligible exam | 200 | 完整 Exam，含 completedAt:null | 建議前往以下站點 |
| 沒有 pending（含無安排項目） | 200 | null | 所有檢查已完成，謝謝您的配合！ |
| 有 pending 但目前不可做 | 200 | null | 目前沒有可進行的檢查，請洽工作人員 |
| 患者不存在 | 404 | 錯誤回應無此欄位 | 找不到受檢者 |

Swagger /swagger-ui/index.html 有 next-exam-controller，/v3/api-docs 現有四支 API，200 描述三種業務結果並含 400/404/500 error schema。

測試：新增 22 項，合計 121 項；mvnw.cmd test 與 clean package 驗證。Fixed Taipei Clock 涵蓋排序、completed、closed、start/end、邊界外一秒、blocked/satisfied/missing prerequisites、全部完成、空列表、無可用項目、invalid DB time；Controller 驗 JSON 完整欄位/null、404、400、safe500。PatientRepository 新增參數綁定與 NOT EXISTS completed prerequisite SQL 測試；既有 Phase 1/2/3 測試保留。

真實 Oracle smoke 必須在已設定 DB_PASSWORD 的原 session 重啟新版後執行：

~~~powershell
Invoke-RestMethod 'http://localhost:8080/api/patients/P001/next'
try { Invoke-RestMethod 'http://localhost:8080/api/patients/P999/next' } catch {
    $_.ErrorDetails.Message
}
~~~

晚間 P001 尚有 pending 時，nextExam:null /「目前沒有可進行的檢查，請洽工作人員」為正確結果。不得修改 DB 時段、系統時間或 Production Clock 來強制出現推薦。真實結果見 PROJECT_STATUS.md 最新 Phase 4 節。

Frontend/MSW、Schema/seed 保持不變；剩餘 Admin Progress、Frontend real API integration。下一階段 Backend Phase 5 - Admin Progress。合法 WS1→WS2 真實成功 E2E 仍待開放時段，Phase 4 不改其 DB 狀態。

Phase 4 真實驗證（2026-10-01 22:05:45 +08:00）：P001 next HTTP200 / nextExam=null /「目前沒有可進行的檢查，請洽工作人員」；P999 next HTTP404 /「找不到受檢者」。晚間所有 P001 pending 已過開放時間，結果正確。Patient P001回歸0/5，Swagger/OpenAPI HTTP200四支。完整狀態见 PROJECT_STATUS.md。


## Backend Phase 5 - Admin Progress

`GET /api/admin/progress` 無 body，HTTP200 application/json array，且僅回 patientId、patientName、completedCount、totalCount。單次 JdbcTemplate SQL 從 PATIENT LEFT JOIN PATIENT_EXAM 聚合，COUNT(PE.EXAM_ID) 計總項目，NVL(SUM(CASE WHEN PE.STATUS='COMPLETED' THEN 1 ELSE 0 END),0) 計完成數；PATIENT_ID ASC。未安排項目仍列出 0/0；無病人回 []。沒有 UI 衍生欄位或 side effect；DB 失敗沿用 generic500「服務暫時無法使用」。

Swagger 新增 admin-controller，可直接 Execute。普通 test / clean package：128 tests，0 failures/errors/skipped，BUILD SUCCESS；Phase 1–4 無回歸。SQL shape/row mapping 使用 mock tests，不等同真實 SQL integration。

本機 Wrapper 修正 Windows PowerShell 的空 Target 索引；若需使用專案既有 Maven cache：

```powershell
$env:MAVEN_USER_HOME = 'D:\HealthCheckAssistant_V1_Latest\.maven-user'
.\mvnw.cmd '-Dmaven.repo.local=D:\HealthCheckAssistant_V1_Latest\.maven-cache' test
.\mvnw.cmd '-Dmaven.repo.local=D:\HealthCheckAssistant_V1_Latest\.maven-cache' clean package
```

真實 Oracle smoke 尚待新版服務啟動（agent 無 DB_PASSWORD）。使用前述安全輸入方式或原已設定密碼的 session 啟動，然後：

```powershell
Invoke-RestMethod 'http://localhost:8080/api/admin/progress' | ConvertTo-Json
Invoke-RestMethod 'http://localhost:8080/api/patients/P001/exams' | ConvertTo-Json -Depth 5
Invoke-RestMethod 'http://localhost:8080/api/patients/P002/exams' | ConvertTo-Json -Depth 5
```

核對各患者 completedCount/totalCount 與目前 PATIENT_EXAM 真實狀態，不硬期待 seed 完成數，不修改 DB。完整結果以 PROJECT_STATUS 為準。Backend v1：5 / 5 main APIs implemented；Frontend 繼續 MSW，真 API integration / full E2E 尚未完成。下一階段 Integration Phase，本次不執行。


Phase5 / Integration最新真實驗收（2026-10-02 21:21–21:24 Asia/Taipei）：已由新版8080取得Admin HTTP200，P001 0/5、P002 0/3，與Oracle-backed Patient exams行數及COMPLETED數一致。Vite proxy與Browser Admin畫面相同、5秒polling正常。前述「尚待服務啟動」為當時紀錄；目前Admin smoke已完成。完整Integration結果見PROJECT_STATUS。
