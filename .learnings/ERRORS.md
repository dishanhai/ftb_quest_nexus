# Author: dishanhai — fork verification errors

## 2026-11-07：提交器权限、登记范围与自动领奖顺序

- 状态：resolved；客户端实机验收保留于 TODO。
- 用户本轮要求修复审查发现的问题，已绑定方块的重绑改为只允许原队伍成员。此规则取代下方“功能保留及命名”记录的历史跨队现场重绑行为；原队伍成员可接替离队领取玩家，保留队列。
- 单一 taskId 封包继续作为任务锚点，服务端一次登记所属 Quest 全部可消耗、非仅合成 ItemTask，仍验证提交器队伍且去重，不增加新协议。
- addProgress 会同步触发 FTBQ 自动领物品，必须在进度事件内暂缓受本提交器领奖范围覆盖的物品奖励，再先执行 AE 真实交付／持久缓冲，最后对确实被暂缓的 Quest 恢复原生自动领奖。仅拦绑定玩家仍不够：可重复任务会被队友的个人奖励先行重置；双平台红灯实证后已修正此顺序。
- 暂缓上下文匹配 TeamData 身份并用 finally 恢复嵌套／异常状态；非物品奖励及其他队伍不受影响。普通个人奖励在同一处理轮恢复，重复任务保留 FTBQ 原生重置规则。
- 红灯：`build/review-fixes-red.log`、双平台 `build/review-repeatable-red.log`。最终绿灯：双平台 `build/review-fixes-final.log`，各 20 项 GameTest；24／29 项 JUnit、构建与 jar 检查通过。详见 `docs/review-fixes-verification.md`。

## 2026-11-07：审查修复的测试环境与夹具校正

- 状态：resolved。
- Forge 源码运行只设置 jar manifest 的 MixinConfigs，不会在开发运行加载本模组 Mixin。追踪得到 shouldDefer=true，但 TeamData.claimReward 仍执行；增加 runs 的 `-mixin.config=ftb_quest_nexus.mixins.json` 后，真实重定向参与测试，19 项服务端回归通过。NeoForge 的 [[mixins]] 会在源码运行加载。
- 2101 的 BaseQuestFile.refreshIDMap 已为 protected，测试用子类公开代理，不能照搬 2001 的 public API。
- Tristate 的 NBT 保存是布尔值，不能把 team_reward 写成字符串；测试已改用 putBoolean，并分别核对个人／队伍奖励。
- 权限案例须先把测试玩家置于有效操作距离内，避免因距离拒绝而误以为队伍权限生效。
- PowerShell 下原生 rg 不展开路径通配符；应明确路径或对目录使用 -g。本轮误用已纠正。

## 2026-11-07：玻璃覆盖的补丁与验证边界

- 状态：resolved。
- NeoForge 实施时，一次跨文件补丁因最后一处 TODO 上下文不匹配整体未应用；改为拆开补丁并逐文件核对后继续，不假定前面文件已写入。
- 共享策略先写测试再实现，首轮测试编译因 `GlassUiPolicy` 尚未存在而失败，属于预期红灯；实现后针对性测试通过。依赖 Mixin 的客户端注入仍需独立于单元测试、构建与专用服务器验证。
- 只在 BaseScreen.getTheme 注入会遗漏自行覆写该方法的 FTBQ 与 FTBLib 页面；已按两个版本真实 jar 分别核对声明并补充目标。弱引用配置快照还必须在 initGui 重建，不能假设关窗即销毁 QuestScreen 实例。
- 检查并行实现时曾误用 Forge 的 QuestGlassUi 文件名读取 NeoForge 实现；先列出 agent 文件清单，改读实际 QuestUiHooks，不据文件不存在推断功能缺失。
- 不猜测并行执行者的验证日志文件名；已由 agent 确认 NeoForge 的本轮服务端证据实际在 `neoforge/run/logs/latest.log`，root 重新读取并核对通过行。

## 2026-11-07：分批提交的搜索参数检查

- 状态：resolved。
- PowerShell 不会为原生 rg 展开 `*.gradle`、`*.properties` 路径；本轮误用导致路径语法错误，已改用明确文件列表重新完成检查。后续统一使用目录与 `-g` 筛选。
- Git 暂存检查发现自有 Forge wrapper 的三处文件末尾空行；仅移除多余空行，不改启动逻辑或第三方 NeoForge wrapper。
- NeoForge 提交前同样发现五个自有 submitter 文件的末尾空行；仅清理尾部空白后重新检查，不修改功能逻辑。

## 2026-11-07：NeoForge 1.21.1 移植取證

- 狀態：平台編譯與單元測試已修正；專用伺服器驗證結果另記 `docs/neoforge-verification.md`。
- NeoForge `DeferredRegister.register` 同時有 Supplier／Function 多載，方塊新增 Properties 建構子後 `SubmitterBlock::new` 產生歧義。改為明確的零參 lambda；未改註冊 ID。
- ModDevGradle 的 Minecraft／Netty classpath 預設只配置主來源集，新 payload 測試因此出現 15 項缺類錯誤。測試來源集明確接入 main compile／runtime classpath，JUnit launcher 另行聲明。之後 21 項單測實際通過。
- 專用伺服器也會依賴 ModDevGradle 的 `downloadAssets` 所產 properties；首次尚未完成下載時直接 `-x downloadAssets` 會在 `prepareGameTestServerRun` 因輸入檔不存在而失敗。保留正常依賴下載，不偽造資產完成標記；首次運行不強制 offline。
- 首次 runtime offline 缺 Netty native epoll，改正常線上解析。這是初次快取缺依賴，不是遊戲測試通過證據。
- 工具取證：PowerShell 不替原生 rg 展開帶萬用字元的路徑，應搜尋目錄並用 `-g` 篩選；GitHub 匿名 API rate limit／舊分支 URL 無效時，改以已固定的官方 jar 與 javap 核對 API。
- NeoFormRuntime 2.0.31 的 `download-assets` 子指令不支援 `--help`，不可據此判斷下載功能故障；本輪沿用 ModDevGradle 的標準下載任務。

## 2026-11-07：创造复制、放置身份与节点归属

- 状态：已依据红灯修正，复验结果另记于 docs/verification.md。
- 创造模式：普通中键无 NBT 不足，Ctrl+中键的服务端会依来源坐标调用 `saveToItem` 回填真实奖励。客户端 `saveAdditional` 不导出用户数据，服务端 `saveToItem` 明确移除既有 `BlockEntityTag`；正常 loot 的 `copy_nbt` 走 `saveWithFullMetadata`，仍保留用户数据。红灯证据：`build/gametest-defects-red.log`。
- 原生放置：`onLoad` 早于物品 NBT 恢复，立即登记会使用临时 UUID。登记和碰撞修正延至首次 tick；补测试复现两个恢复相同 UUID 的方块未被去重。
- 节点归属：`mainNode.create` 的 NBT 载入可覆盖先设的 owner，现于 create 后恢复绑定领取玩家的 AE owner。
- 并行文件协作：两位实施者同时 Add 错误记录导致后写覆盖先写；已合并，后续该文件由 root 单独维护。跨文件 apply_patch 验证失败不会部分写入源码，重新核对后单独应用源码补丁。

## 2026-11-07：功能保留及命名

- 用户最终确定 FTB Quest Nexus / `ftb_quest_nexus`，不以 shanhai 为题；之前的名称仅作为被替换的提案，不创建对应项目。
- 复核恢复原有现场重绑语义：当前真实队伍可重绑，跨队时清队列，存在待交付奖励仍禁止重绑；其他操作及远程登记必须属于绑定队伍。
- 审查发现 Jade 遗漏原有前三项目标摘要，已补可翻译目标标题／失效 ID／隐藏数量，并保留原数量统计。

## 2026-11-07：工具和历史环境

- 旧记忆中的桌面 `shanhai_shop/build.gradle` 已不存在；以当前可读取项目核对构建链路。
- PowerShell 的 `-ErrorAction` 不能传给原生 rg；apply_patch 修改已有路径使用 Update，不能在同一补丁 Delete/Add 同路径。
- 首轮并发编译在 HeaderButtonLayout 创建之前到达测试编译，后续串行 Gradle 已通过；之后 Gradle 由一位执行者持有。

## 2026-11-07：FTBQ ItemReward 测试构造数量

- 状态：resolved。
- 问题：满盘奖励 GameTest 假设 `new ItemReward(id, quest, new ItemStack(item, 8))` 创建 8 个奖励，实际只创建 1 个。
- 根因：真实 2001.4.22 jar 的三参构造固定委托四参构造的 `count=1`，奖励数量独立于样板 ItemStack 的 count；`javap -c` 已证实。
- 修正：测试改用四参构造 `new ItemReward(id, quest, new ItemStack(item), 8)`，生产领奖算法不变。失败证据 `build/gametest-defects-red.log`，复验见后续 GameTest 绿灯日志。

## 2026-11-07：GameTest 模板和开发 refmap

- 状态：resolved（配置修正，具体测试结果另看验证日志）。
- 首轮：AE2 的 SRG refmap 在 Mojmap 开发环境找不到 MinecraftServer 目标。根因是开发运行未配置 refmap 再映射；启用 `mixin.env.remapRefMap` 与 `build/createSrgToMcp/output.srg` 后，所有依赖与本模组在服务端正常加载。证据：`build/gametest-server.log`、`build/gametest-server-red.log`。
- 第二轮：`@GameTestHolder` 会给模板自动添加模组 namespace；`template="ftb_quest_nexus:empty"` 形成重复前缀。修正为 `template="empty"`，`@PrefixGameTestTemplate(false)` 保留；自有模板位于 `data/ftb_quest_nexus/structures/empty.nbt`。Forge 发行 jar 不内含此前假定的 `forge:empty5x5x5`。
- 改进：在实际 Forge jar 中核验模板资源，且将模板注解值和 Forge 自动 namespace 处理分开检查。不要通过禁用依赖 Mixin 绕过开发映射错误。

## [ERR-20261107-TOOLTIP-001] Modern UI 先绘制的文字被玻璃模糊取样

- 作者：山海／dishanhai；状态：resolved（注册顺序与双平台构建验证），客户端视觉验收见 TODO.md。
- 用户现象：提交器按钮、信息行和 JEI 物品 tooltip 出现文字模糊、断裂或不可见。
- 根因证据：实机 ModernUI-Forge-1.20.1-3.12.0.1 的 `UIManagerForge.onRenderTooltipH` 在 HIGH 优先级绘制完整 tooltip，LOW 才取消事件。Nexus 原 NORMAL 监听在两者之间调用 `GlassTooltip.render`，背景模糊将已有 tooltip 文字取样，随后重复绘制。
- 修正：Forge／NeoForge 的 NexusClient 都用 HIGHEST 优先级注册 tooltip 监听，沿用原有提交器／FTBQ 玻璃画面范围与 GlassTooltip 的事件取消，先于其他渲染器取得绘制权。
- 回归：新增 TooltipRegistrationTest，直接读取编译后 NexusClient，核验 HIGHEST 和不接收已取消事件的注册重载；Forge 修改前 1 项失败，修改后 Forge 25／NeoForge 30 项 JUnit 全通过。
- 打包：Forge test／jar／reobfJar、NeoForge test／build／verifyModJar 通过，Forge 正式 jar 与 reobfJar/output.jar 的 SHA-256 一致。
- 构建环境：首次并发构建的 Forge 测试 worker 遇到本机 socket 消息读取与连接拒绝，退出码 1。随后串行执行并设置进程 TEMP／TMP 为 C:\jtmp、max-workers=1，退出码 0；无法仅凭此次重跑判定环境故障的唯一原因。
- 改进：接管 tooltip 前先核验其他监听器实际绘制与取消的优先级，避免只调整模糊半径或文字 Z 层；源码／字节码检查不代替 GPU 实机验收。
