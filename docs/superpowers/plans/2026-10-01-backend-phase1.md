# Backend Phase 1 Implementation Plan

Goal: 僅建立 GET /api/patients/{patientId}/exams，Oracle HEALTH_CHECK 真實讀取。
Spec: 使用者提供的 Backend Phase1 Codex Prompt 與 docs/API_CONTRACT.md。
Architecture: Controller → Service → JdbcTemplate Repository → Oracle；record DTO、enum、{message} error。

決策：java 命令 JDK 23（無 21），Wrapper 依 JAVA_HOME 使用 JDK 17，編譯 release 17；Spring Boot 3.5.9、springdoc 2.8.14、Maven 3.9.11。無 Git repository，直接在指定 workspace 實作。依「直接開始實作」執行已提供規格，不重複要求確認。無 DB_PASSWORD，不搜尋憑證。TIMESTAMP 無時區，回 ISO LocalDateTime。

- [x] 建立 pom / Maven Wrapper 與先失敗的 Service、Controller 測試。
- [x] DTO、Repository bind parameter / examId 排序 / timestamp null mapping。
- [x] Service 計數與 404；Controller contract、400、500 不洩漏 DB details。
- [x] 完整 test/package；啟動驗證 Oracle driver、Swagger UI、OpenAPI。
- [x] 密碼已設定才執行真實 P001/P999 smoke test；更新 README、STATUS、LOG。

Review focus: 混合狀態計數、空列表、未知 patient、不限制 seed pattern、空白與過長 ID、timestamp 與 null、500 隱藏資訊。保留 Frontend/MSW/SQL，禁止其他 Phase endpoints、JPA、Security、migration。

驗證結論：16 tests / package 通過；Swagger HTTP 200。真實 Oracle smoke test 因缺少 DB_PASSWORD 留待本機設定，未宣稱完成。

