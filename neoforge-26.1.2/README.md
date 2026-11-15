# FTB Quest Nexus · NeoForge 26.1.2

作者为山海／dishanhai。此目录使用 Java 25、Gradle 9.2.1 和 NeoForge 26.1.2.126。将 [开发依赖](libs/README.md) 放入 `libs/`。运行时独立安装同版本线的 FTB Quests、FTB Library、FTB Teams、AE2、GuideME、SH-Better-UI 1.0.7-alpha.8 和 KotlinForForge 6.2.0。

开发构建沿用 1.21.1 的 Gradle wrapper。从仓库根目录运行：

```bash
./neoforge/gradlew -p neoforge-26.1.2 build
./neoforge/gradlew -p neoforge-26.1.2 runGameTestServer
```

构建优先从本目录的 `libs/` 查找对应版本的 SH-Better-UI JAR 和可选 SH-Common-Lib JAR，其次查找同级项目各自的 `neoforge-26.1.2/build/libs/`。可用 `-PshBetterUiJar=<jar>` 或 `-PshCommonLibJar=<jar>` 指定路径。SH-Common-Lib 仅在存在时加入编译路径，目前不是必需前置。正式产物位于 `build/libs/ftb_quest_nexus-neoforge-26.1.2-1.2.0.jar`。

模组列表中的 FTB Quest Nexus 配置页沿用 SHBUI 26.1.2 控制面板尺寸。可用 SHBUI 圆角按钮分别选择提交器和检查器的主题，也可以跟随 SHBUI 当前外观或选择资源包主题。应用后重新打开方块界面。26.1.2 的工具提示使用 Minecraft 原生提取流程，搜索补全在更高的绘制层显示。

GameTest 覆盖提交器存档、掉落和创造复制、检查器 FE 能力与配方。测试函数和结构仅随开发构建加载。其余验收项目见 [TODO.md](../TODO.md)。
