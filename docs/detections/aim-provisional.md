# Aim* 阈值（provisional）· 2026-11-18

数据：`replay/captures/` 中带 attack 的 A1/B1/B2/C1/C2 + 11-07 look。  
原则：VL + 衰减；**默认只告警**；踢人/取消伤害默认关。

## 样本摘要

| 场 | align p50 / mean10 p50 | \|dYaw\|≥20° | 1s 内不同 targetId |
|---|---|---:|---:|
| A1 合法 | 11.9° / 11.4° | 0% | 1 |
| B1 Always | 3.95° / 4.2° | ~0% | 1 |
| B2 OnHit | 45.9° / 49° | **30.7%** | 1 |
| C1 max3 | 35.4° / 46° | 12.8% | **7** |
| C2 only-on-look | 5.4° / 6.0° | 1.9% | 1 |

## AimSnap

| 参数 | 值 | 依据 |
|---|---|---|
| window / big_yaw | 40 / ≥20° | A1 0% vs B2 30.7% |
| big_ratio / p90 | ≥12% / ≥25° | 同左 |
| streak | 3 | 缓冲 |
| **require_recent_attack_ms** | **3000** | 避免挖树/闲逛误报 |

预期：抓 **OnHit**；Always 主要靠 Align。

## AimAttackAlign

| 参数 | 值 | 依据 |
|---|---|---|
| window / min_samples | 20 / 10 | |
| mean_max_deg | **8.0** | A1 mean10≈11.4；B1≈4.2 |
| tight_deg / tight_min_count | 8.0 / 6 | A1 几乎不满足；B1 常满足 |
| streak | 2 | |

预期：抓 **Always / only-on-look**；不抓 OnHit/多目标乱切（align 大）。

## AuraSwitch

原规则「1 秒内 ≥3 个不同 targetId」会误判**正常横扫**（一次挥击打到多个怪，它们都在视野前方）。

现行规则（2026-11-18 修订）：

| 参数 | 值 | 依据 |
|---|---|---|
| cluster_ms | **120** | 一次挥击的多目标命中落在这个窗内 |
| min_distinct | **3** | |
| wild_align_deg | **60°** | 横扫的每个目标都在视野内（align 小）；光环会打视野外的目标 |

回放验证：A1/B1/B2/C2 → **0** 触发；C1 → 25 个触发簇。

> 若仍有横扫误报，把 `min_distinct` 提到 4，或把 `cluster_ms` 降到 80。

## 处罚

分档见 [`punishment.md`](punishment.md)：VL 累计 → 告警（自动）→ 取消伤害 → 踢出。  
默认 `cancel_damage=false`、`kick=false`；先观察 VL 曲线再逐档开启。
