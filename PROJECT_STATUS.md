# PROJECT_STATUS

更新：2026-10-02（Asia/Taipei）

## 介面視覺改版

- 深墨綠側欄、暖白背景與薄荷綠狀態配色，統一卡片、按鈕、輸入欄、字級與留白。
- Staff 新增圖示流程列與卡片說明；Patient 完成進度與下一站並排；Admin 改善表格標題、姓名識別與摘要卡片。
- 手機使用底部三角色導覽，內容改單欄；Admin 表格保留水平捲動。
- 5 項測試與 production build 通過；瀏覽器重驗 WS1/WS2、Patient 20% 與 390px Admin 版面，無 console error/warn。
- 本次未改 API、Mock 規則或資料庫；三份 database 成果雜湊維持一致。
- 預覽：docs/frontend-staff-redesign.png、docs/frontend-mobile-redesign.png。

## Backend 最新狀態

Backend v1：5 / 5 main APIs implemented，包含 Patient Exams、WS1、WS2、Next Exam、Admin Progress。Phase 5 普通測試及 clean package 共 128 項全數通過，0 failures/errors/skipped。Admin 真實 Oracle smoke 已通過（P001 0/5、P002 0/3）；Frontend 已預設 Real API mode，整合驗收狀態見 Integration Phase。各歷史階段的未完成清單僅代表當時狀態。

## 已完成

- Frontend Prototype v1：React + TypeScript + Vite、官方 shadcn/ui 元件、Tailwind CSS。
- TanStack Query server state、統一 API layer/types/query keys。
- MSW development worker 與五個 API endpoint；WS1 時段/前置條件/重複檢查驗證、WS2 一次性 code1。
- Staff 手動辨識、QR scanner、資格判斷與完成確認。
- Patient 完成進度、已完成/待完成列表、規則式下一站。
- Admin P001/P002 表格、每 5 秒 polling 與手動更新。
- loading/error/success 回饋；WS2 成功 invalidate patient/next/admin queries。
- README、API contract、執行紀錄與 backend 預留說明。

## 目前架構

預設：`pages → hooks / api → fetch /api → Vite proxy :8080 → Spring Boot → Oracle`。明確啟用 Mock：`pages → hooks / api → MSW handlers → mock store`。
UI 不直接讀取 Mock store；production 不啟動或打包 MSW handler。
另有 Spring Boot Backend v1 五支 API：Controller → Service → JdbcTemplate Repository → Oracle。各 API 真實 smoke 狀態見下方。Frontend 預設真 API，Mock Demo 可手動開啟。沒有 RFID、AI、WebSocket、Redux 或 Admin framework。

## 目前可執行功能

Real mode：先啟動已設定密碼的 Backend，再於 frontend 執行 `npm run dev`。以下固定時間 Demo 流程需使用 `npm run dev:mock`。
Staff P001/S002 → WS1 code1 → WS2 完成 → Patient 1/5、20% → Admin 同步進度。
P001/S005 展示前置條件阻擋；P001 做完全部項目後 nextExam 為 null。
Mock P001 5 項、P002 3 項，初始均待完成，固定檢查時間 10:30。

## 驗證結果

- `npm test`：2 個測試檔、5 項測試通過。
- `npm run build`：TypeScript 檢查與 Vite production build 通過。
- 瀏覽器實際操作 WS1/WS2、Patient 20% 與下一站、Admin 一致資料。
- Admin 更新時間自動前進，確認 polling 運作。
- S999 顯示「找不到站點」，修正輸入後可繼續操作。
- 390px 手機 Staff 版面可操作；Admin 表格可水平捲動。
- `/admin` 直接載入與重新整理正常；檢查期間無 console error/warn。
- 修正品牌連結改用 SPA 導航，完成進度不再因點擊 Logo 重設。
- 修正官方 Progress 元件，螢幕閱讀器可讀取 aria-valuenow。
- 相機啟動/停止/卸載 cleanup 已程式碼審查；真實裝置掃碼、權限拒絕與硬體 stream 釋放仍需人工實機測試。
- 未配置 lint，因此沒有宣稱 lint 通過。

## 資料庫檔案完整性

初始 database 只有交接提示詞；根目錄的三份成果保留，新增副本至標準 database 路徑。原檔與副本 SHA-256 一致：

| 檔案 | SHA-256 |
| --- | --- |
| health_check_schema.sql | 00B0F3B53DD931F80748325F9C6A6EEFBA065CDF50AC8AA7F9A28A6414B63BF5 |
| health_check_seed.sql | 36F78CD3C1AE95CBBA9AA3359066B20880A80B7B3291002E25FD2654F708AC20 |
| health_check_erd.png | DC6CC25699DB6C1DA5BFCF4B3D3D4C743193792E5C4CBED907E2E9412E8AFC20 |

## 尚未完成

真實 WS1→WS2 成功端到端測試、其餘完整錯誤情境/穩定性測試、登入授權、跨分頁同步、正式 HTTPS 部署與真實相機驗收。

## 下一步

Final E2E 已完成可立即執行的驗收；狀態Partial。下一個合法時段補WS1→WS2成功、DB timestamps/進度更新/同碼409；另補實機QR。報告見docs/FINAL_E2E_TEST_REPORT.md。

## 已知問題與限制

Mock 記憶體資料重新整理重設，各分頁獨立；固定 Demo 時間與實際 completedAt 時間不同。
production preview 不啟用 Mock，API 未建置時畫面會顯示 API 錯誤。
相機需要 localhost 或 HTTPS；正式主機需 SPA fallback。
Prototype 無認證授權，不供真實病人資料使用。Mock code1 尚無到期機制。

## Backend Phase 1（2026-10-01）

本節保留 Phase 1 當時紀錄；後續 Phase 2 開始時已由既有真實服务驗證 P001/P999，見下方。

### 已完成

- Spring Boot 3.5.9、Maven Wrapper 3.3.4 / Maven 3.9.11、Spring Web/JDBC/Validation、Oracle ojdbc11、springdoc 2.8.14。
- GET /api/patients/{patientId}/exams；PATIENT / PATIENT_EXAM / EXAM_ITEM / CHECK_STATION bind parameter 查詢，按 EXAM_ID 排序。
- camelCase DTO、PENDING/COMPLETED enum、completedCount/totalCount、NULL 或 ISO LocalDateTime completedAt。
- 400/404/405/500 {message}；500 不回傳 SQL/連線資訊/stack trace。
- 環境變數與 secret ignore；禁用 SQL 初始化，沒有修改 Schema、seed、Frontend/MSW 或 contract。

### 驗證結果

- java -version：23.0.1；無 Java 21；mvn -version 不可用。
- mvnw.cmd -version：Maven 3.9.11 / Java 17.0.12（JAVA_HOME 指向 JDK 17）；編譯 release 17。
- Wrapper test / package：16 項測試、0 failures/errors/skipped；JAR 建立成功。測試涵蓋 Service、Controller、Repository mapping 與完整 Spring context。
- Oracle JDBC Driver class 正常載入；不代表已連上 Oracle。
- JAR 在本機 18080 啟動，以非真實密碼佔位值僅驗證 Swagger：UI 與 /v3/api-docs 實際 HTTP 200，OpenAPI 僅列出第一支 GET。臨時服務已停止。
- 未實作 /api/patients/P001/next 實際回 404；POST Exams 實際回 405，維持 {message}。
- 單元測試 P001 mock HTTP 200 / P999 mock HTTP 404；並非真實 DB 結果。
- 獨立程式碼審查發現 framework 404/405 被轉成 500，回歸測試先失敗後修正，完整 suite 通過。
- Schema/seed SHA-256 與上表一致。Workspace 沒有 Git repository，本次沒有 commit。

### 尚未完成

- 缺少 DB_PASSWORD，尚未驗證 HEALTH_CHECK 真實連線、P001 exams/count/status/time 與 P999 真實 404。
- WS1、WS2、Next Exam、Admin Progress；Frontend Integration。

### 下一步

依 backend/README.md 在本機 PowerShell 安全輸入 DB_PASSWORD，啟動後以 Swagger 或 Invoke-RestMethod 驗證 P001/P999。P001 原 seed 為 5 項，完成狀態以 DB 現況為準，不修改 DB。完成 smoke test 後進入 Backend Phase 2 - WS1。

## Backend Phase 2 - WS1（2026-10-01）

本節保留 Phase 2 當時紀錄；WS2 現已實作，最新狀態見下方 Phase 3。

### 已完成

- 新增 POST /api/ws1/check，沿用 Phase 1 與既有 Oracle Schema。
- 依序檢查患者、站點、安排、完成狀態、時段、前置條件；正常拒絕 HTTP 200 / allowed=false / code1=null。
- pending remainingExams 按 EXAM_ID 排序；允許時排除目前項目，回 examId/examName。
- UUID code1 於 @Transactional Service 寫入 CHECK_CONFIRMATION；CREATED_AT=SYSTIMESTAMP、USED_AT=NULL。不修改 PATIENT_EXAM。
- production Clock 為 Asia/Taipei；嚴格 HH:mm LocalTime，包含精確開始／結束邊界，保留秒／奈秒精度。
- 缺少患者前置安排仍拒絕；同 patient/station 多項檢查記錄 error 並 generic 500；validation/JSON 錯誤 400 {message}。

### 驗證結果

- Phase 1 baseline 16/16；既有 8080 真實 P001 HTTP 200 / totalCount 5 / completedCount 0；P999 HTTP 404 / {message:"找不到受檢者"}。這是既有 Phase 1 服務的 Oracle 讀取，非新版 WS1 smoke。
- mvnw.cmd test：59 tests；mvnw.cmd package：59 tests、JAR success；0 failures/errors/skipped。
- fixed Clock：10:30 allowed、17:30 closed、08:00/16:00 inclusive、邊界前後拒絕；已完成優先於時間／前置；缺前置與滿足前置；多項配置錯誤；重複 token；insert 失敗。
- Controller contract/400/404/500、Repository bind/mapping/NOT EXISTS、transaction proxy commit/rollback、台北 Clock、Phase 1 回歸、Swagger 測試通過。一般 tests 不需 DB_PASSWORD、不連 Oracle。
- 新版 JAR 短暫啟動於 18080；Swagger UI/OpenAPI 實際 HTTP 200，包含 Patient GET 與 WS1 POST。POST WS1 {} 實際 HTTP 400 / {message:"請提供受檢者與站點代碼"}。
- 此臨時服務使用非真實密碼佔位值，只測不涉及 DB 的請求；已停止，不代表 WS1 Oracle 成功。
- 獨立程式碼審查：無 actionable findings。
- Schema/seed SHA-256 保持 Phase 1 原值；Frontend/MSW 未改，API contract 只補已實作狀態。

### 真實 Oracle Smoke Test

2026-10-01 21:18:36 Asia/Taipei，確認 8080 已載入新版 WS1，直接呼叫真實 Oracle-backed API：

| Request | HTTP | Result |
| --- | --- | --- |
| P001 / S002 | 200 | allowed=false、不在允許檢查時間（08:00–16:00）、remaining E001–E005、code1=null |
| P001 / S005 | 200 | allowed=false、不在允許檢查時間（10:00–17:00）、remaining E001–E005、code1=null |
| P002 / S005 | 200 | allowed=false、受檢者未安排此站檢查、remaining E001–E003、code1=null |
| P999 / S002 | 404 | 找不到受檢者 |
| P001 / S999 | 404 | 找不到站點 |
| {} | 400 | 請提供受檢者與站點代碼 |

Oracle smoke test 成功驗證 DB query + business denial。之後 Patient API regression P001 totalCount=5 / completedCount=0。原始結果記於 ignored .tools/phase2-oracle-smoke.json。

今晚時段已關閉，沒有 allowed=true 或 confirmation INSERT；allowed path、INSERT binding 與 transaction commit/rollback 由 fixed Clock / mock tests 驗證，真實 INSERT 尚未另驗。沒有修改 DB 時段、Clock 或 PATIENT_EXAM。

人工測試時間若為本次晚間，P001/S002 應正常 business denial（不在 08:00–16:00）；以 DB 目前完成狀態為準。不可改時段或跳過時間檢查。allowed path 由 fixed Clock tests 驗證，真實 INSERT 可於開放時段另驗。

### 目前限制／尚未完成

- Prototype v1 不限制同 patient/exam 同時存在多個未使用 code1；尚無 token 到期、消耗。
- WS2、Next Exam、Admin Progress 與 Frontend Integration 尚未實作。

### 下一步

本階段真實 business denial / error smoke 已完成。開放時段可額外用 P001/S002 驗證 allowed=true，依 code1 查 CHECK_CONFIRMATION 正確 patient/exam / USED_AT=NULL。下一階段 Backend Phase 3 - WS2。


## Backend Phase 3 - WS2（2026-10-01）

### 已完成

- POST /api/ws2/complete，camelCase request/response 保持 Frontend contract。
- validation 400、invalid code 404、used code／completed exam 409；錯誤僅 {message}，500 不洩漏 DB details。
- @Transactional：confirmation exists/unused → exam PENDING → conditional claim USED_AT IS NULL → conditional complete STATUS='PENDING'。任一步失敗 rollback。
- claim/exam 都確認 affected=1；0 affected throw Runtime ConflictException。不同 token 競爭同 exam 時，敗方 claim 回滾，仍 unused。
- USED_AT／COMPLETED_AT 使用 Oracle SYSTIMESTAMP，STATUS 與 COMPLETED_AT 同 SQL 更新；不再查時段/前置/站點，不增加expiry。
- 共用 strict string parser、WS1/WS2 正確400訊息，Swagger新增ws2-controller，OpenAPI三個endpoint。
- 新增手動 Ws2OracleSmokeIT：default test/package excluded，明確執行才查真Oracle；可完成最新既有unused碼、查DB timestamps/status、Patient count+1與replay409。沒有造碼或reset；本次只編譯，未執行此手動IT。

### 測試

- Phase1/2 baseline：59/59。
- mvnw.cmd test、package：99 tests / 0 failures / 0 errors / 0 skipped；JAR success。
- Service：成功/老碼無expiry、invalid/used/completed、missing FK row、claim0/exam0 race、DB failures、錯誤 affected rows。
- Repository：bind parameters、NULL/TIMESTAMP mapping、USED_AT IS NULL、STATUS='PENDING' 與完整timestamp更新。
- Transaction component：成功後commit、exam race rollback claim、claim race rollback before409、第二update DB failure rollback before500。使用mock repository/transaction manager，沒有宣稱真DB concurrent test。
- Controller：200/400/404/409/500、typed JSON validation、context path、Phase1/2回歸；原有tests持續通過。
- 獨立程式碼審查：沒有 actionable findings。
- Schema/seed未改；Frontend/MSW保留，API_CONTRACT只補狀態與精確WS2行為。

### Oracle Smoke Test

2026-10-01 21:39:55 Asia/Taipei，使用者重啟新版8080後實際驗證：

| Request | HTTP | Response |
| --- | --- | --- |
| WS2 {code1:"INVALID-CODE-TEST"} | 404 | {message:"無效的確認碼，請重新判斷"} |
| WS2 {} | 400 | {message:"請提供 code1"} |
| WS1 {} | 400 | {message:"請提供受檢者與站點代碼"} |
| WS1 P001/S002 | 200 | 正常時段拒絕、code1=null、remaining5項 |
| Patient P001 exams | 200 | totalCount5、completedCount0 |

Swagger UI HTTP200，OpenAPI包含Patient GET、WS1 POST、WS2 POST。invalid-code Oracle path 已驗證，證明CHECK_CONFIRMATION查詢與404處理正常。
本次沒有取得可用的合法真實code1；agent沒有DB_PASSWORD，未直接查完整unused list、未執行手動Oracle IT。success Oracle path與duplicate code真實409待下一個允許檢查時段做WS1→WS2 E2E，或在原密碼session明確執行手動IT查合法既有碼。success/race/rollback由automated tests驗證，沒有偽造confirmation或自動重置結果。

### 尚未完成／下一階段

- 真實成功更新與replay409 E2E仍待合法code1。
- Next Exam、Admin Progress、Frontend real API integration、Authentication/Authorization/Token expiry未實作。
- 下一階段：Backend Phase 4 - Next Exam。成功E2E時查DB兩個時間欄位，保留完成狀態，不偷偷改回PENDING。


## Backend Phase 4 - Next Exam（2026-10-01）

### API 與規則

GET /api/patients/{patientId}/next，Response {nextExam: Exam|null, message}。沿用 PatientRepository，PENDING → 時段符合 → prerequisite 全部完成 → EXAM_ID ASC 第一項。沒有 pending 代表全部完成（含 0 項）；有 pending 但無 eligible 回另一個 200 message。推薦完整 Exam，completedAt 明確為 null。unknown patient404「找不到受檢者」。

WS1/Next 共用 common/exam/ExamEligibility、ExamPrerequisiteQueries 與 Production Asia/Taipei Clock。嚴格 HH:mm → LocalTime，開始與結束相等包含，保留秒／奈秒精度。NOT EXISTS completed patient assignment 可阻擋未安排 prerequisite。Next 只讀取、不建立確認碼、不完成檢查，真正進站仍走 WS1。

### 驗證

- Baseline 99 項；新增 22 項後 mvnw.cmd test / clean package 121/121，0 failures/errors/skipped、BUILD SUCCESS。
- Fixed Clock 覆蓋排序、completed、closed、start/end、邊界外一秒、blocked/satisfied prerequisites、全部完成、空安排、無 eligible 與 invalid DB time。
- Controller 覆蓋完整 JSON、明確 null、200/400/404/500；Repository 檢查 prerequisite SQL 綁定與 completed NOT EXISTS 條件；OpenAPI 四支 API。
- 既有 Phase 1/2/3 測試無回歸，獨立唯讀審查無 actionable findings。
- Schema/seed SHA-256 與原紀錄一致，Frontend/MSW 未修改。

### 真實 Oracle smoke

2026-10-01 22:05:45 +08:00，確認 8080 已載入 Next endpoint 後驗證：

| 請求 | HTTP | 結果 |
| --- | --- | --- |
| P001 next | 200 | nextExam=null；目前沒有可進行的檢查，請洽工作人員 |
| P999 next | 404 | 找不到受檢者 |
| P001 exams 回歸 | 200 | completedCount=0、totalCount=5，全部 PENDING |
| Swagger UI / OpenAPI | 200 | 四支 API；Next 200 三種結果與404文件正確 |

此時已超過所有 P001 pending 的時段，null 是正確結果。未修改 DB 時段、系統時間或 Clock；沒有 confirmation 或狀態寫入。推薦成功、全部完成及 prerequisite path 由 fixed Clock/mock tests 驗證，未宣稱真實 Oracle 有推薦成功。完整 HTTP 證據保存 ignored .tools/phase4-oracle-smoke.json。

### 剩餘／下一階段

Backend Admin Progress、Frontend real API integration 尚未實作；Frontend 保持 MSW。下一階段 Backend Phase 5 - Admin Progress。合法 WS1→WS2 真實成功/replay409 E2E 仍待開放時段，Phase 4 不造碼、不重置資料。


## Backend Phase 5 - Admin Progress（2026-10-02）

GET /api/admin/progress 已實作。Controller → 薄 Service → JdbcTemplate Repository，一次 PATIENT LEFT JOIN PATIENT_EXAM aggregation；COUNT(PE.EXAM_ID) 為 totalCount，NVL(SUM(CASE WHEN PE.STATUS='COMPLETED' THEN 1 ELSE 0 END),0) 為 completedCount。GROUP BY patientId/name、ORDER BY PATIENT_ID ASC，沒有 N+1 或資料寫入。

Response 僅 patientId、patientName、completedCount、totalCount。沒有安排的病人仍回 0/0；無病人回 HTTP200 []；DB/unexpected failure 沿用 GlobalExceptionHandler HTTP500 {message:"服務暫時無法使用"}（以 workspace 既有訊息為準）。Swagger 新增 admin-controller，五支 API 與 array schema 已驗證。

Baseline 121/121；新增 7 項 Controller/Service/Repository tests，合計 128/128，0 failures/errors/skipped；mvnw.cmd test 與 clean package BUILD SUCCESS。Repository tests 使用 mocked ResultSet 與 SQL shape review，未宣稱在 Oracle 執行 SQL。獨立唯讀審查無 actionable findings。

Schema/seed SHA-256 與原值一致；Frontend/MSW/Vite proxy/fetch 未修改。Backend v1：5 / 5 main APIs implemented。

Oracle Smoke Test：初始 8080 沒有服務，agent 環境無 DB_PASSWORD。待使用者於原密碼 session 啟動新版後，只讀 GET Admin/Patient 核對 P001/P002 當下完成數與總數。尚未取得 HTTP200 或真實 DB 計數證據，不宣稱 smoke 完成。

尚未完成 Frontend real API integration 與 Full end-to-end testing。下一階段 Integration Phase - Frontend + Backend + Oracle；本次不自動開始。


## Integration Phase（2026-10-02 Asia/Taipei）

已實作：預設 Real API mode，VITE_USE_MSW=true 且 development 才啟動 Mock；npm run dev:mock 保留獨立 Demo。集中 VITE_API_BASE_URL（Backend origin，預設空）、五支 API 同一 /api prefix；Vite development proxy /api → localhost:8080，port5173/strictPort。Real mode 自動解除本專案 mockServiceWorker.js 舊註冊，保留其他 worker。

原 API contract、Query keys、Admin 5000ms polling、WS2 success invalidation（patients prefix 包含 exams/next，另 admin）與 QR scanner 核心保留。完成按鈕改為 allowed=true 且 code1 存在才顯示；Mock unknown patient 訊息同步 Backend。真模式不顯示固定10:30模擬資料提示。DB secrets 沒有進入 Frontend。

Automated regression：Frontend 17 tests/4 files passed，TypeScript + production build 成功；無 lint script，因此未宣稱 lint。Backend test / clean package 128 tests passed，0 failures/errors/skipped、BUILD SUCCESS。Schema/seed/ERD SHA-256 與原值一致，Backend 程式/response contract未變。

Browser 已驗：
- 預設Real mode（5173）：沒有Demo標示，Backend未啟動時顯示可重試API錯誤，頁面未crash；觀測console無error/warn。
- 明確Mock mode（臨時5174）：P001/S002允許、MOCK-code、WS2成功後進度1/5，Demo提示存在。
- 同origin由Mock切Real：Demo與mock進度消失、完成按鈕不可用，Vite終端記錄真proxy請求遭8080 ECONNREFUSED。這證明切換後未持續由MSW回應；未宣稱Oracle成功。

初始待驗項目於使用者啟動新版後已完成，結果見下方「Integration 真實 Oracle 驗收」。不擷取密碼、不假造DB資料。

WS1→WS2 success E2E 尚未完成，晚間不可改時鐘/時段或fake code1；需在合法時段驗證。Integration Phase已完成；WS1→WS2 success E2E與其他穩定性情境留待Final E2E & Stabilization，本次未開始美化或新功能。


### Integration 真實 Oracle 驗收（2026-10-02 21:21–21:24 +08:00）

使用者於21:20:31啟動新版8080後完成；此前無法連線的紀錄保留為初始狀態，不代表目前失敗。

| API / UI | 結果 |
| --- | --- |
| Patient P001 | Backend與Vite proxy HTTP200；瀏覽器姓名、0/5、5項pending正確 |
| Patient P002 | HTTP200，0/3，與Admin計數核對一致 |
| Patient P999 | HTTP404；瀏覽器顯示「找不到受檢者」，頁面可操作 |
| Next P001 | HTTP200，nextExam=null；瀏覽器顯示「目前沒有可進行的檢查，請洽工作人員」 |
| Admin | HTTP200；瀏覽器P001 0/5、P002 0/3，總2人/進行中2人；數字與Oracle-backed Patient exams行數/COMPLETED狀態一致 |
| WS1 P001/S002 | HTTP200，allowed=false、code1=null；瀏覽器正常「不在允許檢查時間（08:00–16:00）」、無完成按鈕 |
| WS2 wiring | Backend及Vite proxy POST invalid code都回404「無效的確認碼，請重新判斷」；不是合法完成成功測試 |

瀏覽器臨時fetch trace顯示Admin HTTP200時間21:23:32.761、37.792、42.818，每次間隔5.031/5.026秒。真模式未啟動MSW，先前同origin Mock→Real清理已驗；五支proxy API均直接對照8080相同結果。沒有宣稱取得DevTools from-service-worker欄位（瀏覽器工具不提供該欄位）；以模式/清理、browser fetch trace及Vite proxy對照驗證來源。trace診斷程式已移除，console觀測無error/warn。

證據：ignored .tools/integration-oracle-http.json、integration-proxy-http.json、integration-browser-trace.txt。Admin Phase5 Oracle smoke也一併完成。Frontend 17 tests/build與Backend128 tests/clean package結果保留。未改DB、Clock、schema/seed/ERD；未造code1。

Integration Phase完成。WS1→WS2真成功、Patient/Admin成功後更新與真實replay409仍待合法檢查時段，沒有將Mock成功算成Oracle E2E。下一階段Final E2E & Stabilization，本次停在Integration。


## Final E2E & Stabilization（2026-10-02）

- Core Development: Complete
- Integration: Complete
- Final E2E: Partial — Success E2E pending legal time
- Documentation: In progress

Final報告：docs/FINAL_E2E_TEST_REPORT.md。前端baseline/最終17 tests及build成功；後端test/clean package128 tests、0 failures/errors/skipped、BUILD SUCCESS。真APIPatient/Next/Admin、Staff未知患者/站點/未安排/closed denial、WS2 invalid404通過。Admin多次約5秒polling及離頁停止已驗；三頁dev/build-preview直接route+refresh、1366px桌面/390px手機基本版面通過。

QR僅啟動/停止UI控制與video移除驗證，瀏覽器未取得track且有安全來源warning；實際串流/硬體cleanup/實掃仍待實機，不勾成完成。P001/S005前置case被晚間時段優先擋下，固定時間automated evidence保留。

真實WS1→WS2成功、CHECK_CONFIRMATION欄位對照、PATIENT_EXAM更新、Patient/Next/Admin更新及same-code409全部保留待合法時段；未改Clock/DB時間、造碼或reset。未發現產品阻斷性Bug；修正README中preview proxy說明為目前Vite實際繼承行為。臨時trace移除，schema/seed/ERD hash與原值一致。停在本階段，不新增功能。
