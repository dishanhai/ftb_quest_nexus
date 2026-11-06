# FTB Quest Nexus — NeoForge 1.21.1

作者：dishanhai。此目录是独立 Gradle 建置，保留上层 Forge 1.20.1 原实现；共用纯逻辑、语言与图片，平台 API 单独适配。

## 环境与前置

Java 21、Minecraft **1.21.1**、NeoForge **21.1.265**。固定开发依赖：AE2 19.2.18、FTB Quests 2101.1.38、FTB Library 2101.1.39、FTB Teams 2101.1.12、Architectury 13.0.11、GuideME 21.1.22。

UI 使用独立 **SH-Better-UI NeoForge 1.21.1 1.0.3** 及其 **KotlinForForge NeoForge 5.10.0** 前置。Jade 15.10.6 为可选集成。不需要 GTCEu、GTL、KubeJS 或 LDLib。

本机开发 jar 位于 `libs/`，对应官方下载链接和 SHA256 见 [`libs/README.md`](libs/README.md)。开发库不会打入 Nexus 产物。KotlinForForge 由 Maven 下载，正式游戏需另装其完整 NeoForge runtime jar。

## 构建

先在 `SH-Better-UI/neoforge/` 使用该目录自己的 wrapper 执行 `test build`，再在本目录运行：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = 'C:\jtmp'
$env:TMP = 'C:\jtmp'
.\gradlew.bat test build --no-daemon --console=plain
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

wrapper 使用 Gradle 9.2.1，不使用上层 Forge 的 Gradle 8.8。首次构建会下载 NeoForm/Minecraft 工具链，缓存完整后可加 `--offline`。NeoForge 使用正式映射，无需 `reobfJar`。

默认外部 UI 路径：`../../SH-Better-UI/neoforge/build/libs/sh_better_ui-neoforge-1.21.1-1.0.3.jar`，可用 `-PshBetterUiJar=<jar>` 覆写。

正式产物：`build/libs/ftb_quest_nexus-neoforge-1.21.1-1.0.0.jar`。游戏中同时安装对应的 SH-Better-UI NeoForge jar 及所列运行前置；不要安装 sources jar 或 Forge 1.20.1 jar。

## FTBQ 玻璃主题覆盖

客户端配置位于 `config/ftb_quest_nexus-client.toml` 的 `[ftbqUi]`：`mode = "ORIGINAL"` 保留原版，改为 `"GLASS"` 启用透明玻璃。`opacity` 为玻璃填充的不透明度倍率（0–1，默认 1），`blurRadius` 为模糊半径（0–16，默认 8；0 关闭模糊）。修改后关闭并重新打开 FTBQ 界面。

`opacity = 0` 不绘制结构玻璃面层；0 与 1 之间缩放填充和边框透明度，库内细高光及 tooltip 保持预设材质。FTBQ 自定义文字颜色与任务状态颜色保留。

主题覆盖任务画布、章节栏、任务详情、FTBQ 独立页面及由它打开的 FTB Library 子对话框；控件、任务图标、任务状态、任务连线与操作仍由 FTBQ 处理。复用独立 SH-Better-UI，不内嵌 UI 库。无关模组的 FTB Library 界面不启用该覆盖。自定义画布图片继续绘制在玻璃色层下；玻璃效果和各项交互仍需客户端实机验收。

## 行为与保存

沿用 [`../README.md`](../README.md) 的使用方式。网络改用 NeoForge 单向 payload，服务端重新验证任务与绑定队伍；AE2 六面连接、真实扣料、物品奖励缓冲与去重语义保留。

1.21.1 生存掉落使用 `minecraft:custom_data` 保存绑定、队列、设置与待交付奖励，只复制本模组用户数据，不复制 AE 节点身份。创造复制的客户端导出与服务端回填均剥离身份与奖励。两个 Minecraft 版本之间不提供存档迁移承诺。

实际构建及测试结果见 [`../docs/neoforge-verification.md`](../docs/neoforge-verification.md)，待验内容见 [`TODO.md`](TODO.md)。独立测试仅使用本目录 `run/`，不修改现有整合包。
