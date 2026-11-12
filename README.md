# FTB Quest Nexus

**简体中文** · [繁體中文](README.zh-TW.md) · [English](README.en.md)

面向 **Minecraft Forge 1.20.1** 的 FTB Quests 扩展模组。首个功能为 AE 自动提交器，后续扩展以独立功能模块加入。

另提供独立的 **NeoForge 1.21.1** 版本线，源码及构建入口在 [`neoforge/`](neoforge/)。两个版本使用相同模组 ID，分别安装对应平台 jar，不在同一实例混用。NeoForge 使用外置的 SH-Better-UI NeoForge 版本；根目录继续构建 Forge 1.20.1。

## AE 自动提交器

提交器与 ME FTB 任务检查器均加入原版创造物品栏的「功能方块」分页及搜索目录，JEI 使用对应目录收集物品。

方块 ID：`ftb_quest_nexus:ftbq_ae_submitter`。

1. 将提交器接入有电力与可用频道的 AE 网络，六个面都可连接。
2. 打开界面点击「绑定」，绑定当前玩家及其 FTB Teams 队伍。
3. 在 FTB Quests 任务详情页选择本队伍的提交器。可一次登记该任务内的消耗型物品及流体目标。只有非消耗型目标的经验奖励任务，也可以通过「登记任务到 Nexus 提交器领取经验奖励」加入队列，完成后领取。
4. 提交器每秒检查一次，按照 AE 实际扣除的物品数量或散装流体 mB 增加进度。仅合成物品目标及非消耗型目标交给原有任务流程。
5. 可处理的物品奖励自动回到 AE 网络，直接发放的经验点数和等级奖励发给当前绑定的玩家。绑定玩家离线时，经验奖励等待其上线。「奖励」切换仅队列任务／全部任务，新方块默认仅队列任务。「提示」只控制本模组自动提交触发的完成通知。

界面上的「匹配」默认关闭。开启后，提交器从 AE 库存匹配已解锁的消耗型目标，无需手动登记。列表显示本模式已实际提交过的目标，只拉取这些目标所属任务的可回网奖励。关闭后返回手动队列及原有领奖设置。

支持普通物品奖励及完全由物品构成的随机／战利品嵌套奖励表；选择奖励和带命令、经验等非物品结果的表继续手动领取。空抽会正常记为已领取，不会重新抽取。

可重复任务仍沿用 FTBQ 原生的领奖重置规则；提交器先完成本轮 AE 交付，再恢复其余原生自动领物品流程，避免队友先领奖导致本轮 AE 奖励被重置。审查修复及验证记录见 [docs/review-fixes-verification.md](docs/review-fixes-verification.md)。

AE 放不下的奖励保存在方块内，优先重试回网。有待交付余量时暂停新提交、领奖和重新绑定。正常采掘掉落会保留绑定、队列、设置和余量。创造模式中键选取生成全新提交器。同队玩家可点击「绑定」申请移交，当前绑定者会收到一条私人游戏消息，并可在 60 秒内点击同意或拒绝。绑定者离线时须等其上线后再申请。领取玩家离队后暂停任务，移交给原队伍成员后保留队列。

合成材料：书、AE 接口、逻辑处理器，按竖列顺序放入工作台。

## ME FTB 任务检查器

作者为山海／dishanhai，方块 ID 为 `ftb_quest_nexus:ftbq_ae_checker`，支持 Forge 1.20.1 与 NeoForge 1.21.1。

放置时自动绑定放置者 UUID 及其 FTBQ 队伍 UUID，六面连接 AE 网络，需要供电与可用频道。每秒以同一份只读库存快照检查全部已解锁物品／流体目标。非消耗型目标在库存达到总需求时自动完成，消耗型目标只显示库存，由提交器扣除资源后推进。遵循队伍锁定、任务依赖、顺序与重复冷却，排除仅合成目标。

流体库存合计 AE 原生流体与物品库存内流体桶，每桶为 1000 mB；桶物品本身仍可匹配物品目标。流体 NBT／数据组件和物品目标匹配规则分别沿用对应版本 API。全程不取出物品、不排空桶；奖励继续走 FTBQ 原生规则。

UI 使用外置 SH-Better-UI 的公开 `UiButton` 与玻璃面板，显示绑定、网络及目标库存／总需求，每页六行，支持按钮和滚轮翻页。每位观察者独立分页。已绑定的检查器由原队伍成员管理，移交需要当前绑定者在游戏消息中同意。绑定玩家离队后暂停，移交给原队伍成员后可恢复。世界存档保留绑定与暂停状态，挖掘掉落为全新检查器，重新放置绑定新的放置者。

合成材料为书、AE 接口、运算处理器，按竖列顺序放入工作台。材质采用亮灰白 AE 机壳与紫色指示灯、散热栅和核心，以点缀色区分提交器；六面在线／离线资源均为原生 16×16 px 帧，动画包含 8 帧。可编辑生成源位于 `tools/GenerateCheckerTextures.java`，布局示意生成源位于 `tools/CheckerUiPreview.java`。

运行时使用本轮重建、包含公开 `UiButton` API 的 SH-Better-UI 对应平台 jar。客户端六面显示、动画、玻璃模糊、tooltip、键盘与滚轮操作验收记录于 TODO。

## 安装与构建

运行环境：**Java 21**，Minecraft 1.20.1，Forge 47.4.16。

本机验证依赖：AE2 15.4.10、FTB Quests 2001.4.22、FTB Teams 2001.3.2、FTB Library 2001.2.13、Architectury 9.2.14、GuideME 20.1.15、SH-Better-UI 1.0.6、KotlinForForge 4.12.0。Jade 11.13.3 是可选信息显示集成。

**SH-Better-UI 是必需前置**，直接使用桌面 `SH-Better-UI` 项目提供的玻璃面板、模糊、tooltip 和动效，不复制或内嵌其实现。开发构建默认引用 `../SH-Better-UI/build/libs/sh_better_ui-1.0.6-dev.jar`，可用 `-PshBetterUiJar=<dev-jar>` 指定其他位置；UI 项目修改后重新构建其 `devJar` 即可供本项目使用。

游戏中独立安装 SH-Better-UI 正式 jar 及其 KotlinForForge 前置。检查器类型筛选需要包含 `IngredientFilter` API 的 SH-Better-UI 1.0.6 或更新构建。本模组自身不使用 Kotlin，也不将依赖展开或嵌入模组 jar。其余开发依赖已放在 `libs/`，正式运行依赖的本机副本在 `libs/runtime/`。不需要 GTCEu、GTLCore、GTLAdditions、KubeJS 或 LDLib。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = 'C:\jtmp'
$env:TMP = 'C:\jtmp'
.\gradlew.bat test build reobfJar --no-daemon --console=plain
```

根目录执行 `gradlew.bat build` 会依次构建 Forge 与 NeoForge。带 `reobfJar` 或 `extractNatives` 的命令只在 Forge 执行，包括用 `-x` 排除这些任务。NeoForge 的独立命令见 [NeoForge 构建说明](neoforge/README.md)。

本机依赖缓存完整时可加 `--offline`。更换机器后需要取得 ForgeGradle、映射及测试依赖缓存。Forge wrapper 使用 Gradle 8.8。

正式产物：`build/libs/ftb_quest_nexus-1.1.0.jar`。不要安装 `-sources.jar` 或开发用的 UI `-dev.jar`。

独立服务端测试：

```powershell
.\gradlew.bat runGameTestServer --offline --no-daemon --console=plain -x downloadAssets -x extractNatives
```

使用新项目的 `run/` 目录，不触碰现有游戏实例。实际执行结果见 `docs/verification.md`，实机待验内容见 [TODO.md](TODO.md)。

## FTB Quest Nexus 配置

在模组列表打开 FTB Quest Nexus 的「配置」，可在「外观」和「功能」之间切换。功能页提供任务详情的提交器入口及提交器／检查器的搜索补全开关。两项默认开启，应用后重新打开相应界面即可生效。

### 服务端配置

管理员可以通过 `/ftbqn config` 查看当前服务端设置。在下列键名后加 `true` 或 `false` 即可保存并生效，例如 `/ftbqn config rules autoSubmit false`。专用服务器也可在控制台执行。

手动编辑时请先停服。Forge 使用世界目录下的 `serverconfig/ftb_quest_nexus-server.toml`。NeoForge 默认使用实例的 `config/ftb_quest_nexus-server.toml`，如世界目录下存在同名 `serverconfig` 配置，则优先使用并写入该文件。

| 分组 | 键名 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `defaults` | `matching` | 关 | 新提交器启用匹配模式。开启时可自动扣除 AE 库存。 |
| `defaults` | `queuedRewards` | 开 | 新提交器在手动模式下仅领取队列所属任务的奖励。 |
| `defaults` | `completionToast` | 开 | 新提交器提交时显示完成提示。 |
| `rules` | `autoSubmit` | 开 | 允许提交器在手动队列和匹配模式中自动扣料并提交。 |
| `rules` | `autoCheck` | 开 | 允许检查器扫描任务库存、推进检查目标和接收任务所需的 FE。 |
| `rules` | `networkRewards` | 开 | 允许提交器将新领到的物品和支持的化学品奖励回网。已入账的待交付奖励继续重试。 |

默认值应用于首次由物品放置的提交器。旧存档和采掘后重新放置的提交器保留机器自身的设置。

### FTBQ 透明玻璃覆盖

两个平台都可通过客户端 `config/ftb_quest_nexus-client.toml` 切换 FTBQ 外观，默认保留原版：

```toml
[ftbqUi]
mode = "GLASS"
opacity = 1.0
blurRadius = 8
```

`mode` 可选 `ORIGINAL`／`GLASS`。`opacity` 范围 0–1，缩放玻璃底色和边框原有的不透明度（默认材质本身已半透明）；0 不绘制结构玻璃面层。`blurRadius` 为 0–16 的整数 GUI 单位，0 关闭背景模糊。修改后关闭并重新打开对应界面；任务界面打开的子对话框继承其配置快照。

覆盖主画布、章节栏、工具按钮、任务详情、槽位、输入框、滚动条、右键菜单及 FTBQ 所属的配置／选择窗口。原任务控件、拖曳、缩放、编辑、提交、领奖及状态颜色保持；整合包自定义背景、章节图片、图标与连线继续使用原资源。其他模组的 FTB Library 界面不启用覆盖。

复用外置 SH-Better-UI，玻璃根背景采样后再绘制控件；模糊不可用时保留透明材质。非零透明度下的库内细高光，以及 tooltip，继续使用 UI 库预设材质。实际画面和交互仍需客户端验收，构建与验证见 [docs/glass-verification.md](docs/glass-verification.md)。

### 功能方块外观

在模组列表打开 **FTB Quest Nexus → 配置 → 方块外观**，分别选择 AE 任务提交器和检查器的主题，然后点击「应用」。可以跟随 SHBUI 当前主题，也可以选择其内置材质或资源包注册的主题。默认使用透明玻璃，选项保存在客户端的 `[ftbqUi]` 下，键名为 `submitterTheme` 和 `checkerTheme`。资源包主题暂不可用时回退到透明玻璃，主题 ID 会保留。

## 原项目与来源

由 `gt_shanhai:ftbq_ae_submitter` fork，保留其提交与奖励交付语义、玻璃外观及动画资源。原项目代码、注册及配方保留；旧方块与新方块使用独立 ID、队列、网络通道和节点，不自动迁移旧存档。并装时各自有任务登记入口，实际共存操作仍需按验收单测试。

作者与代码来源见 [SOURCE.md](SOURCE.md)。项目名称和模组 ID 固定为 **FTB Quest Nexus / `ftb_quest_nexus`**。
