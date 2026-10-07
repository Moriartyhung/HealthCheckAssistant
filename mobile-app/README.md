# HealthCheckAndroid — 健檢現場流程助手 Android 端

| App | 專案 | applicationId |
|---|---|---|
| 受檢者 | HealthCheckPatientAndroid | com.healthcheck.patient |
| 工作人員 | HealthCheckStaffAndroid | com.healthcheck.staff |

API 規格：`openapi.json`（唯一來源）。

## Backend Base URL
- Build 預設值：各專案根目錄 `gradle.properties` → `healthcheck.baseUrl`
- 執行期：App 右上角「伺服器設定」可直接改（存在 SharedPreferences），不必重新 Build
- Backend 在另一台電腦 → 用那台電腦的 LAN IP，例如 `http://192.168.1.23:8080/`
- 不要用 `localhost`；`10.0.2.2` 只在 Backend 與 Emulator 同一台 Windows 時可用
- Debug build 允許 HTTP（`app/src/debug/res/xml/network_security_config.xml`），Release 不允許

## Build
```
gradlew.bat assembleDebug
```
APK：`app/build/outputs/apk/debug/app-debug.apk`

## QR 格式
純文字：受檢者 `P001`、站點 `S002`。


## Android Studio 與環境

分別開啟本目錄下的 `HealthCheckPatientAndroid/` 或 `HealthCheckStaffAndroid/`；本目錄本身不是合併的 Gradle 專案。保留各自 Gradle Wrapper、設定與 source。此機安裝 Android Studio 2025.2.1、內建 JBR 21；專案使用 AGP 8.13.0、Gradle 8.14.3、Kotlin 2.2.20、JDK 17+、compile/target SDK 36、min SDK 26。準備 Android SDK Platform 36，讓 IDE 產生本機 `local.properties`，該檔不提交。

使用 API 26+ 真機或 Emulator。Spring Boot Backend 必須先啟動於 8080 並連上 Oracle，啟動步驟見 [根目錄 README](../README.md)。目前兩個專案的 `gradle.properties` 中 `healthcheck.baseUrl` 都是 `http://192.168.0.100:8080/`。由各自 `app/build.gradle.kts` 注入 `BuildConfig.DEFAULT_BASE_URL`，Retrofit 與 SharedPreferences 設定位於：

- `HealthCheckPatientAndroid/app/src/main/java/com/healthcheck/patient/data/api/ApiConfig.kt`
- `HealthCheckStaffAndroid/app/src/main/java/com/healthcheck/staff/data/api/ApiConfig.kt`

App 功能、API URL、package、applicationId、namespace 與套件版本均沿用原始工作目錄內容。本次不重跑會改動 Oracle 資料的測試；本機 build-logs、SDK 路徑、IDE/cache、簽章檔與輸出 APK 不上傳。`build-debug.ps1` / `emulator-smoke-test.ps1` 為既有輔助腳本，可能下載工具或操作 Emulator；本次驗證直接使用各專案 Wrapper。

## 本次 Repository 整理

原始位置：`D:\AAAAAAAAa\Health\HealthCheckAndroid`，保留原始工作目錄與未提交修改。新位置：`mobile-app/`，含兩個完整 Android Studio 專案，不建立第二個 GitHub Repository。V1 Word / PDF / PPT 後續加入根目錄 `docs/`；本次保留既有 `v1.0` tag。

## 本次編譯驗證（2026-10-07）

在 Repository 的兩個 Android 專案目錄分別執行：

```powershell
$env:JAVA_HOME = 'D:\Andriod studio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest --console=plain
```

使用本機 Android Studio 2025.2.1 內建 JBR 21.0.8、Android SDK Platform 36；`JAVA_HOME` 路徑請依實際安裝位置調整，SDK 路徑僅存於 ignored `local.properties`。

| 專案 | 編譯 | 既有單元測試 | Debug APK |
|---|---|---|---|
| HealthCheckPatientAndroid | BUILD SUCCESSFUL，exit 0 | 3 passed，0 failures/errors | `HealthCheckPatientAndroid/app/build/outputs/apk/debug/app-debug.apk` |
| HealthCheckStaffAndroid | BUILD SUCCESSFUL，exit 0 | 8 passed，0 failures/errors | `HealthCheckStaffAndroid/app/build/outputs/apk/debug/app-debug.apk` |

既有 `android.overridePathCheck=true` 實驗性警告及部分 native library 無法 strip 的訊息未阻止編譯，未因此更改設定或版本。此驗證涵蓋 Debug 編譯與既有 JVM 單元測試；未重新執行 Emulator / 真機 / Oracle 整合測試。

複製時核對 69 個專案檔案的 SHA-256；除 README / .gitignore 整理外，所有上傳內容與原始工作目錄一致，包含未提交的修正與測試。保留 package、applicationId、namespace、API 設定與所有既有套件版本。

既有 annotated tag `v1.0`：tag object `96391469c5681928f762eb16240ff8b217891253`，指向 commit `6fc0b7525dad66f01b82a11c4fabbbd4f72eeda4`，早於此次 Android 加入。本次不修改 tag；後續 V1 Word / PDF / PPT 加入 `docs/` 後再決定最終 V1 標記。
