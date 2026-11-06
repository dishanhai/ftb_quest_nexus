# FTBQ 玻璃覆盖实施计划

> 按用户授权由 root 执行 Forge／共享资源，neoforge_glass agent 执行 `neoforge/`，共同完成后整合验证；按用户后续要求分模块提交，不部署至游戏实例。

**Goal:** 为 FTBQ 添加默认关闭、可配置的透明玻璃外观。

**Architecture:** FTBQuestsTheme 子类覆盖结构绘制；客户端 Mixin 替换相关 getTheme 返回值及少量直接材质调用；屏幕作用域和配置快照限制覆盖范围。透明度与类名判定策略为无平台依赖的共享 Java。

**Tech Stack:** Java 21、Forge 47.4.16、NeoForge 21.1.265、FTB Quests／Library、SH-Better-UI、JUnit 5。

- [x] 1. 共享策略测试：新增 `client/glass/GlassUiPolicyTest.java`，验证 alpha 缩放端点／色彩保持／非法数值处理，以及 FTBQ／FTBLib 名称边界；先运行 `gradlew.bat test --tests '*GlassUiPolicyTest' --offline --no-daemon --console=plain` 记录未实现红灯，再实现 `GlassUiPolicy.scaleAlpha/isQuestScreenClass/isLibraryScreenClass` 并复验。
- [x] 2. Forge 配置与作用域：新增 `config/ClientUiConfig.java` 和 `client/glass/QuestGlassUi.java`，注册 CLIENT spec，配置名和范围见设计；按 BaseScreen 保存弱引用快照，沿 `getPrevScreen()` 检查 FTBQ 父界面，仅允许 FTBQ／FTBLib 对话框。更新 `QuestNexus` 注册与 `NexusClient` 模糊／tooltip 白名单。
- [x] 3. Forge 绘制：新增 `QuestGlassTheme.java`，以 `GlassTheme.GLASS` 的调色和 box 几何覆盖结构控件，使用 `GlassBlur.backdrop` 在根底板采样一次。以真实 jar 的 javap 核验各 getTheme 与直接 Icon／ColorProperty 调用后新增客户端 Mixin；主屏 drawBackground 不可整体取消，保留其拖动／框选逻辑。
- [x] 4. NeoForge 并行适配：agent 仅改 `neoforge/`，接入同名配置和对应客户端 hook；共享 root 的纯策略、语言、图片，使用 2101 的实际章节与任务详情绘制入口。
- [x] 5. 共享资源与文档：向中英语言加入 `config.ftb_quest_nexus.ftbq_ui` 和 `.mode/.opacity/.blur_radius`；README 写配置用法，TODO 记录未实机验收；错误修正只写 `.learnings/ERRORS.md`。
- [x] 6. 整合验证：Forge `test build reobfJar`，NeoForge `test build`，检查测试 XML、JSON 语言键及真实依赖 jar 的 Mixin 注入点契约；分别 `runGameTestServer` 验证 dedicated server 隔离。对修改执行空白检查，复核未嵌入 UI／依赖类和测试夹具。客户端 Mixin 实际变换和画面交互验收仍在 TODO 中跟踪。
