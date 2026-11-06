# 构建与资源核验

2026-11-07，作者 dishanhai。

- ForgeGradle/Parchment 及六个本机依赖经过离线配置和 Java 编译解析；首轮仅因同时实施中的 BE 注册表 import 尚未统一而失败，后续完整验证由整合步骤执行。
- 真实 jar 元数据确认 AE2 15.4.10 强制 GuideME [20.1.7,20.2)，采用实际存在的 20.1.15。
- SH-Better-UI 1.0.3 强制外部 KotlinForForge [4.12.0,5)，故运行依赖提供 4.12.0；本项目本身仅 Java、不嵌入 Kotlin 或其他模组。
- 原始所有提交器 PNG 与 mcmeta 复制到新命名空间：外壳、前面、在线前面及发光。模型为 cutout，六向朝向、online 状态共 12 变体，发光面用 ForgeFaceData block_light/sky_light=15。
- Forge 47.4.16 sources.jar 的 ForgeFaceData.java 核验了 forge_data 的字段名称；仍需 Minecraft 客户端实机核对纹理透明、发光与方向。
- 掉落表仅复制 SubmitterData 到 BlockEntityTag.SubmitterData，避免 AE 节点身份被复制。审查发现 Ctrl+中键会绕过普通 pick 方法调用客户端 saveWithFullMetadata；方块实体现拒绝从客户端快照导出用户数据，普通／Ctrl pick 都不能复制 UUID、绑定或奖励。客户端实机操作仍待验收。

工具自我修正：apply_patch 同一补丁不能对同一路径同时 Delete/Add；改为 Update 后模型已成功写入。没有删除其他协作文件。
