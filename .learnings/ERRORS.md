# Author: dishanhai — fork verification errors

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
