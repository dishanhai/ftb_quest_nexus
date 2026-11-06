# FTB Quest Nexus 1.0.0 验证记录

作者 dishanhai；本次验证日期按会话工作日记为 2026-11-07。

## 结果

- 项目目录与本地 Git 仓库：`C:\Users\dishanhai\Desktop\ftb_quest_nexus`。尚无提交，无远端。
- `gradlew.bat test build reobfJar --offline --no-daemon --console=plain`：成功，最终 wrapper 执行退出码 0。见 `build/final-build.log`。
- JUnit：4 个测试类，17 项，0 failure／error／skip。见 `build/test-results/test/`；UI 开发依赖改为直接引用独立项目后 Gradle 确认相同测试输入，测试任务 UP-TO-DATE。
- 独立服务端 GameTest：11 项必需测试全部通过，退出码 0，见 `build/gametest-verified.log` 的 `All 11 required tests passed`。
- 服务端加载的本模组、AE2、FTBQ、FTB Teams、FTB Library、Architectury、GuideME、SH-Better-UI、KotlinForForge 均正常初始化，无 GTCEu、GTL 或 KubeJS。
- 两轮只读源码审查发现的实质问题均已修正；最终复核无未解决实质问题。源码审查不替代客户端验收。

## 测试覆盖与红绿证据

- 单元：奖励共享容量、满盘余量、长整型、恢复快照、插入异常、无效数量；unsigned 队列 ID、坏数据、去重、清空、不可变快照；提示抑制嵌套／异常清理；任务页顶栏避让几何。
- 服务端：用户数据／队列／奖励 NBT 往返、新机默认奖励模式、真实 loot 保存余量并排除节点身份、普通与服务端创造复制、恢复 UUID 碰撞、AE 六面节点创建／销毁、恢复领取玩家的节点归属。
- FTBQ 算法：真实 ItemTask／TeamData 的需求数量扣料及进度、不消耗或锁定时拒绝、真实 ItemReward 的满盘缓冲／领取防重／部分容量及重试；存储适配器为 GameTest 内存实现，并非完整真实 AE 线缆网络。
- 两个 P1 缺陷先在 `build/gametest-defects-red.log` 失败：服务端 Ctrl-pick 回填复制奖励、物品 NBT 恢复后 UUID 索引失配；修复后全部通过。

## UI 前置

- **SH-Better-UI 项目作为必需外部前置提供玻璃 UI**。开发依赖实际路径为 `../SH-Better-UI/build/libs/sh_better_ui-1.0.3-dev.jar`，可通过 `-PshBetterUiJar` 覆盖。
- Screen 复用该库的 GlassTheme、GlassBlur、GaussBlur、GlassTooltip、Spring／GuiMotion；没有复制玻璃库源码、着色器或类。
- 该开发 jar 核验 SHA-256：`17779F3436C36F9A497DD97AB4D29F20B1EF132455BDAD099AADFF9089D750A9`。
- 游戏需安装独立 SH-Better-UI 正式包和它要求的 KotlinForForge；本模组不嵌入前置。

## 正式产物

- 文件：`build/libs/ftb_quest_nexus-1.0.0.jar`。
- 大小：83,352 bytes，82 个 ZIP 条目。
- SHA-256：`1F6F540A451DC844EED968C914A363FFF30B3E8D16B01E486A6A7BE2EA09B0E7`。
- `build/reobfJar/output.jar` 与正式产物 SHA-256 完全一致。
- MANIFEST 的 `MixinConfigs` 指向 `ftb_quest_nexus.mixins.json`；mods.toml 已展开为正确的 ID、名称及 1.0.0 版本。
- 正式 jar 不含依赖 jar、Kotlin 类、SH-Better-UI 类、旧 gt_shanhai 类、GT／LDLib 类或 GameTest 类。测试类与空模板仅留在开发运行／源码中。
- 11 个 JSON 资源均可解析；双语键集合一致，共 52 键。原提交器的 PNG／mcmeta 资源保留，六向 × 在线状态共 12 种模型变体。

## 验证边界

保留 Forge 1.20.1 API 的 3 条弃用警告及 Gradle 8.8 插件的 Gradle 9 兼容提示，不影响当前成功构建。没有为消除警告升级框架。

尚未进行 Minecraft 客户端玻璃视觉、完整真实 AE 网络任务链、原模组并装、Jade 客户端显示、跨团队／离队交互及游戏存档重启验收；详细清单在 TODO.md。

未部署 jar、未重启既有游戏实例、未提交或推送任何仓库。原 `gt_shanhai` 功能源码／资源保留；本轮只在其本机方案／待办／错误记录中跟踪移植。
