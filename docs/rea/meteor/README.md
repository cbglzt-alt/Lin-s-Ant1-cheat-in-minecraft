# REA · Meteor Client 0.5.8（MC 1.21 / 1.21.1）— Phase 1 锁定

| 项 | 值 |
|---|---|
| **锁定 jar** | `cheat-mod/meteor-client-0.5.8-1.21.1.jar` |
| 版本 | **0.5.8**（depends `1.21` / `1.21.1`） |
| 上游 tag | [`v0.5.8`](https://github.com/MeteorDevelopment/meteor-client/releases/tag/v0.5.8) @ `37c5c5c6` |
| 私服 | 简幻欢 Spigot **1.21** · `play.simpfun.cn:17184` |
| 备选 jar | `cheat-mod/` 内 0.5.9 / 26.3 **仅存档，无分析卡、不联机** |

本目录是 Phase 1 **唯一** REA 文档树。曾用 0.5.9 做过草稿对照：`KillAura` / 外层 `Rotations` 等与 0.5.8 **字节码相同**；`Rotations$Rotation.sendPacket` 在 0.5.9 多一个 `horizontalCollision` 参数（1.21.2+ 映射），不影响瞄准机制结论。动态阈值必须以 **0.5.8 真机** 录包为准。

## 门禁进度

```
☑ 锁定 Meteor 0.5.8（对齐 Spigot 1.21）
☑ KillAura + Rotations（+ BowAimbot / Hitboxes）静态分析卡
☑ 与 0.5.9 瞄准类字节码核对（结论：主路径等价）
☑ 动态录包 look 矩阵 A1/B1/B2/B3/C1（2026-11-07）
☑ look 向 §4 + AimSnap provisional
☑ 补录带 attack 的 B1 + 真 C2（2026-11-17）
☑ 下一步计划：检测 + 录包对照（见 detections/implementation-plan.md）
□ 补录带 attack 的 A1（Align 误报线）/ C1（AuraSwitch）
□ 实现 AimSnap 告警-only + Align 日志骨架（与 /ac record 并行）
```

## 产出

| 文件 | 状态 |
|---|---|
| [`killaura.md`](killaura.md) | P0 静+动（look）；Align 有作弊侧样本、缺合法 attack |
| [`rotations.md`](rotations.md) | P0 静+动（look） |
| [`captures.md`](captures.md) / [`batch-2026-11-07.md`](batch-2026-11-07.md) | 矩阵 + 11-07 汇总 |
| [`../../replay/captures/SUMMARY.md`](../../replay/captures/SUMMARY.md) | 滚动汇总（含 11-17） |
| [`../detections/aim-provisional.md`](../detections/aim-provisional.md) | AimSnap 候选阈值 |
| [`../detections/implementation-plan.md`](../detections/implementation-plan.md) | **S3 开工计划（下一步接这里）** |
| [`bowaimbot.md`](bowaimbot.md) / [`hitboxes.md`](hitboxes.md) | P1 静态 |

模板：[`../TEMPLATE.md`](../TEMPLATE.md)。  
插件：`plugin/`（录包已有；检测按 implementation-plan 加）。
