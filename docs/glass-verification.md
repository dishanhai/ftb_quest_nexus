# FTB Quest Nexus 1.0.0 玻璃覆盖验证

作者：dishanhai。日期：2026-11-07。范围为用户批准的主题覆盖＋局部绘制适配；NeoForge 由独立 agent 实施，root 整合核验。

## 文件与行为

- Forge：`src/main/java/com/dishanhai/ftb_quest_nexus/config/ClientUiConfig.java`、`client/glass/` 及六个玻璃 Mixin；入口注册客户端配置／模糊白名单／tooltip，客户端 Mixin 清单同步。
- NeoForge：`neoforge/src/main/java/com/dishanhai/ftb_quest_nexus/config/ClientUiConfig.java`、`client/glass/` 及七个玻璃 Mixin。2101 章节背景与编辑器状态列使用其实际 API 独立适配。
- 共用：`GlassUiPolicy`、中英语言及原有图片资源。两个平台原版默认，打开画面时捕获配置，子对话框继承；FTBQ 重用同一实例时由 `initGui` 重建配置快照。
- 材质复用外置 SH-Better-UI；不改依赖源码、任务业务和网络协议。根背景取样，子控件仅叠色；其他模组 FTB Library 界面不启用覆盖。

## 实际检查

| 检查 | 结果与证据 |
| --- | --- |
| Forge `test build reobfJar --offline --no-daemon --console=plain` | 成功；JUnit XML 24 项、失败／错误／跳过均 0 |
| NeoForge `test build --offline --no-daemon --console=plain` | 成功；JUnit XML 29 项、失败／错误／跳过均 0；`verifyModJar` 成功 |
| 共用策略 | 3 项：alpha 缩放／边界／无效数值；FTBQ／FTBLib 包名作用域 |
| Forge 实际 jar 契约 | 4 项：七个 FTBQ getTheme、三个 FTBLib override、详情背景／图标／计时器调用、背景拖曳／框选与编辑器状态条 getter |
| NeoForge 实际 jar 契约 | 5 项：八个 FTBQ getTheme、三个 FTBLib override 与 BaseScreen 初始化／背景、详情／章节、拖曳／框选、编辑器状态列 |
| Forge 专用服务器 | 11 项 required GameTest 全部通过；`build/glass-gametest.log:105` |
| NeoForge 专用服务器 | 11 项 required GameTest 全部通过；`neoforge/run/logs/latest.log:73` |
| 资源 | 中英语言各 56 键、集合一致；双平台 jar 的 JSON／mcmeta 均可解析 |
| 产物隔离 | 无 UI 库、AE2、FTB、Kotlin、Jar-in-Jar、GameTest 类或空模板；Forge 正式 jar 与 reobf 输出哈希相同 |

单元测试确实执行；最终仅客户端 Mixin 覆盖点的补充之后重新构建／单测，未重复运行专用服务器。服务端测试证明服务端入口及现有任务／保存行为没有因初次接入而崩溃，不验证客户端 Mixin 变换和 GPU 绘制。

## 最终产物

- Forge：`build/libs/ftb_quest_nexus-1.0.0.jar`，SHA256 `70EF6AE873CDC43678E5EB14AEDB93E69946A6A65AA4DD43F65686425E68D25A`。
- NeoForge：`neoforge/build/libs/ftb_quest_nexus-neoforge-1.21.1-1.0.0.jar`，SHA256 `CE5A06CF9A2B0405927F1BBEB06CC9762C2DBEB5E6034C75B82B0E9818532192`。

## 尚未验证与材质限制

未启动 Minecraft 客户端验证 Client Mixin 实际变换、画面、模态层、命中区域、拖曳缩放、编辑、任务提交／领奖、不同 GUI 缩放、资源重载和其他模组画面隔离。TODO 与 NeoForge TODO 保留完整清单。

`opacity=0` 跳过结构面层；非零时填充／边框缩放，外部 UI 库的细高光和 tooltip 使用预设材质，tooltip 不随本配置调整透明度／模糊半径。Git 交付状态以提交历史为准；未部署或重启现有游戏实例。
