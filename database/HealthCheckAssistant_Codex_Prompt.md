# Codex 開發提示詞：健檢現場流程助手 2026

你現在要接手一個大學專題「健檢現場流程助手 2026」的實作工作。

請把這份提示詞視為本專案目前的正式開發交接文件。不要自行擴張需求，不要為了展示技術而加入不必要的框架，也不要偷偷改資料庫設計。先完成目前指定的前端階段，再進入 API / Backend。

---

## 一、專題目的

系統要協助多人健檢流程管理。

主要角色有三種：

1. 工作人員
2. 受檢者
3. 管理者

核心情境：

- 每位受檢者有多個健檢項目
- 每個健檢項目有指定檢查站點
- 每個檢查項目有允許檢查時間
- 某些檢查有前置檢查條件
- 所有受檢者需在規定時間內完成健檢
- 工作人員使用 QR Code 辨識受檢者與站點
- WS1 判斷受檢者目前是否能進行指定檢查
- 若允許，WS1 產生 `code1`
- 工作人員確認完成後，由 WS2 使用 `code1` 更新資料
- 受檢者可以查看目前完成狀態與下一站建議
- 管理後台可以監看當日所有受檢者進度

第一版只做 QR Code，不做 RFID。

下一站推薦第一版只做規則式，不做 AI、不做排隊最佳化。

---

## 二、目前已完成項目

資料庫已完成並驗證，使用 Oracle 19c。

Schema：

```text
HEALTH_CHECK
```

目前共有 6 張核心資料表。

### 1. CHECK_STATION

```text
STATION_ID    VARCHAR2(10)   PK
STATION_NAME  VARCHAR2(100)  NOT NULL
```

用途：健檢站點。

---

### 2. EXAM_ITEM

```text
EXAM_ID       VARCHAR2(10)   PK
EXAM_NAME     VARCHAR2(100)  NOT NULL
STATION_ID    VARCHAR2(10)   FK -> CHECK_STATION.STATION_ID
START_TIME    VARCHAR2(5)    NOT NULL
END_TIME      VARCHAR2(5)    NOT NULL
```

用途：檢查項目設定與允許檢查時間。

---

### 3. PATIENT

```text
PATIENT_ID    VARCHAR2(20)   PK
PATIENT_NAME  VARCHAR2(100)  NOT NULL
```

用途：受檢者基本資料。

---

### 4. PATIENT_EXAM

```text
PATIENT_ID    VARCHAR2(20)   PK + FK -> PATIENT.PATIENT_ID
EXAM_ID       VARCHAR2(10)   PK + FK -> EXAM_ITEM.EXAM_ID
STATUS        VARCHAR2(20)   DEFAULT 'PENDING'
COMPLETED_AT  TIMESTAMP
```

複合主鍵：

```text
(PATIENT_ID, EXAM_ID)
```

STATUS 只允許：

```text
PENDING
COMPLETED
```

用途：記錄某位受檢者需要做哪些檢查，以及完成狀態。

---

### 5. EXAM_PREREQUISITE

```text
EXAM_ID           VARCHAR2(10)   PK + FK -> EXAM_ITEM.EXAM_ID
REQUIRED_EXAM_ID  VARCHAR2(10)   PK + FK -> EXAM_ITEM.EXAM_ID
```

用途：記錄檢查項目的前置條件。

例如：

```text
E005 -> E001
E005 -> E002
```

代表 E005 執行前必須先完成 E001、E002。

---

### 6. CHECK_CONFIRMATION

```text
CODE1         VARCHAR2(100)  PK
PATIENT_ID    VARCHAR2(20)
EXAM_ID       VARCHAR2(10)
CREATED_AT    TIMESTAMP
USED_AT       TIMESTAMP
```

其中：

```text
(PATIENT_ID, EXAM_ID)
```

為複合 FK，指向：

```text
PATIENT_EXAM(PATIENT_ID, EXAM_ID)
```

用途：保存 WS1 產生的 `code1`，並讓 WS2 判斷是否已使用。

---

## 三、目前資料庫成果檔

專案最終應保留：

```text
database/
├── health_check_schema.sql
├── health_check_seed.sql
└── health_check_erd.png
```

不要改動上述 SQL 的資料模型，除非後續明確要求。

---

# 四、目前開發順序

現在要開始：

```text
第一階段：Frontend 網頁
第二階段：REST API / Backend
第三階段：Oracle 串接
第四階段：整合測試
```

目前只實作「第一階段 Frontend」。

不要現在建立 Spring Boot Backend。

但前端 API 介面必須先設計好，使用 Mock API 模擬，讓之後 Backend 可以直接照相同 contract 實作。

---

# 五、Frontend 技術選擇

使用：

```text
React
TypeScript
Vite
shadcn/ui
TanStack Query
MSW (Mock Service Worker)
qr-scanner
```

原則：

- 不使用完整 Admin Framework
- 不使用 Next.js
- 不加入 Redux，除非真的出現跨頁複雜 client state
- Server state 用 TanStack Query
- API 尚未完成前使用 MSW
- QR Code 掃描使用 `nimiq/qr-scanner`
- UI 要能在桌機與手機瀏覽器使用
- 優先使用簡單、可維護、能 Demo 的做法
- 不要塞 AI 功能
- 不要塞 WebSocket
- 管理後台第一版使用 polling 即可
- 不要過度抽象化

---

# 六、專案目錄

請先檢查目前 workspace。

如果尚未建立完整專案結構，建立：

```text
HealthCheckAssistant/
│
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── mocks/
│   │   ├── pages/
│   │   │   ├── staff/
│   │   │   ├── patient/
│   │   │   └── admin/
│   │   ├── types/
│   │   ├── hooks/
│   │   └── lib/
│   └── ...
│
├── backend/
│   └── README.md
│
├── database/
│   ├── health_check_schema.sql
│   ├── health_check_seed.sql
│   └── health_check_erd.png
│
├── README.md
└── PROJECT_STATUS.md
```

如果 database 目錄已有檔案，不可覆蓋。

backend 目前只建立說明檔或空目錄，不要建立 Spring Boot 專案。

---

# 七、前端頁面需求

## A. 工作人員 Staff

路由建議：

```text
/staff
```

需要完成：

### 1. 掃描受檢者

提供：

- 開啟相機掃 QR Code
- 也要提供「手動輸入」fallback
- 讀到 PATIENT_ID 後顯示受檢者資料

範例：

```text
P001
測試受檢者A
```

### 2. 掃描站點

提供：

- 掃描站點 QR
- 手動輸入 STATION_ID fallback

例如：

```text
S002
```

### 3. 呼叫 WS1

前端送出：

```json
{
  "patientId": "P001",
  "stationId": "S002"
}
```

回傳範例：

```json
{
  "allowed": true,
  "message": "可以進行檢查",
  "examId": "E002",
  "examName": "胸部X光",
  "remainingExams": [
    {
      "examId": "E003",
      "examName": "心電圖"
    }
  ],
  "code1": "MOCK-CODE-001"
}
```

若不能進行：

```json
{
  "allowed": false,
  "message": "前置檢查尚未完成",
  "remainingExams": [],
  "code1": null
}
```

### 4. 完成確認

allowed = true 時顯示：

```text
確定完成此站點
```

按下後呼叫 WS2。

---

## B. 受檢者 Patient

路由建議：

```text
/patient
```

第一版不做完整登入。

可先提供 PATIENT_ID 輸入或 demo patient selector。

畫面要顯示：

- 姓名
- 病歷號
- 完成進度
- 已完成項目
- 未完成項目
- 下一站建議

例如：

```text
測試受檢者A
P001

完成進度
2 / 5
40%

已完成
✓ 血液檢查
✓ 胸部X光

待完成
○ 心電圖
○ 腹部超音波
○ 醫師總評

下一站建議
心電圖站
```

---

## C. 管理後台 Admin

路由建議：

```text
/admin
```

顯示當日所有受檢者，例如 Table：

```text
病歷號 | 姓名 | 已完成 | 總項目 | 進度 | 狀態
P001   | A   | 2     | 5     | 40% | 進行中
P002   | B   | 3     | 3     |100% | 已完成
```

使用 TanStack Query polling：

```text
refetchInterval: 5000
```

第一版不做 WebSocket。

---

# 八、Frontend API Contract

目前先建立以下 endpoint contract。

## WS1

```text
POST /api/ws1/check
```

Request：

```json
{
  "patientId": "P001",
  "stationId": "S002"
}
```

---

## WS2

```text
POST /api/ws2/complete
```

Request：

```json
{
  "code1": "MOCK-CODE-001"
}
```

Response：

```json
{
  "success": true,
  "message": "檢查完成狀態已更新"
}
```

---

## Patient Progress

```text
GET /api/patients/:patientId/exams
```

回傳：

```json
{
  "patientId": "P001",
  "patientName": "測試受檢者A",
  "completedCount": 2,
  "totalCount": 5,
  "exams": []
}
```

---

## Next Exam

```text
GET /api/patients/:patientId/next
```

第一版規則：

1. 排除已完成項目
2. 排除目前時間不允許的項目
3. 排除前置項目尚未完成的項目
4. 從剩餘項目選一個作為下一站

MSW 階段不用真的寫完整演算法，只要回傳合理 mock response。

---

## Admin Progress

```text
GET /api/admin/progress
```

回傳所有 demo patients 的進度資料。

---

# 九、Mock Data

前端第一版至少準備：

```text
P001
P002
```

站點：

```text
S001 抽血站
S002 X光站
S003 心電圖站
S004 超音波站
S005 醫師總評站
```

項目：

```text
E001 血液檢查
E002 胸部X光
E003 心電圖
E004 腹部超音波
E005 醫師總評
```

前置條件：

```text
E005 requires:
E001
E002
E003
E004
```

---

# 十、UI 原則

整體不要做成花俏 Dashboard。

目標：

- 醫療現場清楚
- 字體大小足夠
- 高可讀性
- 狀態清楚
- 操作步驟少
- 手機可用
- 不追求動畫

Staff 頁面最重要的是：

```text
掃病人
↓
掃站點
↓
系統判斷
↓
確認完成
```

不要把 Staff 畫面塞滿統計圖。

Patient 頁面重點：

```text
我完成多少
還剩什麼
下一站去哪
```

Admin 頁面重點：

```text
今天有哪些受檢者
誰完成了
誰還沒完成
目前進度多少
```

---

# 十一、QR Scanner

使用：

```text
nimiq/qr-scanner
```

要求：

- component 封裝
- 可啟動 / 停止 camera
- cleanup camera stream
- 處理 permission denied
- 處理沒有 camera
- 提供手動輸入 fallback
- 不可因為掃描失敗讓整個頁面 crash

開發環境 localhost 可測相機。

正式部署需要 HTTPS。

---

# 十二、MSW

請正確建立：

```text
src/mocks/
├── handlers.ts
└── browser.ts
```

只在 development 啟用。

不要把 mock 邏輯散落在 React component 裡。

React component 應該只呼叫正常 API。

例如：

```ts
fetch("/api/ws1/check")
```

而不是：

```ts
if (development) {
  return fakeData
}
```

Mock 與 UI 必須解耦。

---

# 十三、TanStack Query

建立一致的 API layer，例如：

```text
src/api/
├── staffApi.ts
├── patientApi.ts
└── adminApi.ts
```

不要讓 pages 直接散落大量 fetch。

需要：

- query keys 統一
- loading state
- error state
- mutation success/error handling
- WS2 成功後 invalidate patient/admin queries

---

# 十四、TypeScript

建立 API types，例如：

```text
src/types/
├── patient.ts
├── exam.ts
├── api.ts
└── station.ts
```

不要大量使用：

```ts
any
```

---

# 十五、這一階段要交付的結果

完成後必須能執行：

```bash
npm install
npm run dev
```

並可以實際操作：

### Staff

```text
輸入/掃描 P001
輸入/掃描 S002
→ WS1 mock 判斷成功
→ 顯示 code1
→ 按完成
→ WS2 mock 成功
```

### Patient

```text
選 P001
→ 顯示完整健檢進度
→ 顯示下一站
```

### Admin

```text
→ 顯示 P001、P002
→ 每 5 秒重新取得 mock progress
```

---

# 十六、驗收

請完成後自行檢查：

```bash
npm run build
```

必須 build 成功。

如果有 lint：

```bash
npm run lint
```

也必須處理到沒有阻斷性錯誤。

同時檢查：

- 無明顯 console error
- QR scanner 未使用時不應持續占用 camera
- route refresh 不應 crash
- mock API error 時畫面有錯誤訊息
- loading 有合理顯示
- mobile width 可正常操作

---

# 十七、PROJECT_STATUS.md

每完成一個階段都更新：

```text
PROJECT_STATUS.md
```

至少包含：

```text
已完成
目前架構
目前可執行功能
尚未完成
下一步
已知問題
```

不要寫長篇心得。

---

# 十八、README

README 先保持簡潔。

至少包含：

```text
專題名稱
專題用途
目前技術
目錄結構
Frontend 啟動方法
目前實作狀態
```

---

# 十九、Backend 下一階段預留

後續預計：

```text
Spring Boot
Spring Web
Spring JDBC
Bean Validation
Oracle JDBC Driver
springdoc-openapi
```

先不要實作。

Backend 預計：

```text
frontend
    ↓ REST
Spring Boot
    ↓ JDBC
Oracle 19c
    ↓
HEALTH_CHECK
```

資料存取第一版偏向 Spring JDBC，不優先使用 JPA，因為目前有：

- composite primary key
- prerequisite query
- WS1 多條條件判斷
- Oracle 已經有明確 SQL schema

但這是下一階段。

---

# 二十、Codex 執行規則

請按照以下方式工作：

1. 先檢查現有 workspace 與檔案
2. 不刪除任何已存在的 database 成果
3. 不覆蓋不確定用途的檔案
4. 先列出你看到的目前結構
5. 再執行 frontend 初始化
6. 每一個重要步驟完成後自行 build / test
7. 發現錯誤直接修，不要留下半成品
8. 不要加入需求之外的大型框架
9. 不要建立 Backend
10. 不要修改 Oracle schema
11. 不要使用假資料取代 API layer，假資料必須透過 MSW
12. 所有新程式必須能被下一階段 Spring Boot 無痛替換
13. 保持程式結構簡潔，避免過度 engineering

---

# 二十一、現在開始執行

第一個目標：

```text
建立可執行的 Frontend Prototype v1
```

完成標準：

```text
React + TypeScript + Vite 可正常執行
三個角色頁面存在
MSW API 可運作
TanStack Query 已接入
Staff WS1/WS2 mock 流程可操作
Patient progress 可顯示
Admin progress 可顯示並 polling
QR Scanner component 已建立且有 fallback
npm run build 成功
PROJECT_STATUS.md 已更新
README.md 已更新
```

不要只建立空 UI。

要做到可以實際 Demo 的程度。
