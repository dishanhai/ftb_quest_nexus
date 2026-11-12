# FTB Quest Nexus 待办

- [ ] 山海／dishanhai：完成 Minecraft 26.1.2 / NeoForge 26.1.2.126 移植。独立构建目录为 `neoforge-26.1.2/`，使用 Java 25。
  - SH-Better-UI `1.0.7-alpha.1` 已有共享内核、搜索索引与图标资源。等 26.1.2 提供 Nexus 使用的搜索输入框、控件、主题、模糊和图标绘制 API 后继续联调。
  - 已接入目标 FTB / AE2 依赖，处理方块、存档、封包、化学品事务与配方的版本差异，迁移新版绘制与输入入口及 FTB Mixin 目标。26.1.2 资源处理和独立的 25 项单测通过。
  - 已注册三项原生 GameTest（提交器存档、掉落和创造复制、检查器能量接口与配方）。待 SHBUI 前置补齐后完成全量编译、运行这三项测试、迁移其余回归用例并验收客户端／专用服务端。
  - 重点验收检查器 FE 提交的事务回滚、提交器掉落保留用户数据与创造复制清除绑定、奖励账本重载，以及 FTB 任务书和 SHBUI 搜索补全。

- [ ] 山海／dishanhai：双平台验收方块外观。分别为提交器和检查器选择两套主题，检查面板、文字、按钮和补全浮层，重开界面并重启确认选项独立保存。验证跟随 SHBUI 主题、资源包主题移除及恢复、取消、键盘和 GUI 缩放。Forge 另核对通量网络选择器继承检查器主题。
  - NeoForge 1.21.1 隔离客户端已对照主控与 SHBUI 的小窗口边界，验证圆角选择器、键盘正反切换、独立保存与重启保留。临时世界内分别打开烟灰玻璃提交器和 Slate 检查器。
  - Forge 1.20.1 隔离客户端两次在进入模组配置前触发 GLFW 原生崩溃，界面仍待验收。对应日志为 `build/nexus-block-theme-smoke-final.log` 和 `build/nexus-block-theme-smoke-retry.log`。

- [ ] 山海／dishanhai：双平台实机验收客户端配置主控。在模组列表打开配置，验证原版／玻璃切换、滑杆键盘操作和 GUI 缩放，应用后重开任务书并重启确认配置留存。验证取消与 Esc，以及提交器在线／离线图标切换。
  - NeoForge 1.21.1 隔离客户端已通过模组列表入口、标题与提示清晰度、模式切换、应用按钮和 TOML 写入检查。
  - 功能页已在 NeoForge 隔离客户端验证分类切换、两项开关反馈及应用后的 TOML 写入。待在 Forge 客户端核对界面，并在两端任务书与机器界面验证入口显隐、关闭补全后的常规筛选、键盘焦点及 GUI 缩放。

- [ ] 山海／dishanhai：双平台实机验收提交器与检查器的 SH-Better-UI 图标。核对奖励按钮、提交器标题的队列图标、任务状态筛选，以及缺失图标资源时的回退显示。测试不同 GUI 缩放与语言下的窄版筛选文字、搜索栏补全、功能提示和键盘旁白，确认暂停／继续及完成提示图标在服务器同步后切换。Forge 另验收通量网络选择器的翻页、连接、断开和返回。

- [ ] 山海／dishanhai：双平台实机验收本轮审查修复。用双人队伍测试 AE 满盘后的多项自动奖励、存档重载及恢复回网；在 NeoForge 先放检查器，再从各方向接线。核对任务页提交器列表跨页、关闭或切换任务时的旧响应、经验任务单位和 GUI 缩放。另在隔离存档测试临时移除并装回化学品依赖，以及原版／玻璃主题下的化学品选择器。

- [ ] 山海／dishanhai：在 GTL 八周目用 Flux Networks 7.2.1.15 验收检查器内的网络选择。只放检查器，在菜单中连接通量网络，核对 FE 扣除与能量任务进度、权限、密码、暂停、断开和存档重载。

- [ ] 山海／dishanhai：双平台实机核对全表奖励的每项数量和 AE 满盘重试。用两名玩家验证只有绑定玩家收到可点击的自选奖励消息，点击后在同页选择多个任务的奖励，确认物品交付、再次打开剩余奖励及默认／玻璃主题下的操作。

- [ ] 山海／dishanhai：实测第一批性能优化。用同一存档分别测单台／多台、开关菜单，记录 P95／P99 MSPT、AE 全库存枚举次数及每位玩家的封包量。客户端同时确认检查器开窗后在下一轮扫描显示目录，两名玩家同时开窗时关闭其中一个仍继续刷新。

- [ ] 山海／dishanhai：第二批性能优化双平台实机验收。核对检查器首次目录、单行变更、增删／重排、关闭重开和重同步。测试不同搜索词与库存／完成筛选组合，编辑并重载任务后核对索引，反复触发可重复奖励并检查 AE 满盘行为。用同一存档对比改动前后的每位玩家封包数、字节数及服务端 P95／P99 MSPT。

- [x] 山海／dishanhai：提交器新增默认关闭的「匹配」模式，自动扣除 AE 中已解锁目标需要的物品及散装流体，仅为匹配提交过的任务领取支持回网的物品奖励。手动登记也支持流体，检查器仅自动完成非消耗型目标。双平台各 45 项服务端 GameTest 和打包通过。
- [ ] 山海／dishanhai：在隔离整合包存档中，用新建或重置的消耗型目标实机核对 AE 终端前后数量、流体 mB、手动／匹配切换、满盘奖励回网及与独立 ae2_ftbquest_detector 同时启用的行为。

- [ ] 山海／dishanhai：双平台客户端复测提交器和检查器的补全层。输入可命中的短词，将指针移到补全框内外及下方的状态、过滤和翻页控件，检查仅长候选可显示提示，Esc 收起后恢复普通提示。另测 Modern UI、上下翻转、小窗口、滚轮和调整 GUI 缩放后继续输入。

- [x] 山海／dishanhai：提交器队列任务搜索接入 SH-Better-UI SearchIndex／SearchField，按整队列的本地化任务／目标名称与任务／物品 ID 建立索引，复用灰字预测、上下键、Tab／点击补全、Esc 收起和 Ctrl+Z 撤销。服务端按队列快照验证命中位置，各玩家独立筛选与分页，清空恢复全队列。
  - 验证：Forge 34 项／NeoForge 40 项单测、两平台各 43 项 GameTest、打包及 jar 隔离检查通过。Forge 正式 jar 与 reobf 输出 SHA-256 一致。最终日志为 build/submitter-search-forge-final.log、build/submitter-search-forge-package.log 与 build/submitter-search-neoforge-final.log。
- [ ] 山海／dishanhai：提交器搜索客户端实机验收。检查中文／英文任务名、跨页命中、空结果、补全列表遮挡与点击／滚轮优先级、Esc、Tab、Ctrl+Z、输入物品栏快捷键、队列变动时清空搜索、名称／语言刷新及 GUI 缩放。

- [x] 山海／dishanhai：检查器流体桶任务可从 AE 散装流体折算整桶，与匹配的实体桶合计。检查器任务搜索接入 SH-Better-UI SearchField／SearchIndex，按完整目录搜索，已完成／未完成与当前库存大于 0 可组合筛选，A–Z 独立排序，先筛选再分页。
  - 验证：Forge 35 项／NeoForge 41 项单测、两平台各 43 项服务端 GameTest、正式打包和依赖隔离通过。Forge 正式 jar 与 reobf 输出 SHA-256 一致。日志见 `build/checker-search-final-forge.log` 与 `neoforge/build/checker-search-final-neoforge.log`。
- [ ] 山海／dishanhai：检查器客户端验收。核对 JDT Extras 生命流体桶、多桶门槛、实体桶混存、库存只读及完成提示。检查跨页搜索、Tab／点击补全、状态与库存组合、A–Z 排序、小窗口分页和 GUI 缩放。
- [x] 山海／dishanhai：检查器接入 SH-Better-UI IngredientFilter 的全部类型／物品／流体条件，与任务搜索、补全和其他筛选组合。按 FTBQ 目标类型分类，流体桶属于物品。
- [ ] 山海／dishanhai：实机核对类型按钮在不同 GUI 缩放下的显示，以及切换时搜索结果、补全、页码和流体桶分类。

- [x] 山海／dishanhai：提交器新增与检查器一致的六行队列列表、目标标题、已提交／总需求、完成高亮、独立分页、按钮和滚轮翻页。保留绑定／清队列／提示／奖励操作，双平台新增队列边界与旧存档失效目标回归。
- [ ] 山海／dishanhai：提交器队列客户端验收。核对空队列、多页队列、部分／全部提交、可重复任务、两名玩家独立翻页、清空后页码、长标题／UUID 悬停、按钮及 GUI 缩放。当前验证为双平台构建、单测与服务端 GameTest。

- [x] 山海／dishanhai：修复指令可获取但 NeoForge 创造栏与 JEI 目录未显示的物品。两平台通过 BuildCreativeModeTabContentsEvent 将提交器／检查器加入原版「功能方块」，可见性为 PARENT_AND_SEARCH_TABS。新增普通玩家／OP 两项原生目录重建回归，覆盖分栏、全局搜索、JEI 使用的目录及去重；NeoForge 旧代码红灯，修复后双平台各 37 项 GameTest 全通过，Forge 34 项／NeoForge 39 项 JUnit 通过，正式打包已核对。日志为 `build/creative-visibility-red-neoforge.log`、`build/creative-visibility-forge-final.log`、`build/creative-visibility-neoforge-final2.log`。
- [ ] 山海／dishanhai：客户端确认提交器／检查器在创造「功能方块」、创造搜索及 JEI 的 `@ftb_quest_nexus` 搜索中可见，并在不同 GUI 缩放下对比检查器亮银外壳、紫色徽记和提交器图标。已验证原生目录构建、JEI 19.57.0.451 的目录读取和 16 px 等轴缩图，客户端画面待验收。

- [x] 山海／dishanhai：按用户反馈增强检查器与提交器的材质辨识。检查器六面采用清晰紫色点缀，覆盖在线／离线指示、散热栅与核心，保持每组 8 个原生 16×16 px 帧。新增六面每帧紫色辨识回归，旧资源红灯、新资源绿灯；双平台各 5 项材质测试、Forge 34 项／NeoForge 39 项全量单测及 build 通过，两个 jar 内各 22 个材质／动画文件与源文件逐字节一致，Forge 发布包与 reobf 输出 SHA-256 一致。日志为 `build/checker-accent-red.log` 与 `build/checker-accent-verification.log`，预览已更新，客户端验收仍列于下方。
- [x] 山海／dishanhai：检查器保留亮银外壳，正面和顶部加浅色勾号及饱和紫色面板，侧面加紫色双横条。原生 16×16 px、8 帧；六面每帧校验亮色面积和紫色点缀面积。Forge 35 项／NeoForge 41 项单测与双平台打包通过，两包各 22 个材质及动画文件与源码一致。日志为 `build/checker-jei-contrast-forge.log` 与 `neoforge/build/checker-jei-contrast-neoforge.log`。

- [x] 山海／dishanhai：新增「ME FTB 任务检查器」。放置时绑定玩家与队伍 UUID，自动扫描已解锁物品／流体目标，只读库存达标后完成，流体合计 AE 原生储量与物品内流体桶，每桶 1000 mB。
  - [x] 库存快照、流体桶换算、不累加进度、精确流体 NBT／components、溢出、依赖／顺序守卫及完整 131 目标扫描回归。
  - [x] 双平台方块、AE 节点、世界存档持久化、放置绑定及原队伍权限，离队暂停与重新绑定回归。
  - [x] SH-Better-UI 公开 UiButton、服务端每位观察者独立六行分页与双语言文本；客户端根据目标 ID 解析 FTBQ 自定义标题，服务端不调用 OnlyIn(CLIENT) 标题方法。
  - [x] 参考图风格六面 16×16 px 帧序列，每组 8 帧；在线／离线模型、材质动画预览及标明非游戏截图的布局示意。
  - [x] Forge 33 项／NeoForge 38 项单测、双平台各 33 项服务端 GameTest、build、Forge reobfJar 与 NeoForge verifyModJar 通过。13 项新增 GameTest 包含真实 AE 存储服务路由，仅网络供电状态在该回归夹具中旁路。最终日志为 `build/checker-verification-final-forge.log` 与 `build/checker-verification-final-neoforge.log`。
  - [x] 两包均包含检查器类、配方、掉落与 16 px 动画资源，未混入 GameTest／AE／FTBQ／SH-Better-UI 类。Forge 发布包与 reobf 输出 SHA-256 一致，公开 UiButton 的外置 UI 对应平台 jar 已重建。
- [ ] 山海／dishanhai：检查器客户端实机验收。实际 AE 供电／频道与断网恢复、不同物品过滤器与流体桶、重复任务冷却／原生奖励、玩家离队重绑、六向放置／相邻遮挡／在线离线动画／资源重载，以及 SH-Better-UI 控件、键盘、滚轮、UUID／目标 tooltip、Modern UI 共存与 GUI 缩放。当前 UI 预览为布局示意，材质预览由原始帧生成。

- [x] 山海／dishanhai：完成自动提交器亮色 AE 材质与 tooltip 重构。11 张 PNG 均为 16×256 px，每帧 16×16 px、共 16 帧、每帧 4 tick，固定亮灰白机壳，仅在线指示与传输符号变化。六面模型共用几何，离线关闭自发光；八行物品提示及四个按钮说明存于双语言文件，自有像素源为 `tools/GenerateSubmitterTextures.java`。
- [x] 山海／dishanhai：材质重构验证。Forge 29 项／NeoForge 34 项单测通过，双平台 build、NeoForge verifyModJar、Forge 独立 reobfJar 通过。两包各 22 个材质／动画文件与源码逐字节一致，64 个中英语言键齐全，发光元数据分别为 forge_data／neoforge_data；Forge 发布 jar 与 reobf 输出 SHA-256 相同。日志为 `build/submitter-assets-verification.log`。
- [ ] 山海／dishanhai：新材质客户端验收。检查六向放置、相邻方块遮挡、离线／在线切换、暗处仅前面指示自发光、16 帧动画与资源重载、物品八行提示及四个按钮说明的换行／GUI 缩放／Modern UI 共存。当前证据为资源检查、单测及构建，客户端仍待验收。

- [ ] 山海／dishanhai：实机验收 tooltip 优先级修复。启用 Modern UI 增强提示，分别悬停提交器的四个按钮、信息行及 JEI 多行彩色物品提示，检查文字／图片清晰、只有一层提示、GUI scale 和屏幕边缘定位；Forge／NeoForge 都需客户端验收。注册顺序回归与打包已通过。

- [x] 修复跨队重绑、多目标登记和自动奖励回 AE 的执行顺序；Forge 24 项／NeoForge 29 项单测、双平台各 20 项服务端 GameTest 与正式构建通过，见 `docs/review-fixes-verification.md`。

- [ ] 三项审查修复实机验收：跨队拒绝重绑、原队伍成员接替离队领取玩家、多物品目标一次登记、自动物品奖励回 AE（含满盘和可重复任务）。服务端回归与打包结果见 `docs/review-fixes-verification.md`。

- [x] 新增双平台可配置 FTBQ 玻璃主题覆盖与局部结构绘制适配，默认保留原版；客户端配置和使用方法见 README。
- [ ] FTBQ 玻璃覆盖实机验收：原版／玻璃往返、同一界面实例关闭重开读取配置、透明度 0／1、模糊 0／16、章节展开、详情翻页、任务节点拖曳／缩放／框选、编辑器、奖励选择、右键菜单、配置／物品选择窗口、tooltip／GUI 缩放及资源重载；核对其他模组 FTB Library 画面保持原样。

- [x] 云端交付：已按构建配置、共用逻辑、Forge、NeoForge、文件记录完成五批提交；用户创建远端仓库 https://github.com/dishanhai/ftb_quest_nexus，已核对远端 main 包含全部五批提交，与本地提交 `2710fbae8f1e439727021d080c11b051dc0f45d9` 一致。

- [x] 新增独立 NeoForge 1.21.1 版本及外置 SH-Better-UI 对应产物；21 项 Nexus 单测、109 项 UI 单测、11 项服务端 GameTest 通过，原 Forge 构建回归成功。详见 `docs/neoforge-verification.md`，平台实机待验见 `neoforge/TODO.md`。

- [x] wrapper 构建／reobfJar、17 项 JUnit、11 项服务端 GameTest 全通过；正式产物与 reobf 输出 SHA-256 一致，详见 `docs/verification.md`。
- [x] 玻璃 UI 使用独立 SH-Better-UI 项目作为必需前置；开发依赖直接引用该项目 devJar，正式包不嵌入 UI 库。
- [ ] 实机验收玻璃模糊、按钮反馈、长 UUID 悬停滚动、不同 GUI 缩放及六向方块贴图。
- [ ] 实机验收任务页登记、多个同队提交器选择、跨队拒绝、离队重绑、精确／忽略 NBT 与过滤器目标。
- [ ] 实机验收真实 AE 网络的满盘、断网、卸载重载、服务端重启、正常采掘与重新放置、奖励模式切换及随机表空抽。
- [ ] 山海／dishanhai：Forge 与 NeoForge 客户端验收提交器的线缆接头和经验奖励。分别从六面右键接上 AE 线缆，确认接头渲染与上线状态。使用旧存档中已绑定的提交器，从任务详情登记只有勾选目标的任务，核对经验点数、等级及同队重新绑定后的领取玩家。
- [ ] 山海／dishanhai：两客户端验收提交器及检查器的绑定移交。确认原绑定者可从游戏聊天点击同意或拒绝，其他玩家看不到请求，过期后须重新申请，并核对移交后的 UUID 与经验奖励归属。
- [ ] 与原 `gt_shanhai` 并装验收两个任务页入口布局和各自队列行为；Jade 有／无两种运行环境分别验收。

不将源码、单元测试或 GameTest 通过等同于 Minecraft 客户端实机验收。源码已同步远端仓库；未部署、未重启既有游戏实例，未发布发行版本。
