# Frontend Prototype v1 執行紀錄

依 database/HealthCheckAssistant_Codex_Prompt.md 與使用者確認的十二項範圍實作。

1. 建立 Vite / React / TypeScript、shadcn/ui 與 API types。
2. 以測試驗證 MSW WS1 / WS2、前置條件、時間與一次性 code1。
3. 完成 Staff、Patient、Admin 與 QR camera lifecycle。
4. 驗證測試、build、瀏覽器操作，更新文件。

Ruling: database 原本只有交接提示詞，三個成果位於根目錄；新增逐位元相同副本到指定路徑，保留原檔。成本：重複檔案，日後整理需另行授權。
Ruling: 本機沒有既存獨立實作計畫或 Git repository；以交接規格與使用者十二項確認為執行計畫，直接於指定 workspace 實作。成本：尚無 Git 版本歷史。
Ruling: Mock 固定檢查時間 10:30，使用 seed 定義的時段；頁面標示 Demo 時間。成本：正式 API 需改用伺服器時間。

執行結果：官方 shadcn CLI 產生六個 UI 元件、MSW CLI 產生 worker；API contract 四項測試先失敗後通過。
新版套件相容修正：TypeScript 7 移除 baseUrl、MSW 3 使用 onUnhandledFrame；以安裝套件型別為依據。
最終獨立程式碼審查：發現品牌原生連結會重設 Mock，瀏覽器重現後改成 Link，確認進度保留；沒有其他重要問題。
進度條缺少 aria-valuenow 的測試先失敗，補傳 value 到 Radix Root 後通過。
最終 `npm test` 5/5 通過，`npm run build` 成功。真實相機仍需實機測試，其餘瀏覽器驗證見 PROJECT_STATUS。
Deferred minors: 無審查發現待處理的小問題。

# Backend Phase 1 執行紀錄（2026-10-01 Asia/Taipei）

依使用者指定的 HealthCheckAssistant_Backend_Phase1_Codex_Prompt.md 直接執行；讀取狀態、contract、DDL/seed 與 frontend types/api。範圍僅 Patient Exams GET。

1. java 命令 23.0.1；mvn 不存在；無 JDK 21。Wrapper 以 JAVA_HOME 的 Java 17.0.12 執行，pom release 17。官方 Maven 3.9.11、Wrapper 3.3.4、Spring Boot 3.5.9 / springdoc 2.8.14。
2. Service、MVC contract、Repository mapping 測試先因缺少實作而失敗，再建立 DTO/enum、JdbcTemplate Repository、Service、Controller 與 error advice，11 tests 通過。
3. 新增完整 context Oracle driver、Swagger UI/OpenAPI、unknown route / 不支援方法測試。404/405 測試先失敗，確認 broad handler 造成 500，修正保留框架 4xx status / headers 與安全 message。
4. 獨立審查亦指出同一問題，沒有其他 actionable findings。修正後 package 16/16，0 failures/errors/skipped，生成 executable JAR。
5. Java 17 啟動 JAR 於 18080，以非真實密碼佔位值僅驗證 Swagger UI/OpenAPI HTTP 200，只有一個 API path；Next 404、POST Exams 405。未呼叫 DB endpoint，臨時服務已停止。
6. 更新 README、STATUS、LOG、gitignore；Schema/seed hash 保持原值，Frontend/MSW/contract 未修改。

Ruling: 無 Git repository，直接 workspace 實作；成本：没有 Git 版本歷史。
Ruling: 無 Java 21，使用 Maven 既有 JAVA_HOME Java 17 / release 17，不改全域設定；成本：未另驗證 JDK 21。
Ruling: Oracle TIMESTAMP 無時區，回傳 LocalDateTime ISO 8601，不添加 Z；成本：跨時區需求需另定 contract。

未設定 DB_PASSWORD；不搜尋憑證。HEALTH_CHECK connection、真實 P001/P999 待本機設定密碼後人工 smoke test，不宣稱成功。下一階段 Backend Phase 2 - WS1。
相容版本來源：https://springdoc.org/v2/ 。Logs 在 ignored .tools，reports 在 backend/target/surefire-reports。

# Backend Phase 2 - WS1 執行紀錄（2026-10-01 Asia/Taipei）

依使用者提供 Phase 2 規格直接延伸 Phase 1，不重新初始化。讀取狀態、README、contract、實作紀錄、全部 main、pom、Oracle DDL/seed、Frontend types/handlers/store。

- Phase 1 baseline 16/16；舊 localhost:8080 真實 P001 HTTP 200 / totalCount 5 / completedCount 0。
- WS1 Service/Controller/Repository tests 先因缺少實作失敗，再新增 ws1 package / record DTO、Clock configuration、transactional Service、JDBC Repository。
- 判斷依指定順序；NOT EXISTS completed row 涵蓋缺少前置安排；同站多筆記錄 error 並回 generic 500。拒絕不 INSERT；允許 UUID INSERT，保留 STATUS/COMPLETED_AT。
- request 的局部嚴格字串反序列化拒絕 JSON 數值／布林轉字串，不修改全域 Jackson coercion。共用 handler 補 body 400；保留既有 path validation 與 routing error behavior。
- 第一批 WS1 + Patient 回歸 51/51；完整 context / transaction / Clock 後 test 與 package 59/59，0 failures/errors/skipped，產生 executable JAR。
- 獨立 reviewer read-only 審查：沒有 actionable findings；未自行執行 Oracle。
- Schema/seed SHA-256 與 Phase 1 一致；Frontend/MSW 保持，contract 只補已實作狀態。

Ruling: 生產 Clock 保留秒／奈秒，以 LocalTime 做指定的 inclusive 比較，不截斷到分鐘。成本：16:00:01 已晚於 16:00 邊界。
Ruling: 初始 agent 無 DB_PASSWORD，8080 舊程式只有 Phase 1。要求使用者在原密碼 session 重啟新版；不擷取 process 環境或密碼。Phase 2 真實 Oracle 結果另記於 PROJECT_STATUS，不偽造成功。

Prototype v1 允許重複 WS1 產生多個未使用 code1；未實作到期/消耗。下一階段 Backend Phase 3 - WS2。

Phase 2 補充驗證：新版 JAR 18080 Swagger UI/OpenAPI HTTP 200，兩個 API paths；POST WS1 {} HTTP 400 exact message。以非真實密碼佔位值測不需 DB 的請求，服務已停止。既有 Phase 1 真實 P999 HTTP 404 / 找不到受檢者。新版 WS1 真實 DB 仍待原 session 重啟。

Phase 2 真實 Oracle smoke 更新：2026-10-01 21:18:36 Asia/Taipei，8080 已載入新版。P001/S002 → HTTP 200 / allowed=false / 不在允許檢查時間（08:00–16:00） / remaining E001–E005 / code1=null。P001/S005 時段拒絕；P002/S005 未安排拒絕；P999/S002 404、P001/S999 404、{} 400，訊息正確。Patient GET 再驗 P001 5 項、completed 0。

Oracle smoke test 成功驗證 business denial；allowed path 由 fixed Clock unit tests 驗證。今晚沒有產生 confirmation，真實 INSERT 可於開放時段額外驗證。未改 DB/系統時間/Clock，不擷取密碼、不停止使用者服務。完整結果見 PROJECT_STATUS 與 ignored .tools/phase2-oracle-smoke.json。

# Backend Phase 3 - WS2 執行紀錄（2026-10-01 Asia/Taipei）

依指定 Phase3 prompt 延伸現有 Backend；讀取 README/STATUS/LOG/CONTRACT、main/tests、Oracle DDL/seed、Frontend api/types/mocks。Phase1/2 baseline 59/59。

- WS2 Service/Controller/Repository tests 先失敗。第一版 test literal 轉義語法錯誤修正後，再確認因缺少 WS2 類別而失敗。
- 新增 ws2 package DTO / ConfirmationData / PatientExamCompletionData / Repository / Service / Controller、共用 Runtime ConflictException。
- @Transactional guards → conditional claim USED_AT IS NULL → conditional exam STATUS=PENDING；檢查 affected rows。0 rows throw409、其他非1 throw500。DB exceptions 不吞掉，rollback 前一步 claim。
- 共享 RequestStringDeserializer 移至 common/json，WS1 僅更換 import。Body400 handler 根據 context path + request URI 選 WS1/WS2 各自 contract；新增409。沒有重寫 Phase1/2。
- 第一批單元回歸89/89；完整 test99/99，包含WS2 transaction proxy rollback/commit與三endpoint OpenAPI；package99/99，0 failures/errors/skipped，JAR成功。
- 獨立 read-only 審查：無 actionable findings；未執行 DB。
- 手動 Ws2OracleSmokeIT 只在 -Dtest=Ws2OracleSmokeIT 明確執行，普通test/package excluded。編譯通過，但本 agent 無DB_PASSWORD，因此未執行。只查既有unused token，不造碼；若存在pending token則真正完成並永久保留結果，驗DB欄位、Patient count+1與replay409；無token/已完成exam則明確skip successE2E。

Ruling: WS2 沿用SYSTIMESTAMP，不依WS1 Clock重查規則或增加expiry。成本：Prototype碼仍無到期。
Ruling: missing referenced exam為generic500，不回無效碼或先claim；正常FK下不應發生。
Ruling: 不擷取使用者process密碼；現有8080需原session重啟新版才能測Oracle invalid path。真實結果另記於STATUS，不偽造success。

保留Frontend/MSW、Schema/seed。Next/Admin/Frontend integration未實作；下一階段Backend Phase4 - Next Exam。

Phase3 Oracle最新結果：2026-10-01 21:39:55，使用者重啟新版8080後，WS2 INVALID-CODE-TEST真實Oracle HTTP404 / 無效的確認碼，請重新判斷；{} HTTP400 / 請提供 code1。WS1 {}保持原400，P001/S002正常時段拒絕，Patient P001仍5/0。Swagger HTTP200、OpenAPI三支。臨時18080非真實password服務只驗body400，已停止。
未取得合法真實code1，未直接查unused list或執行manualOracleIT（agent無DB_PASSWORD），success/replay409E2E明確待開放時段或原envsession執行手動IT。沒有造token/重置DB。結果保存ignored .tools/phase3-oracle-smoke.json。

最後乾淨建置：mvnw.cmd clean package BUILD SUCCESS，99/99、0 failures/errors/skipped；清除移動 parser 前遺留的targetclass，最終artifact使用common/json parser。Schema/seed SHA256與原紀錄一致。

# Backend Phase 4 - Next Exam 執行紀錄（2026-10-01 Asia/Taipei）

依使用者提供 Phase 4 prompt 接續現有 Backend。已讀 PROJECT_STATUS / README / API_CONTRACT / IMPLEMENTATION_LOG / backend README、main/tests、Frontend types/api/mocks、Oracle schema/seed。Baseline 99/99。

- 新增 Next Service/Controller tests，先確認因缺少 Next 類別與 prerequisite repository method 而編譯失敗，再實作 next package DTO/Service/Controller。
- PatientRepository 沿用完整 exams query；新增 prerequisite 查詢入口。抽出 common/exam/ExamEligibility、ExamPrerequisiteQueries，WS1 與 Next 共用 status/time/lazy prerequisites；WS1 @Transactional、UUID、INSERT 與拒絕訊息保留。
- Next 只有 PatientRepository SELECT；pending 空列表回全部完成，其他按 examId 排序及 Clock 一次取樣選第一個 eligible。推薦保留完整 Exam（含 completedAt:null）。
- Service fixed Clock tests 涵蓋開始／結束邊界、边界外秒數、排序、completed、closed、blocked/satisfied、空安排、invalid time；Controller 驗 200 完整 JSON / null、404、400、safe500。
- 第一輪完整 test 120/120；追加 PatientRepository SQL 綁定及 NOT EXISTS completed rule，clean package 121/121，0 failures/errors/skipped，BUILD SUCCESS。
- ApplicationSmokeTest 更新 OpenAPI 四 endpoint 與 Next 200/404 schema；原未實作 route404 改測 Admin Progress。
- 依 requesting-code-review skill 進行獨立唯讀 reviewer 審查，沒有 actionable findings；reviewer 未執行 Oracle 或修改資料。
- Schema/seed SHA-256 與原紀錄一致。Frontend/MSW 未修改，文件反映四支 Backend APIs 與 Phase 5 Admin 下一步。

Ruling: 繼續使用單一 Asia/Taipei Clock，不截斷秒／奈秒；start/end inclusive。SQL NOT EXISTS 同時阻擋 pending 與未安排的 prerequisite。避免重複大型 repository 或新 Rule Engine。
Ruling: Agent 無 DB_PASSWORD，請使用者於原 session 重啟新版；不擷取 process 密碼。Oracle 結果完成後追加於 PROJECT_STATUS 與本紀錄，不偽造推薦成功。合法 WS1→WS2 Oracle E2E 維持待開放時段。

Phase 4 真實 Oracle 結果：2026-10-01 22:05:45 +08:00，讀取 8080 OpenAPI 已確認 Next 新路由。P001 next HTTP200 / nextExam=null /「目前沒有可進行的檢查，請洽工作人員」；P999 next HTTP404 /「找不到受檢者」。P001 exams HTTP200 / 0 completed、5 total，全部 pending。Swagger UI/OpenAPI HTTP200，四支 API 與 Next 200三種結果/404 正確。此時晚間皆已關閉，未改時間或 DB。證據 ignored .tools/phase4-oracle-smoke.json。

最終普通 mvnw.cmd test：121/121、0 failures/errors/skipped，BUILD SUCCESS；clean package 同樣121/121 BUILD SUCCESS。Frontend/MSW、schema/seed保留。Phase4完成，下一階段Phase5 Admin Progress。

# Backend Phase 5 - Admin Progress 執行紀錄（2026-10-02 Asia/Taipei）

依使用者指定 Phase 5 文件執行。讀取 workspace 狀態/README/contract/log、現有 main/tests、Frontend Admin types/API/hooks/page/handlers、Oracle schema/seed。無 Git repository，直接既有 workspace 擴充，不重新初始化。

- Wrapper 啟動前失敗：Windows PowerShell 對 null Target[0] 索引；先確認原因再增加空值判斷。未改 Maven/Java/dependency 版本。測試使用專案既有 .maven-user/.maven-cache；沙箱寫 target 遭拒後經 escalation 執行。
- Baseline 121/121。Admin tests先因缺少DTO/Controller/Service/Repository失敗；實作後修正測試 argThat 的 JdbcTemplate overload 型別歧義。
- 新增 admin package：四欄位 record DTO、獨立 Controller、薄 Service、一次 JdbcTemplate LEFT JOIN 聚合。COUNT(PE.EXAM_ID)避免無安排患者被算成1；NVL SUM completed status得到整數0；PATIENT_ID ASC；只SELECT。
- Controller嚴格array/four-field/ordering/0/0、empty200[]、safe500；Service透傳/empty；Repository mapping/SQL shape/single query/empty。新增7項，共128 tests。ApplicationSmokeTest更新OpenAPI五路由、array DTO與integer schema；未知route404回歸保留。
- mvnw.cmd test與clean package：128/128，0 failures/errors/skipped，BUILD SUCCESS；新版JAR建立成功，Phase1–4無回歸。Logs：ignored .tools/phase5-{baseline,red,test,package}.log。
- requesting-code-review獨立唯讀審查：無actionable findings，未連Oracle/修改資料。
- Schema/seed SHA-256與原紀錄一致。Frontend/MSW未修改。Backend v1：5 / 5 main APIs implemented。

Ruling：Phase5文件示例500訊息與現有程式不同，遵循「以workspace為準」「沿用GlobalExceptionHandler」，保持既有「服務暫時無法使用」，不改Phase1–4錯誤契約。

Oracle smoke待處理：本次初始8080連線遭拒，agent無DB_PASSWORD；已請使用者於原安全密碼session啟動新版。尚未宣稱HTTP200或P001/P002計數；測試均不依賴Oracle，mock mapping不取代真實SQL驗證。

尚未完成Frontend real API integration、Full E2E。下一階段Integration Phase - Frontend + Backend + Oracle；本次不開始。

# Integration Phase 執行紀錄（2026-10-02 Asia/Taipei）

使用者授權執行 Integration prompt；以workspace實際狀態為準。文件開頭「Admin Oracle smoke已成功」與現有紀錄不符，本次初始8080拒絕連線，故沒有直接採信成功。已請使用者於安全密碼session啟動Backend。

- 先讀狀態/README/contract/log、Frontend config/bootstrap/API/hooks/types/mocks/三角色page、Backend設定與既有契約。不新增endpoint、相依框架或重寫UI。
- 新增runtime/API wiring tests，先確認缺runtime與baseURL失敗。Vite暫存遭EPERM後經escalation執行，並非修改node_modules。
- runtime集中USE_MSW與API_BASE_URL；development+explicit true啟MSW，Real清理僅本專案mockServiceWorker.js。main await模式準備後render，App僅Mock顯示Demo提示。production build未產生MSW chunk。
- vite proxy8080、5173 strictPort；.env.example Real預設、.env.mock/dev:mock保留Demo。client共用base origin，400/404/409/500與business200處理保留。Staff完成按鈕補allowed+code1 gate，Mock患者404訊息對齊。
- Frontend 17 tests/4 files passed，build成功；Backend128 tests與clean package成功，0 failures/errors/skipped。没有lint配置。Schema/seed/ERD hashes保持原值。
- Browser預設Real後端離線可重試畫面、Mock WS1→WS2進度1/5、同origin Mock→Real去除Mock提示與進度已驗。Real proxy在終端明確ECONNREFUSED，沒有以mock結果假稱DB成功；console觀測無error/warn。
- Logs保存ignored .tools/integration-{red,frontend-test,frontend-build,backend-test,backend-package}.log。

尚待真OracleBrowser smoke：P001/P999、Next、Admin polling與計數、WS1 denial、WS2真API wiring；需要8080已設定密碼服務。合法WS1→WS2 success E2E晚間不可強做；不改時間、schema/seed或fake confirmation。下一階段Final E2E & Stabilization，本次不開始。

Integration獨立唯讀審查：無actionable findings；production MSW runtime/handlers未bundling，QR、polling、invalidation與API契約保持。審查未連Oracle。

Integration真實驗收補充（2026-10-02 21:21–21:24 Asia/Taipei）：使用者啟動新版8080後，Backend/OpenAPI五paths正常。Oracle-backed Patient P001 0/5、P002 0/3；Admin相同，並按Patient exams行數與COMPLETED數計算核對。P999404；Next晚間null/message；WS1 P001/S002時段拒絕200/code1=null；WS2 invalid404。五支Vite proxy均對照真Backend。

Browser驗Patient P001、P999錯誤、Next晚間訊息、Admin0/5與0/3、WS1正常拒絕/無完成按鈕。临時fetch trace捕獲Admin21:23:32.761、37.792、42.818 HTTP200，間隔5.031/5.026秒；trace程式隨後移除。沒有DevTools from-service-worker欄位可讀，使用Real mode不startMSW、已驗同origin清理、browser trace與proxy結果作來源佐證。console觀測無error/warn。

證據ignored .tools/integration-oracle-http.json、integration-proxy-http.json、integration-browser-trace.txt。Phase5 Admin真Oracle smoke完成。Integration完成，合法WS1→WS2 success/replay409與成功後真DB進度更新仍待開放時段。本次未修改DB、時間或fake確認碼，停於Integration。

# Final E2E & Stabilization（2026-10-02 Asia/Taipei）

依使用者Final prompt驗收，未新增功能。Baseline frontend17/backend128通過；真API經5173proxy驗Patient P001/P999、Next、Admin、WS1未知患者/站點/未安排/closed、WS2invalid404。UI unknown patient先由查詢guard擋下，另直接驗WS1API404，不繞過guard。

Admin trace刷新後單一序列21:32:55.912→21:33:00.924→05.934→10.954→15.986，約5秒；離頁後>10秒無新增Adminrequest，返回才恢復。trace已移除、client hash與原備份相同。桌面1366×900/手機390×844三頁可操作；Admin horizontal scroll實際可讀右側。三頁dev與buildpreview直接route+refresh通過。

QR兩種控制可進入/停止並移除video，但readyState0/tracks=[]；兩次安全來源warning，未宣稱實機camera stream或scanner成功。無blocking consoleerror。沒有適合當下合法時段，success與samecode409 pending；P001/S005 prerequisite分支被時段優先擋，automated tests證據保留。

發現文件錯誤：README說preview沒有proxy；GET4173/api/admin/progress200及安裝Vite來源preview.proxy ?? server.proxy證實會繼承，已更正。產品無需bugfix。Final frontend17 tests/build成功，backendcleanpackage128/0fail/0error/0skip成功。臨時preview驗後停止；原5173/8080保留。Schema/seed/ERD未改。

建立docs/FINAL_E2E_TEST_REPORT.md含expected/actual/result、完整checklist與pending；PROJECT_STATUS標Core/IntegrationComplete、FinalE2EPartial、DocumentationInprogress。不勾未執行的Oracle成功流程/實機QR，不reset資料。停止不新增功能。
