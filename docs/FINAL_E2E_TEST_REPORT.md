# Final E2E & Stabilization 測試報告

測試日期：2026-10-02 21:29–21:39（Asia/Taipei）。
狀態：**Final E2E: Partial — Success E2E pending legal time**。

## 環境

- Windows、Java 17.0.12、Spring Boot 3.5.9、Oracle 19c / HEALTH_CHECK。
- Frontend：http://127.0.0.1:5173（Vite Real mode，MSW預設關閉）。
- Backend：http://localhost:8080；Frontend /api 經Vite proxy。
- Build preview：臨時 http://127.0.0.1:4173，驗收後停止。
- 瀏覽器：Codex in-app browser；桌面1366×900、手機390×844。
- 使用目前DB，不重灌seed、不改Schema/Clock/開放時間、不造確認碼。

## 自動測試

| 項目 | 結果 |
| --- | --- |
| Frontend baseline / 最終 npm test | 17 tests，4 files passed |
| Frontend 最終 npm run build | TypeScript與Vite production build成功 |
| Frontend lint | 無lint script，未執行/未宣稱通過 |
| Backend mvnw.cmd test | 128 tests，0 failures/errors/skipped，BUILD SUCCESS |
| Backend mvnw.cmd clean package | 128 tests，0 failures/errors/skipped，BUILD SUCCESS |

Backend沿用專案Maven cache參數；一般測試不連Oracle。SQL/transaction成功路徑由自動測試覆蓋，不算真實成功E2E。Schema/seed/ERD SHA-256與PROJECT_STATUS原值一致。

## 真實API與人工瀏覽器驗收

| Case | Input | Expected | Actual | Result |
| --- | --- | --- | --- | --- |
| Patient真資料 | P001 | 200、目前DB進度 | 0/5、5項PENDING、completedAt=null；UI姓名/進度正常 | PASS |
| Unknown patient | Patient P999 | 404、Backend message、無crash | 找不到受檢者、可重試 | PASS |
| Next | P001，晚間 | 200/null、正常訊息 | 目前沒有可進行的檢查，請洽工作人員 | PASS |
| Admin真資料 | progress | 計數與Patient一致 | P001 0/5、P002 0/3、總2人、進行中2人 | PASS |
| Staff unknown patient | P999/S001 | 404/找不到受檢者 | WS1 API404；UI由Patient查詢404顯示訊息並禁用WS1按鈕 | PASS |
| Staff unknown station | P001/S999 | 404/找不到站點 | API與UI一致，輸入可修正 | PASS |
| Staff unassigned | P002/S005 | 200/allowed=false | 受檢者未安排此站檢查；code1=null、無完成按鈕 | PASS |
| Staff closed time | P001/S002 | 200/allowed=false、正常業務拒絕 | 不在允許檢查時間（08:00–16:00）；非API error alert | PASS |
| Prerequisite blocked | P001/S005 | 合法時段才檢查前置 | 目前先被10:00–17:00時段拒絕；前置分支無法真實到達 | PENDING LEGAL TIME |
| WS2 invalid code | INVALID-CODE-FINAL-E2E | 404/無效確認碼 | Vite proxy→Backend回「無效的確認碼，請重新判斷」 | PASS API |
| 三頁console | Staff/Patient/Admin | 無blocking error/rejection | 未觀測blocking error或未處理rejection；QR有安全來源warning | PASS（限制見下） |

P001/S005的前置case具備現有資料，但因晚間時段判斷優先而受限，並非缺seed。Ws1ServiceTest前置拒絕及NextExamServiceTest的blocked/missing prerequisite tests通過，未改DB硬造case。

## Polling與來源驗證

Admin重新整理後的臨時browser fetch trace：

| 台北時間 | HTTP | 與前一次間隔 |
| --- | --- | --- |
| 21:32:55.912 | 200 | 起始 |
| 21:33:00.924 | 200 | 5.012s |
| 21:33:05.934 | 200 | 5.010s |
| 21:33:10.954 | 200 | 5.020s |
| 21:33:15.986 | 200 | 5.032s |

其後直到21:33:56.119仍保持單一約5秒序列，沒有累積多組timer。直接導頁+refresh各有一次起始請求，未將其誤認為timer重複。離開Admin至Patient後觀察超過10秒，沒有新增Admin請求，直到21:35:20返回Admin才恢復。

Real mode不startMSW，前一階段同origin Mock→Real清理已驗。真時鐘拒絕與Backend/Vite proxy結果一致。本次使用browser fetch trace及proxy HTTP證據確認；工具不提供DevTools「from service worker」欄位，未宣稱讀到該欄位。臨時trace已移除，最終client檔案與診斷前SHA-256相同。

## 路由、版面、QR

- /staff、/patient、/admin直接導航與refresh：dev與production preview皆正常，無白屏/404/crash。
- Preview三路由HTTP200/SPA root；preview /api/admin/progress HTTP200。確認安裝的Vite原始碼使用 preview.proxy ?? server.proxy；更正README先前「preview沒有proxy」的錯誤描述。
- 三頁桌面/390px手機基本版面PASS；Staff查詢、QR與站點按鈕未截斷，Patient文字可讀。
- Admin表格574px、容器333px/overflow-x:auto；实际scrollLeft約240.67px，能讀取右側進度/狀態；整頁沒有水平溢出。
- QR受檢者與站點都能進入相機區、按停止回復按鈕且移除video。環境未取得MediaStream（readyState=0、tracks=[]），出現安全來源warning；**未驗證真實串流啟動/停止、硬體釋放或實際掃碼**。相機實機檢查保持待辦，manual fallback已驗，不阻塞其他項目。

## Final E2E checklist

- [x] Patient P001 real data
- [x] Patient P999 404 UI
- [x] Next real data
- [x] Admin real polling
- [x] Staff unknown patient（UI查詢guard + WS1 API404）
- [x] Staff unknown station
- [x] Staff unassigned station
- [x] Staff closed-time denial
- [x] WS2 invalid code（proxy API）
- [ ] Prerequisite blocked real E2E — PENDING LEGAL TIME
- [ ] WS1 allowed → 真code1 — PENDING LEGAL TIME
- [ ] WS2 success — PENDING LEGAL TIME
- [ ] Oracle PATIENT_EXAM updated
- [ ] Oracle CHECK_CONFIRMATION created/used/timestamps
- [ ] Patient progress auto updated after real WS2
- [ ] Next updated after real WS2
- [ ] Admin polling updated after real WS2
- [ ] Same code1 → 409
- [ ] QR basic real-camera check（UI開/關控制已驗，串流/實掃未驗）
- [x] Direct route refresh（dev + build preview）
- [x] Desktop/mobile basic check
- [x] Frontend tests/build
- [x] Backend tests/package

## 修正與剩餘項目

未發現需修改產品程式的阻斷性Bug；只更正README的preview proxy描述，沒有新增功能/endpoint/依賴、重設DB或UI改版。

成功流程與重送409沒有執行，因測試時間已超過所有檢查時段；不以Mock成功替代Oracle E2E。於下一個合法時段先確認P001仍有可做項目，由Staff真正WS1取得code1，查CHECK_CONFIRMATION初始USED_AT=NULL，再由UI完成WS2，核對STATUS/COMPLETED_AT/USED_AT、Patient/Next/Admin更新及same-code409。保留成功資料，不reset。直接DB欄位對照需在安全密碼session/SQL Developer執行，不從process擷取secret。

Core Development: Complete；Integration: Complete；Final E2E: Partial；Documentation: In progress。

證據（workspace ignored .tools）：final-http-evidence.json、final-browser-evidence.txt、final-preview-routes.json、final-frontend-baseline.log、final-frontend-test.log、final-frontend-build.log、final-backend-baseline.log、final-backend-package.log。完整真API evidence不含帳號密碼或JDBC secret。
