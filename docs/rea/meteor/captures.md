# 动态录包 · Meteor 0.5.8（Phase 1）

目标：用服务端视角序列填满 [`killaura.md`](killaura.md) / [`rotations.md`](rotations.md) 第 4 节，再定 Aim* 阈值。

## 0. 环境（锁定）

| 侧 | 要求 |
|---|---|
| 作弊客户端 | Fabric **1.21 或 1.21.1** + **`meteor-client-0.5.8-1.21.1.jar`**（仅隔离 VM） |
| 合法对照客户端 | 原版 / 无战斗作弊的 Fabric，同协议进服 |
| 服务端 | 简幻欢 Spigot **1.21** `play.simpfun.cn:17184`，或本地同版本 Paper/Spigot |
| 录包插件 | 本仓库 `plugin/` → 构建后 `./tools/deploy-to-simpfun.sh …` |
| 依赖插件 | **PacketEvents**（与 `plugin/build.gradle.kts` 同大版本）放入 `plugins/` |

## 1. 会话矩阵

| ID | label | 客户端 | Meteor 配置 | 要观察的特征 |
|---|---|---|---|---|
| **A1** | `A1-legit-melee` | 合法 | 无 KA | 人类 Δyaw/Δpitch、攻击对齐噪声、GCD |
| **B1** | `B1-ka-always-default` | 0.5.8 | KA **默认**（`rotate=Always`） | Snap、贴 Body、无 GCD、AttackOrder |
| **B2** | `B2-ka-onhit` | 0.5.8 | 仅改 `rotate=OnHit` | 尖峰仅在攻击附近 |
| **B3** | `B3-ka-none` | 0.5.8 | 仅改 `rotate=None` | Aim 弱；Reach/视线是否仍能打中 |
| **C1** | `C1-ka-max3` | 0.5.8 | 默认 + `max-targets=3` + 多目标 | AuraSwitch |
| **C2** | `C2-ka-only-on-look` | 0.5.8 | `only-on-look=true` | 更像手动时的对齐分布 |

## 2. 已完成批次

- 11-07 look 矩阵：[`batch-2026-11-07.md`](batch-2026-11-07.md)（**B1↔C1 标签纠正**）  
- 滚动汇总（含 11-17 attack 场）：[`../../replay/captures/SUMMARY.md`](../../replay/captures/SUMMARY.md)  
- 检测开工计划：[`../detections/implementation-plan.md`](../detections/implementation-plan.md)

```
☑ A1 + B1 + B2 + B3 各 ≥1 场 look jsonl（标签已纠正）
☑ C1 多目标 ≥1 场 look
☑ killaura.md / rotations.md §4 已用 look 统计填表
☑ provisional AimSnap → docs/detections/aim-provisional.md
☑ 带 attack 的 B1 Always（2026-11-17，82 attacks）
☑ 真 C2 only-on-look + attack（2026-11-17，67 attacks）
□ 带 attack 的 A1 合法（Align 误报基线）— P0
□ 带 attack 的 C1 max3（AuraSwitch）— P1
□ 代码：AimSnap 告警 + 与 record 对照日志（见 implementation-plan）
```

原始文件放 `replay/captures/`（`*.jsonl` gitignore；`*.md` 可提交）。

## 3. 操作步骤（每场）

1. 确认 `LinAntiAim` + PacketEvents 已加载；OP 自己。  
2. `/ac record start <label>` → 战斗 → `/ac record stop`  
3. **停录后打开 jsonl：确认有 `type":"attack"` 且 `alignDeg`≥0**（否则本场作废重录）。  
4. 拉回 `replay/captures/`，`python3 tools/summarize-capture.py …`

## 4. 记录字段

| type | 含义 |
|---|---|
| `session` / `session_end` | 元数据与计数 |
| `look` | yaw/pitch/Δ、位置 |
| `attack` | 目标 + **alignDeg**（伤害回退或包监听） |
| `swing` | 挥手 |
