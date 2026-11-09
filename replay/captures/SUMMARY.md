# 录包批次 2026-11-07

本地原始文件（gitignore）：`replay/captures/*.jsonl`  
标签纠正说明：`replay/captures/LABEL_CORRECTIONS.md`

## 标签纠正

操作者说明：**B1 / C1 录反**。

| 入库 label | 含义 | 下载时文件名 |
|---|---|---|
| `A1-legit-melee` | 合法近战 | 同名 |
| `B1-ka-always-default` | **单目标** Always 默认 | 原 `C1-ka-only-on-look`（session 曾写 `C2-ka-only-on-look`） |
| `B2-ka-onhit` | OnHit | 同名 |
| `B3-ka-none` | None | 同名 |
| `C1-ka-max3` | **多目标** | 原 `B1-ka-always-default` |

本批未得到可信的 `C2-ka-only-on-look`。

## 汇总表

| label | rotLooks | swings | attacks | \|dYaw\| p50/p90/p99 | \|dYaw\|≥20° |
|---|---:|---:|---:|---|---:|
| A1-legit-melee | 780 | 45 | 0 | 0.83 / 6.02 / 31.32 | 2.2% |
| B1-ka-always-default | 712 | 54 | 0 | 1.55 / 7.05 / 136.27 | 5.6% |
| B2-ka-onhit | 874 | 218 | 0 | 4.88 / 64.94 / 170.41 | **29.3%** |
| B3-ka-none | 655 | 53 | 0 | 1.65 / 6.60 / 42.45 | 3.1% |
| C1-ka-max3 | 928 | 70 | 0 | 1.65 / 9.08 / 91.73 | 4.7% |

## 数据质量

1. Session 行曾有 `"protocol":c2s-...` 未加引号（已修插件 `JsonLine`）。
2. **全部 attacks=0**：PacketEvents `INTERACT_ENTITY` 本环境未采到；swings 正常。已加 `BukkitAttackListener`（`EntityDamageByEntityEvent`）作回退。
3. 因此本批不能定 `AimAttackAlign` / `AuraSwitch` 阈值。

## 下一步

1. 部署修复后的 `LinAntiAim.jar`，重启实例。  
2. 至少补录：`B1-ka-always-default`、`C1-ka-max3`（确认 `attack` 行与 `alignDeg` 非空）。  
3. 可选：真正的 `C2-ka-only-on-look`。  
4. 有 attack 数据后实现 `AimSnap`（可先用本批 OnHit 阈值作 smoke）+ `AimAttackAlign`。
