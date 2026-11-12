# FTB Quest Nexus

[简体中文](README.md) · [繁體中文](README.zh-TW.md) · **English**

FTB Quest Nexus extends FTB Quests on **Minecraft Forge 1.20.1**. The AE Auto Submitter was its first feature. Additional features are organized as separate modules.

The standalone **NeoForge 1.21.1** source and build entry point are in [neoforge/](neoforge/). Both versions share the same mod ID. Install the jar for your platform. NeoForge uses the separate NeoForge build of SH-Better-UI. The root project builds the Forge 1.20.1 version.

## AE Auto Submitter

The submitter and ME FTB Quest Checker appear in the vanilla Functional Blocks creative tab and creative search. JEI discovers both through those entries.

Block ID `ftb_quest_nexus:ftbq_ae_submitter`.

1. Connect the submitter to a powered AE network with an available channel. All six faces support connections.
2. Open the interface and select Bind to bind the current player and their FTB Teams team.
3. In the FTB Quests quest details, select a submitter on your team. You can queue the quest's consumable item and fluid tasks together. For a quest with an XP reward and no consumable tasks, use the XP reward registration action to queue the reward for collection after completion.
4. The submitter checks once per second and advances progress by the number of items or millibuckets of fluid actually extracted from AE. Craft-only and non-consumable tasks use the standard quest flow.
5. Eligible item rewards go to the AE network. Direct XP and experience level rewards go to the currently bound player and wait for that player to come online. The Rewards setting switches between queued quests and all quests. New submitters default to queued quests. The notification toggle controls completion messages triggered by this mod's automatic submissions.

Matching is off by default. When enabled, the submitter matches unlocked consumable tasks against AE stock without manual registration. The list shows tasks that have received submissions in this mode, and the submitter claims AE-compatible rewards only for their quests. Turning matching off restores the manual queue and its reward settings.

The submitter handles ordinary item rewards and nested random or loot reward tables made entirely of items. Choice rewards and tables with non-item outcomes such as commands or XP require manual claiming. An empty roll counts as claimed.

Repeatable quests follow FTBQ's native reward reset rules. The submitter delivers the current round of AE rewards before resuming the remaining native item auto-claims, so a teammate's claim cannot reset those AE rewards first. See the [review fixes and verification notes](docs/review-fixes-verification.md).

Rewards that do not fit in AE remain buffered in the block and are retried first. While delivery is pending, new submissions, reward claims, and rebinding pause. Mining the block preserves its binding, queue, settings, and buffered rewards. Creative pick block gives a fresh submitter. A teammate can select Bind to request a transfer. The current owner receives a private in-game message and has 60 seconds to approve or decline. An offline owner must come online before a transfer can be requested. If the reward recipient leaves the team, the submitter pauses until the transfer to a remaining team member is approved. The queue is retained.

Craft the submitter with a book, an ME Interface, and a Logic Processor in a vertical column in that order.

## ME FTB Quest Checker

Created by 山海 / dishanhai. Block ID `ftb_quest_nexus:ftbq_ae_checker`. Available on Forge 1.20.1 and NeoForge 1.21.1.

On placement, the checker binds to the placing player's UUID and their FTBQ team's UUID. It connects to AE on all six faces and requires power and an available channel. Every second, it checks all unlocked item and fluid tasks using a single read-only stock snapshot. Non-consumable tasks complete when stock meets the full requirement. For consumable tasks, the checker displays stock and the submitter advances progress by extracting resources. The checker respects team locks, quest dependencies, task order, and repeat cooldowns. Craft-only tasks are excluded.

Fluid totals include native AE fluid storage and filled buckets in item storage at 1,000 mB per bucket. Filled buckets can also match item tasks. Fluid NBT or data components and item matching follow the APIs for each platform version. The checker reads stock without extracting items or draining buckets. Rewards follow standard FTBQ rules.

The interface uses the public `UiButton` API and glass panels from SH-Better-UI. It displays binding, network status, and current stock against each task's total requirement. Pages show six rows and can be changed with buttons or the mouse wheel. Each viewer has their own page. Members of the bound team can manage the checker. Transfers require the current owner's approval through an in-game message. If the bound player leaves the team, the checker pauses until a transfer to a remaining team member is approved. The world save retains binding and pause state. Mining drops a fresh checker that binds to its next placer.

Craft the checker with a book, an ME Interface, and a Calculation Processor in a vertical column in that order. The light gray AE casing uses purple indicators, vents, and a core to distinguish it from the submitter. Online and offline textures cover all six faces with native 16 × 16 px frames. The animation uses eight frames. The editable texture generator is `tools/GenerateCheckerTextures.java`, and the layout preview generator is `tools/CheckerUiPreview.java`.

Use a build of SH-Better-UI for your platform that exposes the public `UiButton` API. Client checks for all six faces, animation, glass blur, tooltips, keyboard input, and mouse wheel input are tracked in [TODO.md](TODO.md).

## Installation and build

Forge runtime requirements are **Java 21**, Minecraft 1.20.1, and Forge 47.4.16. See the [NeoForge README](neoforge/README.md) for the NeoForge build.

Locally verified dependencies include AE2 15.4.10, FTB Quests 2001.4.22, FTB Teams 2001.3.2, FTB Library 2001.2.13, Architectury 9.2.14, GuideME 20.1.15, SH-Better-UI 1.0.6, and KotlinForForge 4.12.0. Jade 11.13.3 provides optional block information.

**SH-Better-UI is required.** Its separate jar provides glass panels, blur, tooltips, and animations. Development builds use `../SH-Better-UI/build/libs/sh_better_ui-1.0.6-dev.jar` by default. Set `-PshBetterUiJar=<dev-jar>` to override the path. Rebuild SH-Better-UI's `devJar` to pick up changes from that project.

Install the release SH-Better-UI jar and its KotlinForForge dependency separately in Minecraft. FTB Quest Nexus itself uses Java and ships its dependencies separately. Other development dependencies are in `libs/`, with local copies of runtime jars in `libs/runtime/`. The mod runs without GTCEu, GTLCore, GTLAdditions, KubeJS, or LDLib.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:TEMP = 'C:\jtmp'
$env:TMP = 'C:\jtmp'
.\gradlew.bat test build reobfJar --no-daemon --console=plain
```

Run `gradlew.bat build` from the repository root to build Forge and then NeoForge. Commands that name `reobfJar` or `extractNatives` run on Forge only, including when `-x` excludes those tasks. See the [NeoForge build instructions](neoforge/README.md) for standalone commands.

Add `--offline` if the local dependency cache is complete. On a new machine, populate the ForgeGradle, mappings, and test dependency caches. The Forge wrapper uses Gradle 8.8.

Install the release artifact at `build/libs/ftb_quest_nexus-1.1.0.jar` and the release version of SH-Better-UI.

Run dedicated server GameTests with the following command.

```powershell
.\gradlew.bat runGameTestServer --offline --no-daemon --console=plain -x downloadAssets -x extractNatives
```

The tests use the project's own `run/` directory. See the [verification results](docs/verification.md) and [pending in-game checks](TODO.md).

## FTBQ glass theme

Both platforms support a client-side FTBQ appearance setting in `config/ftb_quest_nexus-client.toml`. The default is `ORIGINAL`. The following example enables glass.

```toml
[ftbqUi]
mode = "GLASS"
opacity = 1.0
blurRadius = 8
```

`mode` accepts `ORIGINAL` or `GLASS`. `opacity` ranges from 0 to 1 and scales the tint and border opacity of the already translucent material. A value of 0 hides the glass surfaces. `blurRadius` is an integer from 0 to 16 in GUI units, with 0 disabling background blur. Close and reopen the relevant screen after changing settings. Dialogs opened by the quest screen inherit its settings snapshot.

The theme covers the main canvas, chapter sidebar, toolbar, quest details, slots, inputs, scrollbars, context menus, and FTBQ configuration and selection dialogs. Quest controls, dragging, zooming, editing, submissions, reward claims, and status colors keep their usual behavior. Modpack backgrounds, chapter images, icons, and connectors retain their original assets. The theme applies only to FTBQ screens.

SH-Better-UI samples the root glass background before drawing controls. The translucent material still renders if blur is unavailable. Subtle highlights at nonzero opacity and tooltips use the library's presets. Client-side visual and interaction checks are pending. See the [glass build and verification notes](docs/glass-verification.md).

### Functional block appearance

From the Mods list, open **FTB Quest Nexus → Config → Block Style**. Choose a theme separately for the AE task submitter and checker, then select Apply. Each can follow the current SHBUI theme or use a built-in material or resource-pack theme. Both default to transparent glass. The client saves the choices as `submitterTheme` and `checkerTheme` under `[ftbqUi]`. If a resource-pack theme is unavailable, the screen uses transparent glass and keeps the saved theme ID.

## Origin and attribution

The project was forked from `gt_shanhai:ftbq_ae_submitter` and carries over its submission and reward delivery behavior, glass appearance, and animations. The original project retains its code, registrations, and recipes. Each block uses its own ID, queue, network channels, and nodes, and existing saves keep their original blocks. Both mods expose their own quest registration actions when installed together. In-game coexistence checks are pending.

The project name and mod ID are **FTB Quest Nexus / `ftb_quest_nexus`**. See [source and attribution](SOURCE.md).
