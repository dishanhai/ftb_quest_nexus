# FTB Quest Nexus 1.0.0 审查修复验证

作者：dishanhai。日期：2026-11-07。范围：Forge 1.20.1／NeoForge 1.21.1 三项审查修复。

## 文件／位置／内容

| 文件（两平台同名路径） | 位置 | 内容 |
| --- | --- | --- |
| submitter/SubmitterBlockEntity.java | applyAction | 绑定也执行 canManage；已绑定方块只能由原队伍成员重绑，接替离队玩家时保留队列，待交付奖励仍阻止重绑 |
| submitter/SubmitterBlockEntity.java | queueTask | 沿用 taskId 锚点，一次加入所属 Quest 全部可消耗、非仅合成物品目标，仍验证队伍并去重 |
| submitter/SubmitterBlockEntity.java、QuestWork.java | runQueuedWork／claimRewards／canClaim | 复用同一奖励范围和物品树判定，在增加进度时暂缓原生物品自动领奖，先完成 AE 交付或持久缓冲，再恢复被暂缓任务的原生自动领取 |
| api/ftbq/FtbqAutoClaimDeferral.java | 共用服务端上下文 | 按 TeamData 身份与可处理奖励筛选；返回实际暂缓的 Quest，finally 恢复嵌套／异常状态 |
| mixin/FtbTeamDataAutoClaimMixin.java、ftb_quest_nexus.ftb_quests.mixins.json | checkAutoCompletion | 只拦进度作用域内的原生自动领奖；非物品奖励及作用域外操作保持原流程 |
| submitter/SubmitterRegressionGameTests.java | 新增九项 | 权限、队伍成员接替、登记去重／过滤、个人与队伍自动奖励、满盘重试、经验奖励、异常恢复、可重复任务 |
| build.gradle | minecraft.runs | 开发源码运行显式载入 Mixin 清单；正式包继续使用 manifest |
| zh_cn.json／en_us.json、README、TODO、.learnings/ERRORS.md | 文案／记录 | 明确一次登记全部目标、原队伍重绑和验证边界；未硬编码界面文本到 Java |

## 复现与验证

- 初始回归重现：跨队玩家可重绑、第二个物品目标未登记、自动奖励绕过 AE。Forge 首轮还暴露开发运行未加载自有 Mixin，补启动参数后验证真实注入。
- 补充红灯：两平台可重复任务均被队友的个人自动领奖先行重置。最终改为先完成绑定玩家的 AE 交付，再恢复原生流程；继续遵循 FTBQ 原生的重复任务重置规则。
- 红灯日志：Forge `build/review-fixes-red.log`；双平台各自 `build/review-repeatable-red.log`。
- 最终日志：双平台各自 `build/review-fixes-final.log`，包含实际执行的单测、构建及服务端测试。

环境：JDK 21.0.11，TEMP／TMP 为 C:\jtmp。

Forge：

```powershell
.\gradlew.bat test build reobfJar runGameTestServer --offline --no-daemon --console=plain -x downloadAssets -x extractNatives
```

NeoForge（在 neoforge 目录）：

```powershell
.\gradlew.bat test build runGameTestServer --offline --no-daemon --console=plain
```

| 检查 | Forge | NeoForge |
| --- | --- | --- |
| JUnit | 24 项，失败／错误／跳过均 0 | 29 项，失败／错误／跳过均 0 |
| 服务端 GameTest | 20 项 required 全通过 | 20 项 required 全通过 |
| 构建 | build／reobfJar 成功 | build／verifyModJar 成功 |
| 新类与 Mixin | 暂缓上下文及内部类、服务端 Mixin 均存在且已登记 | 同左 |
| 产物隔离 | 无 GameTest、内嵌 UI／AE／FTB／Kotlin 或 Jar-in-Jar | 同左 |

新增业务测试使用真实方块、FTBQ 对象与 AE 网格存储挂载；测试玩家由平台 FakePlayer 创建，在线成员清单由测试 TeamData 提供。为隔离电力／频道问题，测试将 online 状态置真并直接调用 runQueuedWork，存储挂载使用有容量控制的内存 MEStorage。它们证明业务调用链与服务端 Mixin，不证明客户端操作或完整有线网络。

## 产物

- Forge：`build/libs/ftb_quest_nexus-1.0.0.jar`
  - SHA256：`59418D4359415921A3B16FC0EF6749CAE44168108E9CBA79EE6CA589E4140FEC`
  - 与 `build/reobfJar/output.jar` 哈希一致。
- NeoForge：`neoforge/build/libs/ftb_quest_nexus-neoforge-1.21.1-1.0.0.jar`
  - SHA256：`B667D3A8EAFBBE91A86C88698F0AC2BC1D98A92EA175CBB7CCF7A66A563FB540`

未部署、重启现有游戏实例或提交 Git。任务详情页实际点击、多队伍实机操作、完整有线 AE 网络、客户端界面和跨重启场景仍保留在双平台 TODO。
