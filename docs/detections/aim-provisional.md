# Aim* 阈值（provisional）

数据：[`../rea/meteor/batch-2026-11-07.md`](../rea/meteor/batch-2026-11-07.md)（look）+ 2026-11-17 attack 场（见 [`../../replay/captures/SUMMARY.md`](../../replay/captures/SUMMARY.md)）。  
实施顺序：[`implementation-plan.md`](implementation-plan.md)。  
原则：VL + 衰减；默认只告警；踢人开关默认关（见 HANDOFF）。

## AimSnap（look 窗）— 可实现

| 参数 | provisional | 依据 |
|---|---|---|
| 窗长 | 最近 **40** 个 hasRot look | ~2s@20Hz 量级，可配置 |
| 大转定义 | \|dYaw\| **≥ 20°** | A1 仅 2.2% 超过；B2 29% |
| 加分条件 | 窗内大转占比 **≥ 12%**，或窗内 p90(\|dYaw\|) **≥ 25°** | 介于 A1 与 B2；避开 B1 常态跟踪 |
| VL | +1 / 触发窗；连续 **3** 窗才告警 | 中缓冲 |
| 衰减 | 无战斗 5s 清零或 −1/2s | |
| 豁免 | 传送后 2s、载具、橡皮筋大幅度拉回 | |

**预期**：默认可抓 **OnHit**；对 **Always** 可能偏弱（跟踪时 Δ 小）→ 必须叠加 Align。

## AimAttackAlign — 骨架 + 作弊侧参考（非正式阈值）

| 样本 | alignDeg p50 | min | n |
|---|---:|---:|---:|
| B1 Always（2026-11-17） | 3.95° | 0.19° | 82 |
| C2 only-on-look（2026-11-17） | 5.40° | 0.00° | 67 |
| A1 合法 attack | **缺** | — | — |

**Blocked（处罚阈值）**：无合法 A1 attack 前，Align **只打 debug/低 VL 日志**，不升中档告警、不处罚。

## AuraSwitch — 阻塞

缺带 `attack` + 多 `targetId` 的 C1。接口可预留；短窗 distinct targetId ≥2 的加分逻辑等补录后再填。

## 处罚档（与累计设计一致）

| VL 档 | 动作（默认） |
|---|---|
| 低 | 仅 debug 日志 |
| 中 | 向 OP/`linantiaim.alerts` 告警 + 记片段 |
| 高 | 可配置取消伤害 |
| 极高 | 可配置踢出（**默认 false**） |

单 Check 升温不够时：要求 `AimSnap` 与 `AimAttackAlign` **同时**处于中档以上再升「高」。

## 明确不做（本阶段）

- 不根据单次大转踢人  
- 不在无合法 Align 基线时对 Always 默认配置宣称「可稳定处罚」  

