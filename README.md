# FTB Quest Nexus

面向 **Minecraft Forge 1.20.1** 的 FTB Quests 扩展模组。首个功能为 AE 自动提交器，后续扩展以独立功能模块加入。

另提供独立的 **NeoForge 1.21.1** 版本线，源码及构建入口在 [`neoforge/`](neoforge/)。两个版本使用相同模组 ID，分别安装对应平台 jar，不在同一实例混用。NeoForge 使用外置的 SH-Better-UI NeoForge 版本；根目录继续构建 Forge 1.20.1。

## AE 自动提交器

方块 ID：`ftb_quest_nexus:ftbq_ae_submitter`。

1. 将提交器接入有电力与可用频道的 AE 网络，六个面都可连接。
2. 打开界面点击「绑定」，绑定当前玩家及其 FTB Teams 队伍。
3. 打开 FTB Quests 中可消耗的物品任务，在详情页点击「登记到 Nexus 提交器」，选择本队伍的提交器。
4. 提交器每秒检查一次，按任务要求从 AE 网络真实扣料并增加进度。不会处理仅合成任务或不消耗物品的目标。
5. 物品奖励自动回到 AE 网络；「奖励」切换仅队列任务／全部任务，新方块默认仅队列任务。「提示」只控制本模组自动提交触发的完成通知。

支持普通物品奖励及完全由物品构成的随机／战利品嵌套奖励表；选择奖励和带命令、经验等非物品结果的表继续手动领取。空抽会正常记为已领取，不会重新抽取。

AE 放不下的奖励保存在方块内，优先重试回网；有待交付余量时暂停新提交、领奖和重新绑定。正常采掘掉落会保留绑定、队列、设置和余量；创造模式中键选取生成全新提交器。领取玩家离队后暂停任务，需本队伍玩家重新绑定。

合成材料：书、AE 接口、逻辑处理器，按竖列顺序放入工作台。

## 安装与构建

运行环境：**Java 21**，Minecraft 1.20.1，Forge 47.4.16。

本机验证依赖：AE2 15.4.10、FTB Quests 2001.4.22、FTB Teams 2001.3.2、FTB Library 2001.2.13、Architectury 9.2.14、GuideME 20.1.15、SH-Better-UI 1.0.3、KotlinForForge 4.12.0。Jade 11.13.3 是可选信息显示集成。

**SH-Better-UI 是必需前置**，直接使用桌面 `SH-Better-UI` 项目提供的玻璃面板、模糊、tooltip 和动效，不复制或内嵌其实现。开发构建默认引用 `../SH-Better-UI/build/libs/sh_better_ui-1.0.3-dev.jar`，可用 `-PshBetterUiJar=<dev-jar>` 指定其他位置；UI 项目修改后重新构建其 `devJar` 即可供本项目使用。

游戏中独立安装 SH-Better-UI 正式 jar 及其 KotlinForForge 前置。本模组自身不使用 Kotlin，也不将依赖展开或嵌入模组 jar。其余开发依赖已放在 `libs/`，正式运行依赖的本机副本在 `libs/runtime/`。不需要 GTCEu、GTLCore、GTLAdditions、KubeJS 或 LDLib。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = 'C:\jtmp'
$env:TMP = 'C:\jtmp'
.\gradlew.bat test build reobfJar --no-daemon --console=plain
```

本机依赖缓存完整时可加 `--offline`。更换机器后需要取得 ForgeGradle、映射及测试依赖缓存；wrapper 会使用 Gradle 8.8。

正式产物：`build/libs/ftb_quest_nexus-1.0.0.jar`。不要安装 `-sources.jar` 或开发用的 UI `-dev.jar`。

独立服务端测试：

```powershell
.\gradlew.bat runGameTestServer --offline --no-daemon --console=plain -x downloadAssets -x extractNatives
```

使用新项目的 `run/` 目录，不触碰现有游戏实例。实际执行结果见 `docs/verification.md`，实机待验内容见 [TODO.md](TODO.md)。

## 原项目与来源

由 `gt_shanhai:ftbq_ae_submitter` fork，保留其提交与奖励交付语义、玻璃外观及动画资源。原项目代码、注册及配方保留；旧方块与新方块使用独立 ID、队列、网络通道和节点，不自动迁移旧存档。并装时各自有任务登记入口，实际共存操作仍需按验收单测试。

作者与代码来源见 [SOURCE.md](SOURCE.md)。项目名称和模组 ID 固定为 **FTB Quest Nexus / `ftb_quest_nexus`**。
