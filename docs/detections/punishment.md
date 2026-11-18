# 分级处罚（punishment）

> 2026-11-18 · 对应 `plugin/` 0.3.0  
> 检测阈值见 [`aim-provisional.md`](aim-provisional.md)。

## 1. 分级表

| 档 | 触发 | 默认动作 | 配置 |
|---|---|---|---|
| **低** | 有命中窗口但 `vl < cancel_at` | 仅日志 + JSONL | — |
| **中** | 单检查告警（`streak` 达标） | OP 聊天 + 控制台 + `alerts-*.jsonl` | `alerts.broadcast` |
| **高** | 累计 **VL ≥ 12** | 取消自己造成的伤害 **5s** | `punish.cancel_damage` |
| **极高** | 累计 **VL ≥ 24** | 踢出 | `punish.kick` |

- VL 每 1 秒衰减 1，战斗停止后自然归零。
- 单次大转**不会**直接踢；必须累计。
- 命中权重：`AimSnap +1`、`AimAttackAlign +2`、`AuraSwitch +2`（对齐/切目标更可信）。
- `linantiaim.bypass` 权限可免除取消伤害/踢出。
- `/ac vl` 查看当前 VL，`/ac reset` 清零（测试用）。

## 2. 灰度上线顺序

```
1) 保持 cancel_damage=false, kick=false —— 只观察 vl 增长曲线
2) 合法玩家 + Always/OnHit/max3 各打一轮；确认
   - 合法 vl 基本不涨（>12 视为异常）
   - Always/only-on-look 能过 12
3) 开 cancel_damage=true（5s），观察是否只作用于作弊侧
4) 最后开 kick=true（阈值可先 30 再降到 24）
```

## 3. 为何不是「检测到就踢」

- Align/Snap 受网络、击退、传送影响，单次不可靠 → 必须 VL + 衰减 + 冷却。
- 取消伤害是**可逆**的中间档，比直接踢更适合线上灰度。
- 踢人默认关，避免误伤正常高感玩家。

## 4. 已知误报源

| 场景 | 缓解 |
|---|---|
| 传送 / 重生 | `teleport_exempt_ms=2000` |
| 挖方块时乱转 | `require_recent_attack_ms=3000`（Snap 只在攻击后评分） |
| **正常横扫多目标** | AuraSwitch 改为「同簇 120ms ≥3 目标 **且** 含 ≥60° 视野外命中」；横扫目标都在视野内，不触发 |
| 高灵敏度玩家 | Align 需 `mean≤8°` 且 `tight≥6/20`，偏保守 |

> 2026-11-18：横扫误报已在 0.3.1 修复。若极端场景仍触发，可把 `min_distinct` 提到 4 或 `cluster_ms` 降到 80。
