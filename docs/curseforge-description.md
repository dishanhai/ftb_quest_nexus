# CurseForge Release Metadata — FTB Quest Nexus

## Summary (One-Line Preview)

> Automate FTB Quests submissions, stock checks, and rewards with AE2. Complete energy tasks with FE.

## Description (English First + 简体中文)

# FTB Quest Nexus

*English | [简体中文](#简体中文)*

**FTB Quest Nexus** connects **FTB Quests** to **Applied Energistics 2 (AE2)**. The submitter takes resources from ME storage to advance consumable tasks. The checker completes stock-detection tasks without taking resources and accepts FE for energy tasks. An optional client theme adds frosted glass to the quest book.

**Supported platforms** · Minecraft **1.20.1 Forge** and **1.21.1 NeoForge**.

### AE Auto Submitter

`ftb_quest_nexus:ftbq_ae_submitter`

- Connect any face to a powered ME network with a free channel, then bind the block to your player and FTB Teams team. Use the quest details screen to choose a team submitter and queue eligible item and fluid goals. XP rewards can also be queued for quests with only non-consumable goals.
- Turn on **Auto-Match** to submit eligible unlocked goals from ME storage without queueing them by hand. Search the submitter's queue and browse its progress by page.
- Supported item rewards go to ME storage, including item-only random, loot, and all-table rewards. XP points and levels go to the bound player when online. For choice rewards, the bound player can open a single selection screen for multiple quests from a private chat message. FTB Quests handles the selected rewards.
- The submitter saves item rewards that ME cannot accept and retries delivery. Normal mining preserves the binding, queue, settings, and pending deliveries. An ownership transfer to a teammate requires the current owner's approval within 60 seconds.
- Craft with a **book**, **ME Interface**, and **Logic Processor**, in that order down a crafting column.

### ME FTB Quest Checker

`ftb_quest_nexus:ftbq_ae_checker`

- Place the checker to bind it to yourself and your team. It reads ME stock once per second and completes eligible non-consumable item and fluid goals. Native fluid storage and filled buckets both count toward fluid stock, at **1,000 mB per bucket**. Matching bulk fluid can also count as whole buckets for eligible bucket item goals.
- The glass panel shows stock against task requirements and supports search, type and status filters, sorting, and pagination.
- Feed **Forge Energy (FE)** into the checker to advance eligible FTB Quests energy goals, even without an ME connection. On **Forge**, you can also join an optional **Flux Networks** network directly from the checker menu. On **NeoForge**, connect an FE output such as an **Applied Flux** Flux Accessor in output mode or a **Flux Networks** Flux Point.
- Craft with a **book**, **ME Interface**, and **Calculation Processor**, in that order down a crafting column.

### Mekanism Chemicals (NeoForge 1.21.1)

- With **Mekanism**, modpacks can configure chemical tasks and rewards. Players can click a chemical task in the quest book to check or submit chemicals from tanks in their inventory, according to the task's settings.
- Add **Applied Mekanistics** for automatic submission of consumable chemical goals, read-only checks of non-consumable chemical goals, and delivery of chemical rewards to ME storage.
- If ME cannot accept the full reward, the remaining amount stays pending for that player. Open it from chat or with `/nexus_chemical_rewards` to retry or claim the remainder in tanks.

### Optional Frosted-Glass Theme

The quest book uses the original appearance by default. To enable glass, edit `config/ftb_quest_nexus-client.toml`, then reopen the quest book.

```toml
[ftbqUi]
mode = "GLASS" # ORIGINAL or GLASS
opacity = 1.0  # 0.0 to 1.0
blurRadius = 8 # 0 to 16, with 0 disabling blur
```

The theme covers the quest canvas, chapter sidebar, toolbar, quest details, and related dialogs while retaining modpack backgrounds, task icons, and quest links.

### Installation and Dependencies

Use **Java 21**. For multiplayer, install the same Nexus build and its required mods on the server and on each client.

| Minimum version | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| Mod loader | Forge 47.4.16 | NeoForge 21.1.251 |
| Applied Energistics 2 | 15.4.10 | 19.2.18 |
| FTB Quests | 2001.4.22 | 2101.1.36 |
| FTB Teams | 2001.3.2 | 2101.1.11 |
| FTB Library | 2001.2.13 | 2101.1.37 |
| SH-Better-UI | 1.0.6 | 1.0.6 |

Also install **Architectury API** for the FTB mods, **GuideME** for AE2, and the loader-specific **Kotlin for Forge** runtime for SH-Better-UI.

**Optional** · Jade for block tooltips, JEI for recipes and item lookup, and the energy and chemical integrations listed above.

---

# 简体中文

**FTB Quest Nexus** 将 **FTB Quests** 与 **Applied Energistics 2（AE2）** 接入同一套任务流程。提交器从 ME 库存扣除资源推进消耗型任务，检查器读取库存完成检测型任务，也能接收 FE 推进能量任务。任务书可选用毛玻璃主题。

**支持平台** · Minecraft **1.20.1 Forge** 和 **1.21.1 NeoForge**。

### AE 自动提交器

`ftb_quest_nexus:ftbq_ae_submitter`

- 六面均可连接有电力和空闲频道的 ME 网络。绑定玩家及其 FTB Teams 队伍后，可在任务详情页选择本队提交器，登记符合条件的物品和流体目标。只有非消耗型目标的任务也可登记领取经验奖励。
- 开启「匹配」后，提交器会从 ME 库存自动提交符合条件的已解锁目标。队列支持搜索和分页，可查看各目标的提交进度。
- 支持回网的物品奖励自动存入 ME，包括纯物品随机、战利品和全表奖励。经验值与经验等级在绑定玩家在线时发放。遇到自选奖励，绑定玩家可从私人聊天消息打开批量选择页，一次选择多个任务的奖励，由 FTB Quests 按原有规则发放。
- ME 收不下的物品奖励会保存在提交器中，后续优先补送。正常挖掘保留绑定、队列、设置和待交付奖励。同队移交需要当前绑定者在 60 秒内确认。
- 合成时在工作台同一列从上到下放入**书**、**ME 接口**和**逻辑处理器**。

### ME FTB 任务检查器

`ftb_quest_nexus:ftbq_ae_checker`

- 放置时绑定放置者及其队伍。每秒读取一次 ME 库存，库存达标时完成符合条件的非消耗型物品和流体目标。流体统计合计散装流体和实体流体桶，每桶按 **1,000 mB** 计算。符合条件的流体桶物品目标也可将相应的散装流体折算成整桶计数。
- 玻璃面板显示当前库存与任务需求，支持搜索、类型与状态筛选、排序及翻页。
- 向检查器输入 **FE** 可推进符合条件的 FTB Quests 能量目标，断开 ME 网络时也能提交能量。**Forge** 可从检查器菜单直接加入 **Flux Networks** 通量网络。**NeoForge** 可接入 FE 输出设备，例如输出模式下的 **Applied Flux** 通量访问点，或 **Flux Networks** 通量网络接入点。
- 合成时在工作台同一列从上到下放入**书**、**ME 接口**和**运算处理器**。

### Mekanism 化学品联动（NeoForge 1.21.1）

- 安装 **Mekanism** 后，整合包可配置化学品任务和奖励。点击任务书中的化学品目标，可按任务设置检测或消耗背包储罐中的化学品。
- 同时安装 **Applied Mekanistics** 后，可从 ME 自动提交消耗型化学品目标、只读检查非消耗型化学品目标，并优先将化学品奖励存入 ME。
- ME 未收完的化学品会留在玩家的待领取记录中。点击聊天提示或执行 `/nexus_chemical_rewards`，可重试回网或将余量罐装领取。

### 可选的任务书毛玻璃主题

默认保留原版外观。编辑客户端的 `config/ftb_quest_nexus-client.toml` 后，重新打开任务书即可应用。

```toml
[ftbqUi]
mode = "GLASS" # ORIGINAL 为原版，GLASS 为毛玻璃
opacity = 1.0  # 0.0 至 1.0
blurRadius = 8 # 0 至 16，设为 0 关闭模糊
```

主题覆盖任务画布、章节栏、工具栏、任务详情及相关对话框，并保留整合包的自定义背景、任务图标和连线。

### 安装与依赖

使用 **Java 21**。联机时，服务端和客户端均需安装同一构建的 Nexus 及其必需前置。

| 最低版本 | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| 模组加载器 | Forge 47.4.16 | NeoForge 21.1.251 |
| Applied Energistics 2 | 15.4.10 | 19.2.18 |
| FTB Quests | 2001.4.22 | 2101.1.36 |
| FTB Teams | 2001.3.2 | 2101.1.11 |
| FTB Library | 2001.2.13 | 2101.1.37 |
| SH-Better-UI | 1.0.6 | 1.0.6 |

另需安装 FTB 系列所需的 **Architectury API**、AE2 所需的 **GuideME**，以及 SH-Better-UI 所需的对应平台 **Kotlin for Forge**。

**可选联动** · Jade 方块信息提示、JEI 配方与物品查询，以及上文的能量和化学品联动。

---

## Changelog — v1.1.0

- The checker accepts FE for eligible energy tasks even when disconnected from ME. On Forge, its menu can connect directly to a Flux Networks network.
- Item-only all-table rewards send every entry to ME. The submitter saves pending deliveries, and the bound player can select choice rewards from several quests on one screen.
- On NeoForge, Mekanism chemical tasks support inventory tanks. With Applied Mekanistics, the submitter and checker can also use ME chemical storage. Chemical rewards that ME cannot accept can be claimed in tanks.
- The quest details screen loads team submitters page by page. The submitter and checker screens have updated icons for the queue, rewards, and task filters.

For multiplayer, update the server and all clients to v1.1.0 together.

## v1.1.0 更新日志（简体中文）

- 检查器断开 ME 时仍可接收 FE，推进符合条件的能量任务。Forge 可从检查器菜单直接加入 Flux Networks 通量网络。
- 纯物品全表奖励的每个条目均可回网。提交器保存待交付奖励，绑定玩家可在同一页面选择多个任务的自选奖励。
- NeoForge 的 Mekanism 化学品任务支持使用背包储罐。安装 Applied Mekanistics 后，提交器和检查器也可使用 ME 化学品库存。ME 未接收的化学品奖励可罐装领取。
- 任务详情页按页加载队伍提交器列表。提交器和检查器界面更新了队列、奖励和任务筛选图标。

联机时，请将服务端和所有客户端一并更新到 v1.1.0。
