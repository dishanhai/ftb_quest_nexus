# CurseForge Release Metadata — FTB Quest Nexus

## Summary (One-Line Preview)

> Bridges FTB Quests and Applied Energistics 2 with automated item/fluid task submission, stock checking, and reward claiming.

---

## Description (English First + 简体中文)

# FTB Quest Nexus

*English | [简体中文](#简体中文)*

**FTB Quest Nexus** bridges **FTB Quests** and **Applied Energistics 2 (AE2)**, providing automated item and fluid task submission, read-only stock checking for non-consumable tasks, automatic reward collection, and an optional frosted-glass UI theme for FTB Quests.

Supported Platforms:
- **Minecraft 1.20.1** (Forge)
- **Minecraft 1.21.1** (NeoForge)

---

## ✨ Key Features

### 1. AE Auto Submitter (`ftb_quest_nexus:ftbq_ae_submitter`)
Automates consumable FTB Quests tasks and collects rewards directly through your AE2 network.
- **6-Sided AE2 Connection**: Connects to any powered AE2 network with an available channel.
- **Direct Quest Queueing**: Bind the block to your player and FTB Team, then queue consumable **Item** and **Fluid** tasks directly from the FTB Quests detail screen.
- **Auto-Match Mode**: Optional toggle that automatically matches unlocked consumable tasks against your AE2 storage without manual registration.
- **Automated Reward Collection**:
  - **Item Rewards** (including ordinary items and item-only random/loot tables) are inserted directly into your AE2 network.
  - **XP & Level Rewards** are delivered to the bound player (or queued until they come online).
  - **Overflow Protection**: Rewards that do not fit into AE2 storage are safely buffered inside the block and retried first. Mining the block preserves its binding, queue, settings, and buffered rewards.
- **Team & Ownership Safety**: Supports repeatable quests without prematurely resetting teammate rewards, and includes an in-game 60-second approval prompt for ownership transfers within a team.
- **Recipe**: Book + ME Interface + Logic Processor (arranged vertically).

### 2. ME FTB Quest Checker (`ftb_quest_nexus:ftbq_ae_checker`)
Completes non-consumable detection tasks automatically by reading your AE2 storage snapshot once per second.
- **Non-Destructive Stock Check**: Completes unlocked **non-consumable** item and fluid tasks when your AE2 stock meets the requirement—without extracting items or draining buckets.
- **Smart Fluid Counting**: Combines native AE2 fluid storage with filled fluid buckets in item storage (`1,000 mB` per bucket).
- **Progress Overview UI**: Built with **SH-Better-UI** glass panels, displaying binding status, network state, and real-time stock vs. required amounts with pagination and filters.
- **Recipe**: Book + ME Interface + Calculation Processor (arranged vertically).

### 3. Optional FTB Quests Glass Theme
Includes a client-side frosted-glass visual overhaul exclusively for FTB Quests screens (`config/ftb_quest_nexus-client.toml`):
```toml
[ftbqUi]
mode = "GLASS" # ORIGINAL or GLASS
opacity = 1.0  # 0.0 to 1.0
blurRadius = 8 # 0 to 16
```
Covers the main quest canvas, chapter sidebar, toolbar, quest details, slots, scrollbars, and dialogs while preserving custom modpack backgrounds, icons, and connectors.

---

## 📦 Dependencies

### Required
- **Applied Energistics 2 (AE2)**
- **FTB Quests**
- **FTB Teams**
- **FTB Library**
- **Architectury API**
- **SH-Better-UI** (`1.0.3+`) & **Kotlin for Forge** (required by SH-Better-UI)

### Optional
- **Jade** (In-game block tooltip integration)
- **JEI** (Recipe & item lookup)

---
---

# 简体中文

**FTB Quest Nexus** 是连接 **FTB Quests** 与 **Applied Energistics 2 (AE2)** 的扩展模组，提供消耗型物品/流体任务自动提交、非消耗型任务只读库存检测、任务奖励自动回网，以及可选的 FTB Quests 毛玻璃界面主题。

支持平台：
- **Minecraft 1.20.1** (Forge)
- **Minecraft 1.21.1** (NeoForge)

---

## ✨ 核心功能

### 1. AE 自动提交器 (`ftb_quest_nexus:ftbq_ae_submitter`)
通过 AE2 网络自动提交消耗型任务目标，并自动回收任务奖励。
- **六面接入 AE 网络**：接入有电力与可用频道的 AE2 网络即可工作。
- **任务详情页直接登记**：绑定玩家与 FTB Teams 队伍后，可在 FTB Quests 任务详情页一键登记消耗型**物品**与**流体**目标；无消耗目标的纯经验奖励任务也可单独登记领奖。
- **自动匹配模式**：开启界面上的「匹配」开关后，无需手动登记即可自动从 AE 库存匹配并提交所有已解锁的消耗型目标。
- **自动领取奖励**：
  - **物品奖励**（含普通物品及纯物品构成的随机/战利品嵌套奖励表）自动存入 AE 网络。
  - **经验值与经验等级奖励**直接发放给绑定玩家（离线时自动挂起等待上线）。
  - **满仓缓存保护**：AE 网络空间不足时，剩余奖励安全保存在方块内并优先重试回网；正常挖掘掉落完整保留绑定、队列、设置与待交付奖励。
- **队伍协同与安全移交**：兼容可重复任务重置规则，同队成员申请移交绑定需当前绑定者在 60 秒内通过游戏内消息确认。
- **合成配方**：书 + ME 接口 + 逻辑处理器（工作台竖列摆放）。

### 2. ME FTB 任务检查器 (`ftb_quest_nexus:ftbq_ae_checker`)
每秒通过只读库存快照检测 AE2 网络库存，自动完成非消耗型（检测型）任务。
- **零消耗只读检测**：当 AE 库存达到需求总量时自动完成已解锁的**非消耗型**物品与流体目标，全程不取出物品、不排空流体桶。
- **智能流体折算**：流体库存自动合计 AE 原生流体与物品库存内的流体桶（每桶按 `1000 mB` 计），桶物品本身仍可同时匹配物品任务。
- **可视化进度面板**：采用 **SH-Better-UI** 玻璃面板，直观显示绑定状态、网络状态及各目标的「当前库存 / 总需求」，支持翻页、滚轮与类型筛选。
- **合成配方**：书 + ME 接口 + 运算处理器（工作台竖列摆放）。

### 3. FTB Quests 透明毛玻璃主题（可选）
支持通过客户端配置文件 `config/ftb_quest_nexus-client.toml` 将 FTB Quests 界面切换为毛玻璃风格（默认保留原版 `ORIGINAL`）：
```toml
[ftbqUi]
mode = "GLASS" # 可选 ORIGINAL 或 GLASS
opacity = 1.0  # 0.0 ~ 1.0 不透明度缩放
blurRadius = 8 # 0 ~ 16 背景模糊半径
```
覆盖主画布、章节栏、工具栏、任务详情、槽位、输入框与右键菜单，同时完整保留整合包自定义背景、章节图标与任务连线。

---

## 📦 前置与依赖

### 必需前置
- **Applied Energistics 2 (AE2)**
- **FTB Quests**
- **FTB Teams**
- **FTB Library**
- **Architectury API**
- **SH-Better-UI** (`1.0.3+`) 及 **Kotlin for Forge**（SH-Better-UI 运行所需）

### 可选联动
- **Jade**（方块信息提示集成）
- **JEI**（配方与物品检索）

---
---

## Changelog — v1.0.0 (Initial Release)

# FTB Quest Nexus v1.0.0 — Initial Release

*English | [简体中文](#v100-更新日志简体中文)*

### ✨ New Features
- **AE Auto Submitter (`ftb_quest_nexus:ftbq_ae_submitter`)**:
  - Connects to AE2 networks on all 6 faces to automatically extract and submit consumable **Item** and **Fluid** tasks once per second.
  - Supports manual task registration from FTB Quests detail screens and an optional **Auto-Match** mode for unlocked consumable tasks.
  - Automatically collects **Item Rewards** (including nested item-only random/loot tables) into AE2 storage and delivers **XP / Level Rewards** to the bound player.
  - Includes overflow reward buffering, repeatable quest reward protection, and in-game 60-second confirmation for team ownership transfers.
  - Preserves binding, queue, settings, and buffered rewards when mined.
- **ME FTB Quest Checker (`ftb_quest_nexus:ftbq_ae_checker`)**:
  - Reads AE2 network stock once per second using a read-only snapshot to automatically complete unlocked **non-consumable** item and fluid tasks without extracting items or draining buckets.
  - Counts both native AE2 fluid storage and filled fluid buckets (`1,000 mB` per bucket).
  - Features a paginated **SH-Better-UI** glass interface with task filtering, mouse wheel scrolling, and real-time stock progress.
- **Optional FTB Quests Glass UI Theme**:
  - Added client config (`config/ftb_quest_nexus-client.toml`) with `ORIGINAL` and `GLASS` modes, adjustable `opacity` (`0.0–1.0`), and `blurRadius` (`0–16`).
- **Mod Integration & Platform Support**:
  - Added **Jade** tooltip integration for block status inspection.
  - Available for **Minecraft 1.20.1 (Forge 47.4.16+)** and **Minecraft 1.21.1 (NeoForge 21.1.251+)**.

### 📦 Requirements
- **AE2**, **FTB Quests**, **FTB Teams**, **FTB Library**, **Architectury API**, **SH-Better-UI (`1.0.3+`)**, and **Kotlin for Forge**.

---

## v1.0.0 更新日志（简体中文）

### ✨ 新增内容
- **AE 自动提交器 (`ftb_quest_nexus:ftbq_ae_submitter`)**：
  - 支持六面接入 AE2 网络，每秒自动扣除并提交消耗型**物品**与**流体**任务目标。
  - 支持在 FTB Quests 任务详情页手动登记队列，或开启「匹配」模式自动匹配已解锁的消耗型任务。
  - 自动回收**物品奖励**（含纯物品随机/战利品嵌套表）至 AE 网络，并将**经验点/经验等级奖励**发放给绑定玩家（离线时自动挂起等待上线）。
  - 内置满仓奖励缓存保护、可重复任务领奖防重置机制，以及同队成员 60 秒游戏内消息确认移交机制。
  - 正常挖掘掉落完整保留绑定、队列、设置与待交付奖励。
- **ME FTB 任务检查器 (`ftb_quest_nexus:ftbq_ae_checker`)**：
  - 每秒通过只读库存快照检测 AE2 库存，自动完成已解锁的**非消耗型**物品与流体任务，全程不取出物品、不排空流体桶。
  - 流体统计自动合并 AE 原生流体与物品库存中的流体桶（每桶 `1000 mB`）。
  - 搭载基于 **SH-Better-UI** 的毛玻璃分页界面，支持实时查看库存进度、滚轮翻页与类型筛选。
- **FTB Quests 透明毛玻璃主题（可选）**：
  - 新增客户端配置 `config/ftb_quest_nexus-client.toml`，支持切换 `ORIGINAL` / `GLASS` 模式并调节不透明度（`opacity`）与背景模糊半径（`blurRadius`）。
- **联动与跨版本支持**：
  - 集成 **Jade** 方块信息提示。
  - 同步支持 **Minecraft 1.20.1 (Forge 47.4.16+)** 与 **Minecraft 1.21.1 (NeoForge 21.1.251+)**。

### 📦 依赖要求
- 需安装 **AE2**、**FTB Quests**、**FTB Teams**、**FTB Library**、**Architectury API**、**SH-Better-UI (`1.0.3+`)** 及 **Kotlin for Forge**。
