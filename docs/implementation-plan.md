# FTB Quest Nexus Implementation Plan

**Goal:** fork 原 FTBQ AE 自动提交器为独立 Forge 1.20.1 模组；项目身份采用用户确定的 `ftb_quest_nexus`。
**Architecture:** Forge 原生 Block/BlockEntity/Menu 承载持久化和 AE 节点，客户端 Screen 复用独立玻璃库。复用原任务与奖励算法，网络只提交意图并在服务端验证队伍归属。
**Tech Stack:** Java 21、Forge 47.4.16、Gradle 8.8、AE2 15.4.10、FTBQ 2001.4.22、JUnit 5、SH-Better-UI 1.0.3。

用户已授权本地创建和实现；本次不提交 Git、不推送、不部署或重启现有游戏实例。各实施单元完成后在当前会话整合。

## 文件与任务

- [ ] 建置与资源：`build.gradle`、`settings.gradle`、wrapper、`NexusContent`、`SubmitterBlock`、`SubmitterMenu`、双语资源、模型、六面纹理、配方、掉落表。沿用本机可用依赖 jar，独立复制所需文件，不从原目录打包全部 libs。
- [ ] 测试先行：迁移 `FtbqRewardBufferTest`，先运行缺少被测类的红灯，复制原奖励缓冲与提示抑制工具；新增 `TaskQueueTest` 覆盖 unsigned ID、持久化去重、无效 ID 和清空，再实现 `TaskQueue`。`gradle test` 应全部通过。
- [ ] 核心：`submitter/SubmitterBlockEntity.java` 实现 AE 节点、队伍鉴权、提交、领奖与待交付奖励持久化；复用原物品过滤／奖励树算法，所有可见文本转成翻译键。
- [ ] 客户端与协议：`client/SubmitterScreen.java`、登记菜单和三个队列数据包；界面操作使用原版菜单按钮协议，服务端验证菜单、距离和队伍；两个 FTBQ Mixin 只负责登记入口和局部 toast 抑制。Jade 为可选插件。
- [ ] 整合：实际 jar 的 `javap` 核验 AE、FTBQ、Jade 和玻璃库 API；执行 `compileJava test build reobfJar`，按实际错误做最小修复。
- [ ] 服务端：使用独立运行目录启动 GameTest，验证没有 GT/GTL/KubeJS 时的类加载、方块注册、队列／余量往返和 AE 节点；不使用用户游戏目录。
- [ ] 交付：检查 jar 内容、Mixin 清单、资源依赖边界和 SHA-256，写 README、SOURCE.md、TODO.md，明确未完成的实机视觉和多模组共存验收。

核心接口约定：`QuestNexus.MOD_ID`；注册表 `NexusContent.SUBMITTER`、`BLOCK_ENTITY`、`MENU`；方块实体 `SubmitterBlockEntity` 提供 `getMainNode()`、`serverTick()`、`applyAction(ServerPlayer,int)`、`writeClientState()`、`readClientState(CompoundTag)`、`listSubmitters(ServerPlayer)`、`queueTask(ServerPlayer,UUID,long)` 和 `SubmitterEntry`。界面读取绑定、状态和计数 getters。服务端与客户端类引用隔离。

验证命令：`gradlew.bat test build reobfJar --offline --no-daemon --console=plain`；服务端 `gradlew.bat runGameTestServer --offline --no-daemon --console=plain -x downloadAssets -x extractNatives`。首次环境配置如需网络，在不修改现有依赖版本的前提下获取必要缓存。
