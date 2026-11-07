# NeoForge 1.21.1 移植驗收

作者：dishanhai。獨立版本線，保留根目錄 Forge 1.20.1。

- [x] 三项审查修复完成：29 项 JUnit、20 项服务端 GameTest、正式构建与 verifyModJar 通过；范围与产物见 `../docs/review-fixes-verification.md`。

- [ ] 审查修复实机验收：跨队拒绝重绑、原队伍成员接替离队玩家、多物品目标一次登记、自动物品奖励回 AE（含满盘、可重复任务与队友个人奖励）。见 `../docs/review-fixes-verification.md`。

- [x] FTBQ 玻璃覆盖基础验证：客户端配置与主题适配编译成功；29 项 JUnit（含共享策略与实际 FTBQ／FTB Library 字节码契约）通过，`verifyModJar` 通过，独立专用服务器 11 项 GameTest 通过。

- [ ] FTBQ 玻璃覆盖实机验收：原版／玻璃配置切换及重开生效、透明度、0／默认／最大模糊半径；确认无关 FTB Library 界面保持原样。
- [ ] FTBQ 全界面实机验收：章节展开、画布拖拽／缩放／框选、自定义图片、任务详情／计时／状态、任务登记、奖励选择、编辑器、弹窗、搜索／滚动条、tooltip 与不同 GUI 缩放。
- [ ] SH-Better-UI tooltip 使用库内固定玻璃材质，当前不跟随 FTBQ 的 `opacity`／`blurRadius` 参数；客户端验收需确认该差异可接受。

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
