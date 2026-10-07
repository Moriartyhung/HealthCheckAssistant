# Backend Phase 3 - WS2 Implementation Plan

Goal: POST /api/ws2/complete，安全消耗 Oracle code1 並完成對應 PATIENT_EXAM。
Spec: C:/Users/USER/Downloads/HealthCheckAssistant_Backend_Phase3_WS2_Codex_Prompt.md；docs/API_CONTRACT.md。
Design: WS2 Controller/DTO → @Transactional Service → JdbcTemplate Repository。Optional Confirmation / PatientExamCompletionData，conditional token claim + conditional exam completion，ConflictException extends RuntimeException，500 隱藏 DB details。

- [x] Phase 1/2 baseline；read existing files/contract/schema。
- [x] WS2 Service、Controller、Repository tests 先失敗，涵蓋 ordered guards、two conditional SQL、0 affected rows、error contracts。
- [x] 實作 WS2；共享 strict string parser；error handler 區分 WS1/WS2 400，新增409。
- [x] Transaction component tests 驗證 commit 與競爭/DB error rollback；OpenAPI 更新成3支；test/package；獨立審查。
- [x] 可完成的真實 Oracle smoke；更新 STATUS、README、LOG、CONTRACT。

Review focus: 不先消耗已完成 exam 的code；used token 不更新exam；兩token同exam loser rollback自己USED_AT；錯誤不被catch後commit；missing FK row generic500；typed JSON/blank/length100限制；WS1 validation message不變。時間使用SYSTIMESTAMP，無Clock/expiration/WS1規則重查。

Scope: 不變Frontend/MSW、Schema/seed、Next/Admin、Security；不造fake confirmation、不重置DB成功結果。無Git repository，指定workspace直接實作。
Current agent沒有DB_PASSWORD；現有使用者Oracle-backed8080服務需要重啟載入WS2。不得取出process密碼。可加手動執行Oracle smoke test（*IT不在一般test中），僅使用本機env憑證與合法現有token；若無token只驗invalid-code，success E2E待開放時段。

驗證：99 automated tests / package成功；真實8080 Oracle invalid-code404 / body400，Swagger三支；success/replay409E2E待合法碼。manualOracleIT編譯成功但未執行，不宣稱完成。

