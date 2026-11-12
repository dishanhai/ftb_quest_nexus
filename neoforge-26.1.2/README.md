# FTB Quest Nexus · NeoForge 26.1.2

作者为山海／dishanhai。此目录使用 Java 25、Gradle 9.2.1 和 NeoForge 26.1.2.126。将 [开发依赖](libs/README.md) 放入 `libs/`。运行依赖为同版本线的 FTB Quests、FTB Library、FTB Teams、AE2、GuideME、SH-Better-UI 和 KotlinForForge。

开发构建沿用 1.21.1 的 Gradle wrapper。从仓库根目录运行：

```bash
./neoforge/gradlew -p neoforge-26.1.2 build -PshBetterUiJar=/path/to/sh-better-ui-26.1.2.jar
./neoforge/gradlew -p neoforge-26.1.2 runGameTestServer -PshBetterUiJar=/path/to/sh-better-ui-26.1.2.jar
```

省略 `-PshBetterUiJar` 时，构建读取同级 SH-Better-UI 仓库的 26.1.2 产物。完整构建需要包含 Nexus 所用控件和渲染 API 的版本。当前工作区的 SHBUI `1.0.7-alpha.1` 提供共享内核、搜索索引和图标资源，Nexus 的 `compileJava` 仍受客户端接口阻塞。

GameTest 覆盖提交器存档、掉落和创造复制、检查器 FE 能力与配方。测试函数和结构仅随开发构建加载。其余验收项目见 [TODO.md](../TODO.md)。
