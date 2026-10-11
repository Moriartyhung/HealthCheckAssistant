# Frontend API Contract v1

所有 endpoint 前綴 `/api`，JSON request/response。UI 呼叫正常 fetch，預設真 Backend；MSW 僅 development 且 VITE_USE_MSW=true 時攔截。Types 位於 frontend/src/types。

| 方法 | Endpoint | Request | Response |
| --- | --- | --- | --- |
| POST | /api/ws1/check | `{patientId, stationId}` | `{allowed, message, examId?, examName?, remainingExams: [{examId, examName}], code1}` |
| POST | /api/ws2/complete | `{code1}` | `{success, message}` |
| GET | /api/patients/:patientId/exams | 無 | `{patientId, patientName, completedCount, totalCount, exams: Exam[]}` |
| GET | /api/patients/:patientId/next | 無 | `{nextExam: Exam 或 null, message}` |
| GET | /api/admin/progress | 無 | `[{patientId, patientName, completedCount, totalCount}]` |
| POST | /api/admin/patients | `{patientId, patientName, examIds?: string[]}` | 201 PatientDetail，Location 指向單筆資源 |
| GET | /api/admin/patients | 無 | 200 `[{patientId, patientName}]`，依病歷號排序 |
| GET | /api/admin/patients/:patientId | 無 | 200 PatientDetail |
| PUT | /api/admin/patients/:patientId | `{patientName, examIds?: string[]}` | 200 PatientDetail |
| DELETE | /api/admin/patients/:patientId | 無 | 200 `{message:"受檢者已刪除"}` |
| GET | /api/admin/exams | 無 | 200 ExamCatalogItem[] |

## PATIENT 資料管理（V1）

新增與修改可同時寫入 PATIENT 基本資料及 PATIENT_EXAM 安排，其他資料表只讀取。病歷號建立後不可修改；PUT 若帶 patientId 或其他未知欄位即回 400。不接受 status、completedAt 等完成欄位。

PatientDetail：`{patientId, patientName, exams: ManagedPatientExam[]}`。

ManagedPatientExam：`{examId, examName, stationId, stationName, startTime, endTime, status, completedAt, removable, removalBlockReason}`。完成時間為 ISO 8601 或 null；removalBlockReason 為不可移除原因或 null。

ExamCatalogItem：`{examId, examName, stationId, stationName, startTime, endTime, requiredExamIds: string[]}`，從 EXAM_ITEM、CHECK_STATION、EXAM_PREREQUISITE 查詢。

examIds 是完整目標安排。新增省略或 null 表示沒有安排；更新省略或 null 保留既有安排（相容舊姓名編輯 API）；更新 `[]` 表示要求移除全部安排，仍須通過保護規則。項目代碼必須為非空字串，不可重複、不移除前後空白。無效或重複項目回 400。

所有前置項目都必須在目標安排中（已完成項目也保留在集合中），前置鏈不可循環。由於現有 WS1 以站點定位單一檢查，同一受檢者同站點只允許一項安排，包含已完成項目；EXAM_ITEM 清單仍可包含同站點多個不同選項。缺少前置條件、循環、同站多項衝突回 409，不修改 WS1 的既有行為。

新增以同一 @Transactional 交易寫入病人與所有 PENDING、COMPLETED_AT=NULL 的安排；修改以同一交易更新姓名與增減安排。修改先鎖定 PATIENT，再鎖定既有 PATIENT_EXAM，重新讀取狀態，僅移除 PENDING 且沒有確認碼的安排。已完成、有效 WS1 確認碼（USED_AT IS NULL）或歷史確認碼關聯皆禁止移除，回 409；確認碼 schema 沒有到期欄位。保留歷史確認碼是既有外鍵及不刪除歷史資料的要求。外鍵衝突回 409，資料庫交易失敗回 500 並回滾，不更改保留項目的狀態或完成時間。

新增需提供字串病歷號與姓名；更新需提供字串姓名。不可空白或含控制字元，新增欄位及更新姓名會移除前後空白。病歷號不可含 `/`、`\`、`;`，也不可為 `.` 或 `..`，以避免路由歧義。欄位限制依現有 Oracle DDL：病歷號 20 BYTE、姓名 100 BYTE；後端用 Oracle LENGTHB 依實際資料庫字元集檢查，不以中文字數代替 byte 數。

新增先查重，並處理並行 INSERT 的唯一鍵衝突：回 409 `{message:"病歷號已存在"}`。不存在的單筆查詢、更新或刪除回 404。非法資料回 400；資料庫失敗回 500，沿用 `{message}`，不洩露 SQL。

刪除使用單一 DELETE，NOT EXISTS 同時檢查 PATIENT_EXAM 與 CHECK_CONFIRMATION；任何狀態的關聯紀錄皆阻擋刪除。Oracle 外鍵另保護並行新增關聯造成的衝突，回 409 `{message:"受檢者已有關聯檢查紀錄或確認碼，無法刪除"}`。不使用 CASCADE，不刪除關聯資料。

Web 操作入口：`/admin` 進度總覽下方的「受檢者資料管理」。上方表單新增／編輯姓名與多選健檢安排，顯示站點、時段、前置條件及既有狀態；不可移除項目鎖定。同站點其他選項停用，缺少前置項目時不能儲存。下方提供單筆查詢／顯示全部，每列「查看安排」、「編輯」、「刪除」，安排明細顯示狀態、完成時間與移除限制，刪除須確認。清單 API 載入失敗時停用儲存。變更後重新查詢列表並使進度／病患查詢快取失效。CRUD 使用現有 fetch → Spring Boot → Oracle；MSW 模式停用此區塊，API adapter 也拒絕呼叫，不新增管理 Mock handler。

原五支健檢 API 的路徑與流程不變，Android App 不變。未加入認證、分頁或 V2 排程。

### CRUD 驗證方式

`backend`：`./mvnw.cmd test` 執行一般測試；CRUD API 測試執行真 Controller、Service、Repository，以 JdbcTemplate double 隔離外部 Oracle，並不代表 Oracle 已實測。`frontend`：`npm test` 與 `npm run build`；介面測試只在測試檔替代 fetch 回應，正式程式沒有替代資料。

需在已安全設定 DB_PASSWORD 的 session 中，於 backend 執行 `./mvnw.cmd '-Dtest=PatientManagementOracleIT,PatientArrangementOracleIT' test` 才是 Oracle opt-in 整合驗證。基本 CRUD 的三個案例使用唯一病人並回滾，關聯拒刪案例只讀既有資料。安排的兩個案例使用既有真實項目清單：新增／查詢／姓名及安排修改在測試交易回滾；故障案例透過真 HTTP 交易注入第二項新增失敗，直接查 Oracle 確認沒有部分資料，最後只清理自身唯一測試病歷號。它不更動既有受檢者、完成狀態或參考表。若清單沒有足夠不同站點且符合前置條件的項目，會明確跳過。預設一般測試不執行 *IT；沒有 DB_PASSWORD 時五個 Oracle 測試跳過，不能算驗證通過。

本階段依 repository 的 Oracle DDL 檢查主鍵、外鍵及約束；尚未連線比對正式資料庫 schema，也未驗證真實鎖定與並行 WS1／WS2 情境。一般安排 API 測試執行完整分層，以 JDBC 及交易管理器 doubles 測試失敗回滾邊界；不冒充 Oracle 實測。

Swagger `/v3/api-docs` 由 Controller 註解生成。若需更新根目錄 api-docs.json，可於 backend 執行 `./mvnw.cmd -Dtest=ApplicationSmokeTest -Dopenapi.snapshot=../api-docs.json test`；此命令只產生契約，不驗證 Oracle。

Exam: `{examId, examName, stationId, stationName, startTime, endTime, status, completedAt}`。
status 僅 `PENDING`、`COMPLETED`。時間為 HH:mm，completedAt 為 ISO 8601 或 null。

WS1 正常業務拒絕仍為 HTTP 200，`allowed:false`、`code1:null`。檢查時段包含開始與結束邊界；前置項目未完成、已完成項目或未安排的站點皆拒絕。允許時產生一次性 code1，remainingExams 排除本次允許項目。

WS2 消耗 code1，更新狀態與 completedAt；無效 code1 回 404，已消耗或該項目已完成回 409。正式 Backend 必須用交易原子更新確認碼與檢查狀態。Prototype 未實作 token 到期。

下一站按 examId 升冪順序挑選尚未完成、在時段內且滿足前置條件的項目。全部完成與目前無可檢查項目都回 nextExam:null，以 message 區分。

錯誤回應 `{message: string}`：400 缺少或錯誤輸入、404 未知識別碼、409 重複確認。未實作認證、授權、分頁。Frontend Mock 使用固定時間；Backend WS1 / Next 使用相同真實台北時間。

Integration 已設定 development Vite proxy：/api → localhost:8080。預設關閉 MSW；npm run dev:mock 可明確開啟 Demo。Production 不載入 Mock，部署需配置 /api 反向代理。以下各 Backend Phase 段落保留當時紀錄；最新整合驗收見 PROJECT_STATUS。

## Backend 已實作狀態（2026-10-02）

- Phase 1：GET /api/patients/:patientId/exams。
- Phase 2：POST /api/ws1/check。Production 使用 Asia/Taipei Clock，包含開始／結束相等邊界；業務拒絕仍 HTTP 200。
- WS1 拒絕時保留全部 pending remainingExams，code1 明確為 null；成功時排除本次 exam、回 examId/examName，UUID 寫入 Oracle CHECK_CONFIRMATION。
- WS1 不更新 PATIENT_EXAM；同 patient/exam 可有多個未使用確認碼，尚無到期機制。Admin 已於 Phase 5 實作；Next 已於 Phase 4 實作；WS2 狀態見下方 Phase 3；此處表格保留 Frontend 完整 contract。
- WS1 400：缺欄位、空白、過長、非字串代碼或不合法 JSON → {message:"請提供受檢者與站點代碼"}；未知患者／站點 404。意外同站多項檢查為 generic 500，不選第一筆。
- 既有 Mock 固定時間 10:30，Frontend development 繼續啟用 MSW；本階段用 Swagger/PowerShell 驗證 Backend。


## Backend Phase 3 - WS2 已實作

POST /api/ws2/complete 保持原 request/response contract。成功：200 {success:true,message:"檢查完成狀態已更新"}；body 錯誤：400 {message:"請提供 code1"}；invalid：404 {message:"無效的確認碼，請重新判斷"}；used：409 {message:"確認碼已使用，請勿重複提交"}；exam completed：409 {message:"此檢查已完成"}。

同一 transaction 先 conditional claim USED_AT IS NULL，再 conditional complete STATUS='PENDING'。任一 update 失敗都回滾；未用的其他 code1 指向已完成 exam 時仍保持 unused。WS2 不重新檢查 WS1 時段/前置，不加入 token 到期。Schema 保持原樣，Frontend MSW 仍啟用。

Next Exam 已於 Phase 4 實作，Admin Progress 已於 Phase 5 實作。此文件的完整 Frontend contract 不變。



## Backend Phase 4 - Next Exam 已實作

GET /api/patients/{patientId}/next 純讀取，Response 嚴格保持 {nextExam: Exam|null, message: string}。

- 200 recommended：完整 pending Exam，completedAt 明確為 null；message="建議前往以下站點"。
- 200 all completed（沒有 pending，包含 0 項）：nextExam=null；message="所有檢查已完成，謝謝您的配合！"。
- 200 no currently eligible exam：nextExam=null；message="目前沒有可進行的檢查，請洽工作人員"。
- 404 unknown patient：{message:"找不到受檢者"}。
- 400 path 參數空白或超過 20 字元：{message:"請求參數不正確"}；500：{message:"服務暫時無法使用"}。

規則：PENDING → START_TIME <= now <= END_TIME → 所有 prerequisite 的患者安排已 COMPLETED → EXAM_ID ASC 第一筆。缺少 prerequisite 安排也視為未完成。與 WS1 共用 eligibility/NOT EXISTS 查詢及 Asia/Taipei Clock；保留秒／奈秒精度，HH:mm 結束邊界後一秒已不可進行。不依固定 seed 或外部 count 推算。

Next 不產生 UUID、不寫 CHECK_CONFIRMATION、不完成檢查；實際进站仍需 WS1。Frontend development 維持 MSW，整體串接留待 Integration Phase。


## Backend Phase 5 - Admin Progress 已實作

Backend v1：5 / 5 main APIs implemented。GET /api/admin/progress 無 Request Body。200 application/json：[{patientId:string,patientName:string,completedCount:integer,totalCount:integer}]，只回四個欄位；沒有病人為200 []。

PATIENT LEFT JOIN PATIENT_EXAM，一次聚合每位患者的 totalCount=COUNT(PE.EXAM_ID)、completedCount=NVL(SUM(CASE WHEN PE.STATUS='COMPLETED' THEN 1 ELSE 0 END),0)。按 PATIENT_ID ASC；有 patient 但無安排仍列出0/0。不按時間/日期篩選，不產生確認碼或修改資料。不存在 unknown-patient404 業務分支。

DB/unexpected failure：500 {message:"服務暫時無法使用"}，沿用既有 GlobalExceptionHandler，不曝露 Oracle/SQL/stack trace。128 tests及clean package成功；真實Oracle smoke狀態見PROJECT_STATUS。Frontend 自行算百分比與狀態，每5秒polling保留；本次沒有切換真API。

