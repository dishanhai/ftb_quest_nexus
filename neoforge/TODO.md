# NeoForge 1.21.1 移植驗收

作者：dishanhai。獨立版本線，保留根目錄 Forge 1.20.1。

- [x] 用戶確認 Minecraft 1.21.1，SH-Better-UI 保持外部前置。
- [x] 先建立 payload 編解碼、數量限制與不可變清單測試。
- [x] 固定 NeoForge／AE2／FTB／KotlinForForge 相容依賴。
- [x] SH-Better-UI NeoForge 109 項測試與正式 jar 建置。
- [x] Nexus NeoForge 21 項單元測試與正式 jar 建置。
- [x] 專用伺服器 11 項 GameTest：保存／掉落／創造複製／節點／任務／獎勵全部通過。
- [x] 原有 Forge 1.20.1 建置回歸。
- [x] jar 內容與 SHA256 驗證，排除測試類及相依模組類別。
- [ ] 客戶端玻璃／模糊／tooltip／GUI scale／資源重載實機驗收。
- [ ] 完整有線 AE 網路、FTBQ 任務登記、隊伍變更與重啟存檔流程實機驗收。
- [ ] Jade 客戶端顯示與安裝／未安裝場景驗收；目前已核對 15.10.6 API，伺服器測試採未裝場景。

開發執行目錄固定為本子專案 `run/`。不部署至現有整合包，不將伺服器測試當成玻璃外觀證明。
