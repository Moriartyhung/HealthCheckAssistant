# Backend Phase 2 - WS1 Implementation Plan

Goal: 僅新增 POST /api/ws1/check，沿用 Phase 1。
Spec: C:/Users/USER/Downloads/HealthCheckAssistant_Backend_Phase2_WS1_Codex_Prompt.md；docs/API_CONTRACT.md。
Design: Ws1Controller → @Transactional Ws1Service → Ws1Repository / JdbcTemplate。DTO records、注入 Asia/Taipei Clock；UUID code1 寫 Oracle CHECK_CONFIRMATION。原 Patient API、Frontend/MSW、Schema/seed 不變。

- [x] Phase 1 baseline tests；讀取既有 schema/types/mocks。
- [x] 先寫 Service fixed-clock / Controller / Repository SQL binding tests，確認失敗。
- [x] 實作 DTO / Clock / WS1 Repository、依指定順序判斷與 transaction / INSERT；invalid JSON / Bean Validation 400 message。
- [x] 更新完整 context Swagger 測試為 2 endpoints；test/package，Phase 1 回歸與獨立審查。
- [x] 真實 Oracle smoke（只有本機可用的使用者啟動新版服務時）；文件記錄結果與限制。

Review focus: missing prerequisite patient row 必須拒絕（NOT EXISTS completed）；同站多筆不自行挑選、generic 500；拒絕全部 pending / 成功排除本項並不更新 status；inclusive LocalTime 邊界；缺欄位、空白、JSON 型別錯誤 400；INSERT 失敗不回 allowed；重複呼叫產生新 token。

目前本 agent 無 DB_PASSWORD；既有 localhost:8080 Phase 1 實際 P001 HTTP 200 / 5 / completed 0。不得取出其他 process 的環境密碼，不重啟使用者服務。無 Git repository，直接指定 workspace 實作。依使用者要求直接執行既定規格。

結果：59 tests / package 成功；HTTP Swagger 200 / invalid body 400。WS1 Oracle smoke 仍待使用者重啟原本持有 DB_PASSWORD 的 session，新版未宣稱真實 DB 成功。


最終結果：8080 已載入新版；21:18:36 真實 Oracle business denial / 404 / 400 smoke 成功，Phase 1 P001 5/0。當晚沒有 allowed=true，真實 INSERT 未產生；fixed Clock allowed / insert / rollback 測試通過。

