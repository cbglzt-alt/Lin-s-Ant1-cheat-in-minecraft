# 录包汇总（滚动）

本地原始文件（gitignore）：`replay/captures/*.jsonl`  
标签纠正：[`LABEL_CORRECTIONS.md`](LABEL_CORRECTIONS.md)  
实施计划：[`../../docs/detections/implementation-plan.md`](../../docs/detections/implementation-plan.md)

## 证据水位（2026-11-18）

| 检查 | 状态 | 说明 |
|---|---|---|
| AimSnap | **可实现告警** | 11-07 A1 vs B2 look |
| AimAttackAlign | **骨架 only** | 11-17 有作弊 B1/C2 attack；缺合法 A1 attack |
| AuraSwitch | **阻塞** | 缺带 attack 的 C1 |

## 批次 2026-11-17（有 attack）

| 文件 | label | attacks | alignDeg p50/min | 备注 |
|---|---|---:|---|---|
| `20261117-145042_…_B1-ka-always-default.jsonl` | B1 Always | **82** | 3.95° / 0.19° | Align 作弊侧 |
| `20261117-145333_…_C2-ka-only-on-look.jsonl` | C2 only-on-look | **67** | 5.40° / 0.00° | Align 变体 |

## 批次 2026-11-07（仅 look；B1↔C1 已纠正）

详见 [`../../docs/rea/meteor/batch-2026-11-07.md`](../../docs/rea/meteor/batch-2026-11-07.md)。

| 入库含义 | 下载时文件名（易混） | attacks | \|dYaw\|≥20° |
|---|---|---:|---:|
| A1 合法 | A1-legit-melee | 0 | 2.2% |
| B1 Always 单目标 | 原 C2/C1 文件名 | 0 | 5.6% |
| B2 OnHit | B2-ka-onhit | 0 | **29.3%** |
| B3 None | B3-ka-none | 0 | 3.1% |
| C1 max3 | 原 B1 文件名 | 0 | 4.7% |

本批 **attacks 全 0** → 不能定 Align / AuraSwitch。

## 作废

| 文件 | 原因 |
|---|---|
| `20261102-144607_…_B1-…jsonl` | attacks=0 且早期无效场 |

## 下一步（数据）

1. **P0** 补录带 attack 的 `A1-legit-melee`  
2. **P1** 补录带 attack 的 `C1-ka-max3`  
3. 代码侧按 [`implementation-plan.md`](../../docs/detections/implementation-plan.md) 做 AimSnap + 录包对照日志  
