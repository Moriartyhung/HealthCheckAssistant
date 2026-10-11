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
