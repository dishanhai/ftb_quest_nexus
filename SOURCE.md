# Source and attribution

Author: dishanhai（山海恒长在）。项目名称：FTB Quest Nexus；mod ID：`ftb_quest_nexus`。

本项目在原作者授权的本地工作区中，由 `C:\Users\dishanhai\Desktop\gt_shanhai` 的 FTBQ AE 自动提交器功能 fork。原项目当前参考提交：`590348e209f8892632eae89d6f894bf235435fe3`（2026-11-07 核验）。原项目未删除或替换。

## 复用与改编

- `FtbqAeSubmitterMachine.java`：任务匹配、真实 AE 扣料、任务进度、物品奖励树验证、奖励领取标记与待交付缓冲。改编为原生 Forge `SubmitterBlockEntity` 和 `QuestWork`。
- `api/ftbq/FtbqRewardBuffer.java` 及其测试：仅调整包名，保留余量交付算法。
- `api/ftbq/FtbqSubmitterToastSuppressor.java`：局部提示抑制，配合只针对提交器的 TeamData Mixin。
- 三个 `Ftbq*Packet.java`、`FtbqSubmitterSelectionClient.java`：登记协议与菜单，改用独立网络通道、显式方向和新的方块实体。
- 任务详情页登记 Mixin：保留登记入口，增加与其他顶栏按钮的布局协调。
- 提交器玻璃面板：改为原生 Screen，使用外部 SH-Better-UI API，保留原尺寸、按钮、模糊与 UUID 悬停语义。
- 原 `textures/block/casings/ftbq_ae_submitter_casing.png` 及 `.mcmeta`；`textures/block/machine/part/ftbq_ae_submitter/` 的前面、在线前面与发光 PNG／动画元数据，原样复制，仅资源命名空间改变。
- 原配方、语言、指南中的相关说明：独立命名空间并移除 KubeJS 配方材料。

## 外部依赖

Forge、AE2、FTB Quests、FTB Teams、FTB Library、Architectury、GuideME、Jade、SH-Better-UI 及 KotlinForForge 的 jar 仅作为本机编译／运行依赖。各依赖保留自身的许可，不包含在本模组主 jar 中；`libs/` 不作为本项目源码发布内容。

本次交付不建立远端仓库或发布依赖文件。原作者文件沿用 All Rights Reserved，见 LICENSE。
