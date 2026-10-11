# 本機啟動檢查（2026-10-11，Asia/Taipei）

專案位置：`D:\HealthCheckAssistant_V1_Latest`。

## 已定位並處理的問題

- 前端缺少整個 `node_modules`，因此 `npm run dev` 找不到 Vite。已以 `npm ci --include=dev` 安裝 lockfile 指定的 341 個套件；未修改套件版本。
- `啟動方式.txt` 缺少首次安裝步驟、明確 DB_URL 與服務啟動後的串接檢查。已補齊，加入常見 npm、Java、port、proxy 及 Oracle 錯誤處理。
- 根 README 與後端 README 的啟動／Maven cache 範例仍使用舊目錄。已更新為本機專案位置。
- `.gitignore` 已加入本機 `.npm-cache/`。

## 驗證結果

| 項目 | 結果 |
| --- | --- |
| Node.js / npm | 24.11.0 / 11.6.1 |
| Maven Wrapper | Maven 3.9.11，使用 JAVA_HOME 的 JDK 17.0.12 |
| 前端 `npm test` | 7 個檔案、31 項測試通過 |
| 前端 `npm run build` | 通過 |
| 後端 `mvnw.cmd package` | 158 項測試通過，0 failure/error/skipped，BUILD SUCCESS |
| API `mvnw.cmd spring-boot:run` | 成功於 8080 啟動 |
| 前端 `/staff`、`/patient`、`/admin` | HTTP 200 |
| 前端 `/src/main.tsx` | HTTP 200 |
| Swagger / OpenAPI | HTTP 200，OpenAPI 含 8 個 paths |
| Vite `/api/__startup_probe__` | 後端 HTTP 404，證明代理可到達 API；此路徑不存在且不觸及資料庫 |
| OracleServiceORCL / Listener | 正在執行；127.0.0.1:1521，orcl READY |

測試、安裝與 HTTP 驗證在完整本機執行環境完成。沙箱曾造成 npm DNS 失敗、Vitest 暫存模組 ENOENT，以及 Maven 測試子程序停滯；在本機環境重跑後通過，未因此變更產品程式碼。

## 尚待真實 Oracle 帳密驗證

使用者已確認本機 `localhost:1521`、SID `orcl`、帳號 `HEALTH_CHECK`。Agent 未取得真實密碼。API 啟動驗證使用臨時佔位密碼，僅存取 Swagger/OpenAPI 與不存在的路由；沒有執行任何資料庫 API，因此尚不能確認登入、schema、SQL 或真實資料讀寫成功。

請依根目錄 `啟動方式.txt` 第二節在自己的 PowerShell 安全輸入密碼，啟動 API 後執行第四節的兩個只讀查詢，核對直連與前端代理回應。密碼不可貼進聊天。此次未執行初始化 SQL、資料修改型 API 或手動 Oracle integration tests。

HTTP 驗證只確認服務、路由、模組與 proxy 回應，不代表完整瀏覽器操作或真 Oracle E2E 已通過。Android 專案已列入資料夾盤點，本次針對 Web/API 啟動，不宣稱完成 Android 編譯或真機驗證。
