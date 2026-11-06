# FTB Quest Nexus 待办

- [x] 新增双平台可配置 FTBQ 玻璃主题覆盖与局部结构绘制适配，默认保留原版；客户端配置和使用方法见 README。
- [ ] FTBQ 玻璃覆盖实机验收：原版／玻璃往返、同一界面实例关闭重开读取配置、透明度 0／1、模糊 0／16、章节展开、详情翻页、任务节点拖曳／缩放／框选、编辑器、奖励选择、右键菜单、配置／物品选择窗口、tooltip／GUI 缩放及资源重载；核对其他模组 FTB Library 画面保持原样。

- [x] 云端交付：已按构建配置、共用逻辑、Forge、NeoForge、文件记录完成五批提交；用户创建远端仓库 https://github.com/dishanhai/ftb_quest_nexus，已核对远端 main 包含全部五批提交，与本地提交 `2710fbae8f1e439727021d080c11b051dc0f45d9` 一致。

- [x] 新增独立 NeoForge 1.21.1 版本及外置 SH-Better-UI 对应产物；21 项 Nexus 单测、109 项 UI 单测、11 项服务端 GameTest 通过，原 Forge 构建回归成功。详见 `docs/neoforge-verification.md`，平台实机待验见 `neoforge/TODO.md`。

- [x] wrapper 构建／reobfJar、17 项 JUnit、11 项服务端 GameTest 全通过；正式产物与 reobf 输出 SHA-256 一致，详见 `docs/verification.md`。
- [x] 玻璃 UI 使用独立 SH-Better-UI 项目作为必需前置；开发依赖直接引用该项目 devJar，正式包不嵌入 UI 库。
- [ ] 实机验收玻璃模糊、按钮反馈、长 UUID 悬停滚动、不同 GUI 缩放及六向方块贴图。
- [ ] 实机验收任务页登记、多个同队提交器选择、跨队拒绝、离队重绑、精确／忽略 NBT 与过滤器目标。
- [ ] 实机验收真实 AE 网络的满盘、断网、卸载重载、服务端重启、正常采掘与重新放置、奖励模式切换及随机表空抽。
- [ ] 与原 `gt_shanhai` 并装验收两个任务页入口布局和各自队列行为；Jade 有／无两种运行环境分别验收。

不将源码、单元测试或 GameTest 通过等同于 Minecraft 客户端实机验收。源码已同步远端仓库；未部署、未重启既有游戏实例，未发布发行版本。
