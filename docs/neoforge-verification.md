# NeoForge 1.21.1 验证记录

作者：dishanhai。日期：2026-11-07。

## 交付范围

FTB Quest Nexus 与 SH-Better-UI 各新增 `neoforge/` 独立建置，保留根目录 Forge 1.20.1。共用纯逻辑、语言与图片，平台入口、网络、保存与渲染 API 单独适配。SH-Better-UI 继续是独立前置，不内嵌在 Nexus 中。

运行组合：Java 21.0.11、Minecraft 1.21.1、NeoForge 21.1.265、AE2 19.2.18、FTB Quests 2101.1.38、FTB Library 2101.1.39、FTB Teams 2101.1.12、Architectury 13.0.11、GuideME 21.1.22、KotlinForForge NeoForge 5.10.0。Jade 15.10.6 编译接口已适配，为可选前置，本次专用服务器未安装 Jade。

## 正式产物

| 项目 | 文件位置 | 大小 | SHA256 |
| --- | --- | --- | --- |
| Nexus | `neoforge/build/libs/ftb_quest_nexus-neoforge-1.21.1-1.0.0.jar` | 84,582 bytes | `03534CC2414A4B62F0BA510F9F22289A1AB1E1E85B38B6B446BE6BAD73CC3E22` |
| SH-Better-UI | `../SH-Better-UI/neoforge/build/libs/sh_better_ui-neoforge-1.21.1-1.0.3.jar` | 87,816 bytes | `DAF607E8C6E888A766FE478268FDDC7A888F845280D43F80417A36B448D37D52` |

两个正式 jar 均通过 ZIP 内容隔离检查：包含 NeoForge metadata，不含 Forge `mods.toml`、测试类、Kotlin/KFF 或其他依赖 class；Nexus 不含 UI 库类与 shader。Nexus 85 个条目、16 个 JSON/mcmeta 均可解析；52 个中英语言键一致。UI 61 个条目、3 个 JSON/mcmeta 均可解析，实际 GLSL 为 `uniform vec2 WaveOrigin`，与 JSON 的二分量 float uniform 相符。

## 实际执行

两库 NeoForge 目录使用各自 Gradle 9.2.1 wrapper / ModDevGradle 2.0.148：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = 'C:\jtmp'
$env:TMP = 'C:\jtmp'
.\gradlew.bat test build --offline --no-daemon --console=plain
```

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| Nexus 编译／test／build／verifyModJar | 成功，21 项 JUnit，0 failures/errors/skips | `neoforge/final-build.log`、`neoforge/build/test-results/test/TEST-*.xml` |
| UI 编译／test／build／verifyLibraryJar | 成功，109 项 JUnit，0 failures/errors/skips | UI `neoforge/shader-verified.log`、测试 XML、`neoforge/VERIFICATION.md` |
| Nexus NeoForge 专用服务器 | 成功，11 项 required GameTest 全部通过 | `neoforge/gametest-server.log` 的 `All 11 required tests passed`，Gradle exit 0 |
| Nexus 原 Forge 1.20.1 `test build reobfJar` | 成功，测试任务由 Gradle 判定 UP-TO-DATE；现有 XML 为 17 项通过 | `build/forge-regression-20261107.log` |
| UI 原 Forge 1.20.1 `test build` | 成功，测试任务由 Gradle判定 UP-TO-DATE；现有 XML 为 107 项通过 | UI `build/forge-regression-20261107.log` |

专用服务器命令在 Nexus `neoforge/` 执行：

```powershell
.\gradlew.bat runGameTestServer --no-daemon --console=plain
```

首次依赖及客户端资产元信息由 ModDevGradle 正常下载；缓存完整后可用 `--offline`。不跳过 `downloadAssets`，因为 prepareRun 需要该任务生成的 properties。服务器模组清单实际包含 Nexus、SH-Better-UI、KFF、AE2、Architectury、GuideME 与 FTB 系列；没有 GT/GTL/KubeJS/LDLib。

服务器首次空运行目录缺 `server.properties`，Minecraft 输出默认配置读取错误后继续启动；另有原版 teleport 参数歧义与开发环境 union URL 警告。它们没有导致测试失败。没有依赖 Mixin 失败或客户端类隔离崩溃。

## 覆盖内容与证据边界

- JUnit：共享队列、奖励缓冲、局部 toast 抑制、任务页按钮布局；新增实际 NeoForge payload codec 回环、UUID／unsigned task bits、清单不可变快照与反序列化数量上限。
- GameTest：默认设置、队伍／队列／待交付奖励保存，物品自定义 data component 保存，真实 `Block.getDrops` → `copy_components` → `BlockItem.place`，创造选取与服务端回填防复制，恢复 UUID 碰撞，AE 六面节点创建／销毁及 owner 恢复。
- FTBQ 集成：实际 2101 的 TeamData／ItemTask／ItemReward API，配合内存 MEStorage 检查准确扣料、拒绝锁定／不消耗任务、满盘奖励保留、部分插入重试及重复领取不再发物品。它不是完整有线 AE 网络实机测试。
- UI：入口 ClassLoader 隔离、动效与几何、材质、实际输出 shader 的 uniform 契约通过；没有执行 GPU 渲染或客户端画面验收。
- 只读审查通过，新平台没有未解决的已确认 P1/P2；单元测试 classpath 缺失问题已关闭。UI 原有异常清理／嵌套 RGBA 条件问题保留于其 TODO，未扩修原 Forge 实现。

待实机项目详见 `neoforge/TODO.md`。未部署到现有游戏，未重启既有整合包，未提交、推送或发布。原 Nexus Forge jar SHA256 仍为 `1F6F540A451DC844EED968C914A363FFF30B3E8D16B01E486A6A7BE2EA09B0E7`。
