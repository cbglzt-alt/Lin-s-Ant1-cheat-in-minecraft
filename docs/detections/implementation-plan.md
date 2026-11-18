# S3 实施计划 · 检测 + 录包对照（告警-only）

> 更新日期：2026-11-18  
> 状态：**下一步开工文档**（接在 REA look 矩阵 + 部分 attack 补录之后）

## 0. 决策

**先做检测骨架，与现有 `/ac record` 并行跑**：同一场战斗既出 capture jsonl，又出检测告警日志，方便回放对照、调阈值。  
默认 **只告警、不取消伤害、不踢人**（与 [`aim-provisional.md`](aim-provisional.md) / HANDOFF 一致）。

## 1. 证据水位（接手前必读）

| 检查 | 证据 | 本迭代目标 |
|---|---|---|
| **AimSnap** | 11-07 A1 vs B2 look 已分离（≥20°：2.2% vs 29.3%） | **实现告警-only + 真机 smoke** |
| **AimAttackAlign** | 11-17 仅有作弊侧：B1 align p50≈3.95°、C2≈5.40°；**缺合法 A1 attack** | **骨架 + debug/低 VL 日志**；不定处罚阈值 |
| **AuraSwitch** | 11-07 C1 仅 look、无 attack | **接口预留**；等补录再填 |
| AimGCD / AttackOrder | 弱 / 未做 | 本迭代不做 |

原始包：`replay/captures/`（见 [`../rea/meteor/captures.md`](../rea/meteor/captures.md)、[`../../replay/captures/SUMMARY.md`](../../replay/captures/SUMMARY.md)）。  
11-07 **B1↔C1 录反**：以 `LABEL_CORRECTIONS.md` 为准。

## 2. 架构约定

```
PacketEvents / Bukkit
        │
        ▼
   PlayerData（每玩家 look 环缓、最近 attack、VL）
        │
        ├─► CaptureService（既有）→ records/*.jsonl
        └─► Checks（新增）→ alerts 日志 / alerts/*.jsonl
```

硬规则：

1. 检测为**旁路观察**：不改包、本阶段不取消伤害。  
2. 录包与检测**共用** look/attack 采样，避免两套计数不一致。  
3. 若玩家正在 `/ac record`，告警行带上 `captureLabel` + 可选 capture 文件名。  
4. 配置项至少：`aimsnap.enabled`、`align.enabled`（可先只打日志）、`punish.cancel-damage=false`、`punish.kick=false`。

## 3. 实现切片（按序）

### Slice A — PlayerData + 告警通道

- 环缓：最近 N 个 hasRot（默认 40）的 `|dYaw|` / `|dPitch|` / t  
- 最近一次攻击：t、targetId、alignDeg  
- `AlertService`：logger + 可选 JSONL（建议 `plugins/LinAntiAim/alerts/`）  
- 权限：`linantiaim.alerts` 收中档及以上聊天告警  

### Slice B — AimSnap（可上线告警）

阈值初值见 [`aim-provisional.md`](aim-provisional.md)：

- 窗=40 hasRot；大转=`|dYaw|≥20°`  
- 触发：窗内大转占比 ≥12% **或** 窗 p90≥25°  
- VL +1/窗；连续 3 窗才中档告警；无战斗衰减  

真机 smoke：

1. 合法近战数分钟 → 不应稳定中档告警  
2. Meteor `rotate=OnHit` → 应出现告警  
3. 同时 `/ac record start B2-ka-onhit` → 对照 jsonl 与 alert 时间戳  

### Slice C — AimAttackAlign（骨架 only）

- 每次 attack（包或 Bukkit 回退）推入短窗 alignDeg  
- 打 debug：瞬时值、窗均值/分位；**默认不升中档告警**  
- 阈值表留空或标 `provisional-blocked-until-A1-attack`  
- 已知作弊侧参考（非正式）：B1 p50≈3.95°、C2 p50≈5.40°  

### Slice D — 文档回填（与代码同 PR 或紧随）

- 更新本文件勾选、`aim-provisional.md` Align 段、`captures.md` 门禁  
- 11-17 批次写入 `replay/captures/SUMMARY.md` / 可选 `batch-2026-11-17.md`  

## 4. 仍须补录（并行，不挡 Slice A/B）

| 优先 | label | 验收 | 解锁 |
|---|---|---|---|
| P0 | `A1-legit-melee` | `attacks>0` 且 alignDeg≥0 | Align 误报安全线 |
| P1 | `C1-ka-max3` | 同上 + 多 targetId | AuraSwitch |
| P2 | B2/B3 带 attack | 可选 | 攻击窗与 Snap 联合 |

停录后必须目检 jsonl 含 `"type":"attack"`（见 captures.md §3）。

## 5. 明确不做（本阶段）

- 踢人 / 取消伤害默认开  
- 宣称 Always 默认可稳定处罚  
- 无 A1 attack 前写死 Align 踢人阈值  
- BowAimbot / 客户端 Hitboxes 检测（P1 分析卡另排）  

## 6. 完成定义（本迭代）

```
☑ 计划文档（本文件）
☑ PlayerData + AlertService
☑ AimSnap 告警 + VL
☑ AimAttackAlign 告警（A1 基线已补齐：mean≈12° vs B1≈4°）
☑ AuraSwitch 告警（横扫安全：同簇 ≥3 目标且存在 ≥60° 视野外命中）
☑ 分级处罚：警告 → 取消伤害(VL≥12，时长随 VL 递增) → 踢出(VL≥30，前有警告)
☑ VL 持久化 vl.yml + 运行时开关 /ac punish
☑ 攻击采集三源：packet / damage(含取消) / swing_ray
☑ 部署 smoke：A1 安静；B1/C2 出 Align；B2 出 Snap；C1 出 Aura；横扫不误报
☑ 0.4.2 实测：VL 正常累积，cancel@12 生效，kick@30 生效（2026-11-18 简幻欢）
□ 线上灰度：正式服先只告警观察 VL 曲线，再开 cancel_damage，最后 kick
```

## 7. 下一迭代候选

1. **AimGCD**：需要真实灵敏度样本（同一玩家多会话）才能算量化网格。  
2. **AttackOrder**：Look→Attack→Swing 固定时序；现有 `source` 字段已可支撑。  
3. **Reach / 视线外命中**（S4）：`rotate=None` 的唯一抓手。  
4. **VL 持久化**：跨重启累计，防「重进服清 VL」。
