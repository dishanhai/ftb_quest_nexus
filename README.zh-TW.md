# FTB Quest Nexus

[简体中文](README.md) · **繁體中文** · [English](README.en.md)

面向 **Minecraft Forge 1.20.1** 的 FTB Quests 擴充模組。首個功能為 AE 自動提交器，後續擴充以獨立功能模組加入。

另提供獨立的 **NeoForge 1.21.1** 版本線，原始碼及建置入口在 [`neoforge/`](neoforge/)。兩個版本使用相同模組 ID，分別安裝對應平台 jar，不在同一實例混用。NeoForge 使用外部的 SH-Better-UI NeoForge 版本；根目錄繼續建置 Forge 1.20.1。

## AE 自動提交器

提交器與 ME FTB 任務檢查器均加入原版創造物品欄的「功能方塊」分頁及搜尋目錄，JEI 使用對應目錄收集物品。

方塊 ID：`ftb_quest_nexus:ftbq_ae_submitter`。

1. 將提交器接入有電力與可用頻道的 AE 網路，六個面都可連接。
2. 打開介面點擊「綁定」，綁定當前玩家及其 FTB Teams 隊伍。
3. 在 FTB Quests 任務詳情頁選擇本隊伍的提交器。可一次登記該任務內的消耗型物品及流體目標。只有非消耗型目標的經驗獎勵任務，也可以透過「將任務登記到 Nexus 提交器領取經驗獎勵」加入隊列，完成後領取。
4. 提交器每秒檢查一次，按照 AE 實際扣除的物品數量或散裝流體 mB 增加進度。僅合成物品目標及非消耗型目標交給原有任務流程。
5. 可處理的物品獎勵自動回到 AE 網路，直接發放的經驗點數和等級獎勵發給目前綁定的玩家。綁定玩家離線時，經驗獎勵會等到該玩家上線。「獎勵」切換僅隊列任務／全部任務，新方塊預設僅隊列任務。「提示」只控制本模組自動提交觸發的完成通知。

介面上的「匹配」預設關閉。開啟後，提交器從 AE 庫存匹配已解鎖的消耗型目標，無需手動登記。清單顯示本模式已實際提交過的目標，只拉取這些目標所屬任務的可回網獎勵。關閉後返回手動隊列及原有領獎設置。

支援普通物品獎勵及完全由物品構成的隨機／戰利品嵌套獎勵表；選擇獎勵和帶命令、經驗等非物品結果的表繼續手動領取。空抽會正常記為已領取，不會重新抽取。

可重複任務仍沿用 FTBQ 原生的領獎重置規則；提交器先完成本輪 AE 交付，再恢復其餘原生自動領物品流程，避免隊友先領獎導致本輪 AE 獎勵被重置。審查修復及驗證記錄見 [docs/review-fixes-verification.md](docs/review-fixes-verification.md)。

AE 放不下的獎勵保存在方塊內，優先重試存入 AE。有待交付的獎勵時暫停新提交、領獎和重新綁定。正常採掘掉落會保留綁定、隊列、設定和待交付獎勵。創造模式中鍵選取會取得全新提交器。同隊玩家可點擊「綁定」申請移交，目前綁定者會收到一則私人遊戲訊息，並可在 60 秒內點擊同意或拒絕。綁定者離線時須等對方上線後再申請。領取玩家離隊後暫停任務，移交給原隊伍成員後保留隊列。

合成材料：書、AE 接口、邏輯處理器，按豎列順序放入工作臺。

## ME FTB 任務檢查器

作者為山海／dishanhai，方塊 ID 為 `ftb_quest_nexus:ftbq_ae_checker`，支援 Forge 1.20.1 與 NeoForge 1.21.1。

放置時自動綁定放置者 UUID 及其 FTBQ 隊伍 UUID，六面連接 AE 網路，需要供電與可用頻道。每秒以同一份只讀庫存快照檢查全部已解鎖物品／流體目標。非消耗型目標在庫存達到總需求時自動完成，消耗型目標只顯示庫存，由提交器扣除資源後推進。遵循隊伍鎖定、任務相依套件、順序與重複冷卻，排除僅合成目標。

流體庫存合計 AE 原生流體與物品庫存內流體桶，每桶為 1000 mB；桶物品本身仍可匹配物品目標。流體 NBT／數據組件和物品目標匹配規則分別沿用對應版本 API。全程不取出物品、不排空桶；獎勵繼續走 FTBQ 原生規則。

UI 使用外部 SH-Better-UI 的公開 `UiButton` 與玻璃面板，顯示綁定、網路及目標庫存／總需求，每頁六行，支援按鈕和滾輪翻頁。每位觀察者獨立分頁。已綁定的檢查器由原隊伍成員管理，移交需要目前綁定者在遊戲訊息中同意。綁定玩家離隊後暫停，移交給原隊伍成員後可恢復。世界存檔保留綁定與暫停狀態，挖掘掉落為全新檢查器，重新放置綁定新的放置者。

合成材料為書、AE 接口、運算處理器，按豎列順序放入工作臺。材質採用亮灰白 AE 機殼與紫色指示燈、散熱柵和核心，以點綴色區分提交器；六面在線／離線資源均為原生 16×16 px 幀，動畫包含 8 幀。可編輯生成源位於 `tools/GenerateCheckerTextures.java`，版面示意生成源位於 `tools/CheckerUiPreview.java`。

運行時使用本輪重建、包含公開 `UiButton` API 的 SH-Better-UI 對應平台 jar。用戶端六面顯示、動畫、玻璃模糊、tooltip、鍵盤與滾輪操作驗收記錄於 TODO。

## 安裝與建置

運行環境：**Java 21**，Minecraft 1.20.1，Forge 47.4.16。

本機驗證相依套件：AE2 15.4.10、FTB Quests 2001.4.22、FTB Teams 2001.3.2、FTB Library 2001.2.13、Architectury 9.2.14、GuideME 20.1.15、SH-Better-UI 1.0.3、KotlinForForge 4.12.0。Jade 11.13.3 是可選資訊顯示集成。

**SH-Better-UI 是必需前置**，直接使用桌面 `SH-Better-UI` 專案提供的玻璃面板、模糊、tooltip 和動效，不復制或內嵌其實現。開發建置預設引用 `../SH-Better-UI/build/libs/sh_better_ui-1.0.3-dev.jar`，可用 `-PshBetterUiJar=<dev-jar>` 指定其他位置；UI 專案修改後重新建置其 `devJar` 即可供本專案使用。

游戲中獨立安裝 SH-Better-UI 正式 jar 及其 KotlinForForge 前置。本模組自身不使用 Kotlin，也不將相依套件展開或嵌入模組 jar。其餘開發相依套件已放在 `libs/`，正式運行相依套件的本機副本在 `libs/runtime/`。不需要 GTCEu、GTLCore、GTLAdditions、KubeJS 或 LDLib。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = 'C:\jtmp'
$env:TMP = 'C:\jtmp'
.\gradlew.bat test build reobfJar --no-daemon --console=plain
```

根目錄執行 `gradlew.bat build` 會依序構建 Forge 與 NeoForge。帶有 `reobfJar` 或 `extractNatives` 的命令只在 Forge 執行，包括用 `-x` 排除這些任務。NeoForge 的獨立命令見 [NeoForge 構建說明](neoforge/README.md)。

本機相依套件快取完整時可加 `--offline`。更換機器後需要取得 ForgeGradle、映射及測試相依套件快取。Forge wrapper 使用 Gradle 8.8。

正式產物：`build/libs/ftb_quest_nexus-1.0.0.jar`。不要安裝 `-sources.jar` 或開發用的 UI `-dev.jar`。

獨立伺服器端測試：

```powershell
.\gradlew.bat runGameTestServer --offline --no-daemon --console=plain -x downloadAssets -x extractNatives
```

使用新專案的 `run/` 目錄，不觸碰現有游戲實例。實際執行結果見 `docs/verification.md`，實機待驗內容見 [TODO.md](TODO.md)。

## FTBQ 透明玻璃覆蓋

兩個平台都可通過用戶端 `config/ftb_quest_nexus-client.toml` 切換 FTBQ 外觀，預設保留原版：

```toml
[ftbqUi]
mode = "GLASS"
opacity = 1.0
blurRadius = 8
```

`mode` 可選 `ORIGINAL`／`GLASS`。`opacity` 範圍 0–1，縮放玻璃底色和邊框原有的不透明度（預設材質本身已半透明）；0 不繪製結構玻璃面層。`blurRadius` 為 0–16 的整數 GUI 單位，0 關閉背景模糊。修改後關閉並重新打開對應介面；任務介面打開的子對話框繼承其設定快照。

覆蓋主畫布、章節欄、工具按鈕、任務詳情、槽位、輸入框、捲軸、右鍵菜單及 FTBQ 所屬的設定／選擇視窗。原任務控制項、拖曳、縮放、編輯、提交、領獎及狀態顏色保持；整合包自定義背景、章節圖片、圖標與連線繼續使用原資源。其他模組的 FTB Library 介面不啟用覆蓋。

沿用外部 SH-Better-UI，玻璃根背景取樣後再繪製控制項；模糊不可用時保留透明材質。非零透明度下的庫內細高光，以及 tooltip，繼續使用 UI 庫預設材質。實際畫面和交互仍需用戶端驗收，建置與驗證見 [docs/glass-verification.md](docs/glass-verification.md)。

## 原專案與來源

由 `gt_shanhai:ftbq_ae_submitter` fork，保留其提交與獎勵交付語義、玻璃外觀及動畫資源。原專案程式碼、註冊及配方保留；舊方塊與新方塊使用獨立 ID、隊列、網路通道和節點，不自動遷移舊存檔。並裝時各自有任務登記入口，實際共存操作仍需按驗收單測試。

作者與程式碼來源見 [SOURCE.md](SOURCE.md)。專案名稱和模組 ID 固定為 **FTB Quest Nexus / `ftb_quest_nexus`**。
