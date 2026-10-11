# 健檢現場流程助手 2026

**Health Check Assistant 2026 — V1.1.0**

Version: v1.1.0

本次更新 Web 患者 CRUD 與檢查安排，整合 frontend / backend / mobile-app / database 至同一 Repository，保留 Spring Boot REST API 與 Oracle 架構。Web 與 Android 共用 API；真實跨端驗證範圍以驗收文件為準。發布副本的 Build / Test 結果見 Release notes。

此 Repository 保存 V1 基礎系統，後續 Android App、專題文件與 V2 都沿用同一個 Repository，以 commit 與版本 tag 保留歷史。

協助工作人員確認檢查資格、受檢者查看健檢進度與下一站，以及管理者監看當日進度。
目前完成 Frontend Prototype v1、Backend v1 核心流程與患者管理／檢查安排 API 與 Integration 的模式切換/API wiring。Frontend development 預設透過 Vite proxy 呼叫真 Backend；Mock Demo 需明確啟用。V1.1 發布副本驗證：前端 31 項測試與 build、後端 158 項測試與 package 通過；兩個 Android App 的 Debug 編譯與既有單元測試通過。真 Oracle 瀏覽器驗收已通過 Patient、Next、Admin 與 WS1；WS2 無效碼已經 Vite proxy 驗證。成功 WS1→WS2 E2E 尚待合法時段，詳見 [驗收報告](docs/FINAL_E2E_TEST_REPORT.md)。

## V1 功能與驗證範圍

- Web 前端 CRUD：管理者可新增、查詢、修改、刪除患者，以及安排／調整患者檢查項目；保留 Staff、Patient、Admin 的資格判斷、完成更新與進度查詢流程。
- Spring Boot REST API：Patient Exams、WS1、WS2、Next Exam、Admin Progress，加上管理者患者 CRUD 與檢查目錄／安排 API。
- Oracle Database：Schema、Seed 與 ERD，後端以 JdbcTemplate 存取。
- Web / API / Oracle 串接：Web 預設使用真 API，已保存只讀查詢及拒絕情境驗收紀錄。
- WS1 / WS2 核心流程：資格與前置條件判斷、確認碼、交易式完成更新及重複提交保護。
- Android App API 串接已驗證、跨端資料同步測試：依專題負責人提供的 V1 完成說明；Android 原始碼已納入 `mobile-app/`；原有本機測試日誌未上傳，此次編譯結果見 `mobile-app/README.md`。
- 現有 Repository 驗收紀錄仍將真實 Oracle WS1→WS2 成功、成功後同步更新與相同確認碼 409 列為待合法時段驗證；Mock／單元測試成功不視為真 Oracle E2E 完成。

## V1 系統架構

```text
Web Frontend ──→ Spring Boot REST API ──→ Oracle Database
Android App ──→ Spring Boot REST API ──→ Oracle Database
```

Web 與 App 都透過 Spring Boot REST API，不直接存取 Oracle。Android App 已加入 `mobile-app/`，包含受檢者與工作人員兩個獨立 Android Studio 專案。

技術：React、TypeScript、Vite、Tailwind CSS、shadcn/ui、TanStack Query、MSW、qr-scanner。

```text
frontend/       前端、API layer、MSW 與測試
backend/        Spring Boot Patient / WS1 / WS2 / Next / Admin APIs、Maven Wrapper 與測試
database/       既有 SQL、ERD 與交接規格
mobile-app/     Android 受檢者 / 工作人員 App
docs/           API contract、執行紀錄
PROJECT_STATUS.md
health_check_schema.sql / health_check_seed.sql / Health_Check_ERD.png
api-docs.json / 啟動方式.txt
README.md / .gitignore
```

保留目前可運作的 `frontend/`、`backend/`、`database/` 名稱與路徑，不搬動程式。`docs/` 已包含 API 與測試文件；後續 Word / PDF / PPT 也放在這裡。Android 來源為 `D:\AAAAAAAAa\Health\HealthCheckAndroid`，已完整整理至 `mobile-app/`；原始資料夾保留。Web 使用實際既有路徑 `frontend/`。


## Android App 啟動

`mobile-app/` 包含兩個完整專案，請在 Android Studio 分別開啟：

- `mobile-app/HealthCheckPatientAndroid`：受檢者，`com.healthcheck.patient`。
- `mobile-app/HealthCheckStaffAndroid`：工作人員，`com.healthcheck.staff`。

專案既有環境：Android Gradle Plugin 8.13.0、Gradle Wrapper 8.14.3、Kotlin 2.2.20、JDK 17+、Android SDK Platform 36，最低裝置版本 Android 8.0 / API 26。使用支援該 AGP 的 Android Studio，透過 SDK Manager 準備 SDK，並讓 IDE 產生各專案自己的 `local.properties`（不提交）。可使用 Emulator 或符合版本的 Android 真機。

先依上方方式啟動 Spring Boot Backend（8080）並完成 Oracle 連線。兩個專案的 `gradle.properties` 中 `healthcheck.baseUrl` 目前皆為 `http://192.168.0.100:8080/`，未修改。各自 `app/build.gradle.kts` 注入 `BuildConfig.DEFAULT_BASE_URL`；執行期設定與 Retrofit 位於各專案 `app/src/main/java/com/healthcheck/patient/data/api/ApiConfig.kt` 或 `com/healthcheck/staff/data/api/ApiConfig.kt`。

App 右上角「伺服器設定」可覆寫 URL，儲存在 SharedPreferences。只有 Emulator 與 Backend 在同一台電腦時才使用 `http://10.0.2.2:8080/`；真機或另一台電腦的 Backend 使用可連線的 LAN IP。Debug 允許 HTTP，Release 不允許 HTTP。這些均為既有行為。

分別進入兩個 Android 專案目錄後執行：

```powershell
.\gradlew.bat assembleDebug
```

各自 APK 輸出：`app/build/outputs/apk/debug/app-debug.apk`。更多說明與本次驗證見 [mobile-app/README.md](mobile-app/README.md)。Web 與 Android App 都不直接存取 Oracle Database，而是透過 Spring Boot REST API 存取資料。

## Oracle Database 初始化

使用 Oracle 19c 或相容環境，由管理者先建立專題帳號並授予建表與 USERS tablespace 配額，再以該帳號（預設 `HEALTH_CHECK`）連線至**新的空白 Schema**。SQL 不建立使用者，也不內含帳號密碼。

在 SQL Developer 選擇正確檔案編碼後使用「執行指令碼」，依序執行：

1. [database/health_check_schema.sql](database/health_check_schema.sql)
2. [database/health_check_seed.sql](database/health_check_seed.sql)

Seed 保留原始 Big5 編碼，請勿直接以 UTF-8 解讀或覆寫；Schema／Seed 的根目錄原檔與 `database/` 副本位元一致。Seed 末尾含 `COMMIT`。這些初始化 INSERT 不供重複執行；不要在有既存成果的資料庫重新灌入。Spring Boot 的 `spring.sql.init.mode=never`，不會自動執行這兩份 SQL。

## 設定與敏感資料

`backend/src/main/resources/application.yml` 使用 `${DB_PASSWORD}`，沒有硬編碼真實密碼；DB URL 預設為 localhost、帳號預設 `HEALTH_CHECK`。保留本機可運作設定。已提供 [backend/.env.example](backend/.env.example)（假密碼 `CHANGE_ME`）與 [frontend/.env.example](frontend/.env.example)；Spring Boot 不會自動讀取 `.env`，請使用下方 PowerShell 環境變數啟動。

`.gitignore` 排除真實 `.env`、local 設定、私鑰、IDE 資料、build output、Node dependencies 與本機 Maven 工具／cache；保留已確認不含秘密的 `.env.mock`、Maven Wrapper、lockfile 與正式 SQL。任何前端 `VITE_*` 值都可能進入瀏覽器，不能放資料庫密碼或 Token。

## Real API mode（預設）

Node.js 22.12+ 或 24 LTS、npm、Java 17+。Backend 使用 Maven Wrapper（不是 Gradle），首次執行需下載 Maven 與 dependencies。需要兩個 PowerShell；Backend 密碼只在本機安全輸入，不寫入 Frontend 或設定檔。下列路徑請依 clone 的實際位置調整。

Terminal 1：

```powershell
cd D:\HealthCheckAssistant_V1_Latest\backend
$env:DB_URL = 'jdbc:oracle:thin:@localhost:1521:orcl'
$env:DB_USERNAME = 'HEALTH_CHECK'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Oracle password' -AsSecureString)).Password
.\mvnw.cmd spring-boot:run
```

Backend 為 http://localhost:8080，Swagger 為 http://localhost:8080/swagger-ui/index.html。DB_URL 設定與 Maven cache 使用見 backend/README.md。

Terminal 2：

```powershell
cd D:\HealthCheckAssistant_V1_Latest\frontend
npm ci --include=dev
npm run dev
```

開啟 http://127.0.0.1:5173，使用 `/staff`、`/patient`、`/admin`。

```powershell
npm test
npm run build
```

未配置 lint；build 包含 TypeScript 檢查。`.env.example` 提供 `VITE_API_BASE_URL=`、`VITE_USE_MSW=false`，未設定時即為 Real mode。`/api` 由 Vite 代理到 8080，固定使用 5173（佔用時明確報錯）。切換環境後重新啟動 Vite。

`VITE_API_BASE_URL` 是選用的 Backend origin，例如 `http://localhost:8080`，不包含 `/api`；所有 API 共用同一個設定。預設留空，使用同源 proxy。跨網域 origin 需由部署環境處理 CORS，本次未增加 Spring CORS。

`npm run preview` 不啟用 MSW；目前 Vite preview 沿用 server.proxy，Backend 8080 啟動時可在本機預覽真 API。這只是本機預覽行為，正式部署仍需由部署端將 `/api` 反向代理到 Backend（或設定已允許 CORS 的 Backend origin）。

## Mock mode（獨立 Demo）

Backend 不在時：

```powershell
cd D:\HealthCheckAssistant_V1_Latest\frontend
npm run dev:mock
```

`.env.mock` 明確設定 `VITE_USE_MSW=true`。也可在 ignored `.env.local` 設定後執行 `npm run dev`；請將 base URL 留空。只有 development 且值恰為 `true` 才啟動 MSW。Production 永遠不啟動 Mock。切回 Real mode 會自動解除本專案 `/mockServiceWorker.js` 註冊，不需要每次手動 unregister。

## Mock Demo 操作

1. Staff 使用 P001、S002 → 判斷 → 顯示 code1 → 確定完成此站點。
2. 切至 Patient，以 P001 查看 1/5、20% 與下一站建議。
3. 切至 Admin，查看 P001、P002，資料每 5 秒自動更新。
4. Staff 使用 P001、S005 可展示前置檢查阻擋；完成 S001–S004 後才允許 S005。
5. P999 或 S999 可展示 API 錯誤；輸入有效代碼即可繼續。

Mock 初始 P001 有 5 項、P002 有 3 項，皆待完成。檢查時間固定 **10:30**，進度僅保存在當前瀏覽器頁面的記憶體；重新整理會重設，不跨分頁同步。WS2 成功會 invalidate patient/next/admin queries。

QR 內容為純文字病歷號（如 `P001`）或站點代碼（如 `S002`）。掃描成功、停止、切換頁面與錯誤時會清理 camera；沒有相機或拒絕權限時使用手動輸入。相機需 localhost 或 HTTPS，手機無法直接用 HTTP 區域網路位址掃描。

正式部署需 SPA fallback 到 index.html，並將 `/api` 交由 Backend。此版無登入與權限驗證。

## 資料庫成果保護

初始三份成果位於 workspace 根目錄，本次保留原檔並新增相同位元內容副本至 database；未修改 SQL、ERD 或 Oracle Schema。SHA-256 已核對一致，詳見 PROJECT_STATUS.md。

API 定義見 [docs/API_CONTRACT.md](docs/API_CONTRACT.md)。整合方式依 [MSW 文件](https://mswjs.io/docs/integrations/browser/)、[shadcn/ui Vite 文件](https://ui.shadcn.com/docs/installation/vite) 與 [qr-scanner 官方文件](https://github.com/nimiq/qr-scanner)，並以已安裝套件型別為準（MSW 3 使用 `onUnhandledFrame`）。

Backend 啟動、環境變數、Swagger 與 Oracle smoke test 步驟見 [backend/README.md](backend/README.md)。目前提供 `GET /api/patients/{patientId}/exams` 、`POST /api/ws1/check` 、`POST /api/ws2/complete` 與 `GET /api/patients/{patientId}/next`，以及 `GET /api/admin/progress`。

## 版本規劃

- **V1.0**：保存 CRUD + REST API + Oracle Database + Web/App Integration 的專題基礎成果；各項可追溯的實測範圍與待驗項目以上述說明及測試報告為準。既有 `v1.0` tag 保持原指向，早於 Android App 加入；待 Word / PDF / PPT 齊備後再決定最終 V1 tag，不移動或覆蓋現有 tag。
- **V2.0（規劃中，尚未完成）**：預計加入 Constraint Programming、多目標排程與動態重新排程，在 V1 架構上繼續開發。

Android App 已加入同一 Repository 的 `main`；接下來先把 V1 Word / PDF / PPT 加入 `docs/`。V2 僅保留原有規劃，本次未開發。V2 完成後再建立 `v2.0` tag，不建立獨立 V1／V2 Repository，也不覆蓋 V1 歷史。









