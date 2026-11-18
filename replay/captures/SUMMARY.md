# 录包汇总（滚动）

本地原始文件（gitignore）：`replay/captures/*.jsonl`  
标签纠正：[`LABEL_CORRECTIONS.md`](LABEL_CORRECTIONS.md)  
实施计划：[`../../docs/detections/implementation-plan.md`](../../docs/detections/implementation-plan.md)

## 证据水位（2026-11-18 晚）

| 检查 | 状态 | 说明 |
|---|---|---|
| AimSnap | **已上线告警** | B2 ≥20°≈30.7%；需近 3s 内有实体攻击 |
| AimAttackAlign | **已上线告警** | A1 mean≈11.4° vs B1≈4.2°；mean≤8° 且 tight≥6/窗 |
| AuraSwitch | **已上线告警** | 同簇(120ms) ≥3 目标且含 ≥60° 视野外命中；横扫不再误报 |
| 分级处罚 | **已实现，默认关** | 告警 → 取消伤害(vl≥12) → 踢出(vl≥24) |

## 批次 2026-11-18（首次带 attack 全矩阵）

| 文件 | label | attacks | align p50 | 备注 |
|---|---|---:|---|---|
| `20261118-133910_…_A1-legit-melee.jsonl` | A1 合法 | **44** | 11.89° | Align 误报基线（关键） |
| `20261118-134526_…_C1-ka-max3.jsonl` | C1 max3 | **159** | 35.44° | 8 个 targetId；Aura 数据源 |
| `20261118-134735_…_B2-ka-onhit.jsonl` | B2 OnHit | **59** | 45.93° | ≥20° 占 30.7%；Snap 数据源 |

> 采集来源全为 `damage`（含取消的伤害也记，`damaged=false` 标注）。

## AuraSwitch 误报修订（2026-11-18）

旧规则「1s ≥3 distinct」把**正常横扫**判成光环。  
现规则：同簇 120ms 内 ≥3 目标 **且** 其中至少一次 `alignDeg ≥ 60°`（视野外目标）。

回放：A1/B1/B2/C2 → 0；C1 → 25 触发簇。

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

补录已完成（A1 / B2 / C1 均带 attack）。  
代码侧已实现 AimSnap / Align / AuraSwitch + 分级处罚，见 [`implementation-plan.md`](../../docs/detections/implementation-plan.md)。  
后续若要扩展：GCD 需同玩家多会话灵敏度样本；Aura 若仍误报可再录「大面积横扫」对照。  
