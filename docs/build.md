# FTB Quest Nexus 构建说明

作者：dishanhai。Java 21 / Forge 1.20.1-47.4.16 / Gradle 8.8。当前仅实现 Java，不打包 Kotlin 类。

`gradlew.bat test build reobfJar --no-daemon --console=plain`。首次 wrapper 运行会下载 Gradle 8.8。已有本机发行版时可直接使用 `..\gt_shanhai\gradle-install\gradle-8.8\bin\gradle.bat`。

本地 API jar 放 `libs/`，发行运行依赖另放 `libs/runtime/`。二者不可一起放进游戏 mods（UI dev/发行包不可同时加载）。jar 不嵌入其他模组。

玻璃 UI 固定复用独立 `SH-Better-UI` 项目作为必需前置；默认开发依赖路径为 `../SH-Better-UI/build/libs/sh_better_ui-1.0.3-dev.jar`，由 `sh_better_ui_version` 控制版本，可用 `-PshBetterUiJar=<dev-jar>` 覆盖路径。先在 UI 项目构建 `devJar`；游戏安装使用该项目正式发行包。新模组不复制库源码、着色器或运行时类。

本机依赖：AE2 15.4.10、GuideME 20.1.15、FTB Quests 2001.4.22、FTB Teams 2001.3.2、FTB Library 2001.2.13、Architectury 9.2.14、SH-Better-UI 1.0.3、KotlinForForge 4.12.0。Jade 11.13.3 是可选运行集成，仅作为 compileOnly API。

虽然本模组不使用 Kotlin，SH-Better-UI 的元数据要求外部 KotlinForForge，因此运行时仍要安装。无需 GTCEu、GTLCore、GTLAdditions、LDLib 或 KubeJS。

`runGameTestServer` 已配置命名空间 `ftb_quest_nexus`；独立 `run/` 目录不涉及原游戏实例。
