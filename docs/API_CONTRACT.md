# Frontend API Contract v1

所有 endpoint 前綴 `/api`，JSON request/response。UI 呼叫正常 fetch，預設真 Backend；MSW 僅 development 且 VITE_USE_MSW=true 時攔截。Types 位於 frontend/src/types。

| 方法 | Endpoint | Request | Response |
| --- | --- | --- | --- |
| POST | /api/ws1/check | `{patientId, stationId}` | `{allowed, message, examId?, examName?, remainingExams: [{examId, examName}], code1}` |
| POST | /api/ws2/complete | `{code1}` | `{success, message}` |
| GET | /api/patients/:patientId/exams | 無 | `{patientId, patientName, completedCount, totalCount, exams: Exam[]}` |
| GET | /api/patients/:patientId/next | 無 | `{nextExam: Exam 或 null, message}` |
| GET | /api/admin/progress | 無 | `[{patientId, patientName, completedCount, totalCount}]` |

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

