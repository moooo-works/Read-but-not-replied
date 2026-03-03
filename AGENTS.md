# AGENTS.md — NotiReply Assistant 協作規範（Jules / Codex）

本文件定義 AI agents（Jules、Codex）在本 repo 的協作方式，目的：

- 避免各自做完卻彼此找不到成果
- 避免未 review 就直接進 main
- 確保每次變更都有可追蹤的 PR、測試與驗證方式

---

## 1. 角色分工

### Jules（實作主力）

- **負責**：功能開發、專案骨架、整合、CI 修復、可跑的 end-to-end 垂直切片
- **輸出**：可合併的 PR（含測試與驗證步驟）
- **禁止**：未經 PR review 直接合併到 main（除非 repo owner 明確要求）

### Codex（審查 / 精修 / 測試補強）

- **負責**：架構審查、局部重構、測試補強、文件（README/AGENTS/規格）與風險收斂
- **輸出**：以 PR 或 PR comment 的方式交付（不要只在聊天中描述）
- **禁止**：大幅重寫 Jules 剛完成的核心流程（除非先提出重構計畫 + 風險/回滾）

---

## 2. 基本工作流程（必遵守）

### 2.1 一律用分支 + PR（禁止直接改 main）

- main 只接受 **PR merge**
- 不允許直接 push 到 main（除非緊急 hotfix 且 owner 明確要求）

### 2.2 分支命名規範

| 前綴 | 用途 |
|------|------|
| `feat/<topic>` | 新功能 |
| `fix/<topic>` | 修 bug |
| `refactor/<topic>` | 重構 |
| `test/<topic>` | 測試補強 |
| `chore/<topic>` | CI/依賴/工具鏈 |

> 範例：`feat/reminders-snooze`、`fix/notification-listener-manifest`

### 2.3 PR 必須包含

- **Summary**：這個 PR 做了什麼（3–8 行內）
- **How to Verify**：如何驗證（至少提供 gradle 指令）
- **Tests**：新增/更新哪些測試、測試結果
- **Known limitations / risks**：若有行為限制，需明確列出

---

## 3. 「成果可被找到」規則（避免 Codex 找不到）

### 3.1 每個 PR 必須在描述中寫明「改動範圍」

請列出：

- 主要檔案 / 主要模組路徑
- 新增的 public API（Repository/UseCase/Worker 等）
- DB schema / migration 是否變更

### 3.2 PR 內一定要連到相關 Issue/任務

若沒有 Issue，請至少寫：

- `Context: Step X <name>`（例如 Step 8 Reminders）

### 3.3 不接受「聊天中完成」的交付

如果 agent 在 chat 說完成，但 GitHub 上沒有 PR/commit，視同未交付。

---

## 4. 合併（Merge）規則

### 4.1 合併條件（必備）

- GitHub Actions **全綠**
- `./gradlew :app:assembleDebug` 可過
- `./gradlew :app:testDebugUnitTest` 可過
- 變更涉及核心功能時，需至少 1 位 reviewer（owner 或另一 agent）確認

### 4.2 合併策略

- 預設使用 **Squash and merge**（main 歷史乾淨）
- 若 PR 內含明確分段 commit（例如「功能」+「CI wrapper」），可用 merge commit 保留兩筆

---

## 5. 測試與品質門檻

### 5.1 必跑指令（每個 PR 至少）

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

### 5.2 Worker / Notification 類變更（建議）

- **WorkManager**：work-testing 測試維持穩定（避免 flaky）
- **Notification/Reply**：JVM 單測優先，盡量不要把測試全丟給 Robolectric

---

## 6. 架構邊界（避免耦合失控）

### 6.1 不允許的耦合

- UI / ViewModel 直接操作 `PendingIntent`、`RemoteInput`、`WorkManager.getInstance`
- Service 直接寫 DB（應轉 DTO 後交給 Processor/Repository）

### 6.2 建議抽象（目前 repo 已採用）

- `ReplyHandler`, `PendingIntentSender`, `IntentOpener`, `Logger`
- `ReminderScheduler`, `NotificationPublisher`

---

## 7. 資料 / 隱私 / 記錄（Logging）規則

- **禁止**在 log 印出訊息內容（message text）
- Reminders 通知內容避免顯示完整訊息；以對話標題 + note 為主
- 任何「ephemeral」能力（Quick Reply）需在 README/設定中說明限制

---

## 8. 溝通格式（讓 Jules/Codex 互通）

每次 agent 回覆「完成/交付」時，需附上：

1. PR 連結或 PR 編號
2. 分支名 + HEAD commit hash
3. 變更摘要（5 行內）
4. 測試指令與結果（pass/fail）
5. 待辦（如有）與風險（如有）

---

## 9. 緊急規則（避免又發生「改了但沒人看到」）

若 agent 無法 `git push`（平台限制），必須：

- 立即在 PR/commit 描述中寫清楚「交付方式」與「如何觸發 publish」
- 在完成前回報：分支名、commit hash、以及如何驗證

---

## 10. Code Owners（可選）

若之後要加 `CODEOWNERS`，建議：

| 路徑 | 說明 |
|------|------|
| `app/src/main/.../service/**` | 通知擷取與 processor |
| `app/src/main/.../worker/**` | WorkManager/reminders |
| `app/src/main/.../feature/**` | Compose UI |
| `app/src/main/.../core/database/**` | Room schema/DAO |

---

## 11. 快速 Checklist（PR 作者自檢）

- [ ] 我有開 PR，而不是直接進 main
- [ ] CI 全綠
- [ ] `assembleDebug` / `testDebugUnitTest` 可跑
- [ ] PR 描述含 Summary + How to Verify
- [ ] 沒有 log 出訊息內容
- [ ] Quick Reply / Reminders 的限制有寫清楚
