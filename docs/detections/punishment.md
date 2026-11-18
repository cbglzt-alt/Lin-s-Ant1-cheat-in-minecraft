# 分级处罚（punishment）

> 2026-11-18 · 对应 `plugin/` 0.4.0  
> 检测阈值见 [`aim-provisional.md`](aim-provisional.md)。

## 1. 分级表

| 档 | 触发 | 默认动作 | 开关 |
|---|---|---|---|
| **低** | 有命中窗口，`vl < 12` | 静默（jsonl 里有记录） | — |
| **中** | 单个检测达标 | OP 聊天 + 控制台 + `alerts-*.jsonl` | 总是开 |
| **中+** | `vl ≥ 12` 但 cancel 关 | 给玩家提醒「suspicious aim」 | 总是开 |
| **高** | `vl ≥ 12` | 取消自己造成的伤害，**5s 起，按 VL 递增** | `punish.cancel_damage` |
| **极高前** | `vl ≥ 26`（kick−4） | 玩家收到最后警告 | `punish.kick` |
| **极高** | `vl ≥ 30` | 踢出 | `punish.kick` |

### 取消伤害时长随 VL 升级

| VL | 时长 |
|---:|---|
| 12–17 | 5s |
| 18–23 | 7s |
| 24–29 | 9s |
| … | 每 +6 VL 加 2s，上限 **20s** |

## 2. VL 规则

- 命中权重：`AimSnap +1`、`AimAttackAlign +2`、`AuraSwitch +2`
- 每秒衰减 1，停手自然回落
- **持久化**：`plugins/LinAntiAim/vl.yml`，退出/重启不清零（防重进服洗分）
- 每 30s 落盘一次，关服也会保存
- `linantiaim.bypass` 权限：不取消伤害、不踢（仍记 VL 和告警）

## 3. 运行时命令

```
/ac vl             # 看自己的 VL 与各检测分量
/ac vl <玩家>       # OP 查他人（判断是否误判时很有用）
/ac punish cancel on|off   # 即时开关取消伤害
/ac punish kick on|off     # 即时开关踢人
/ac reset          # 清零自己（测试）
/ac status         # 汇总
```

## 4. 灰度上线顺序

```
1) cancel_damage=false, kick=false —— 只观察 /ac vl 曲线
2) 合法玩家打几轮：vl 应长期 < 12（偶尔尖峰后衰减）
   Always / OnHit / max3：应稳定越过 12
3) 开 cancel_damage（建议先用 /ac punish cancel on 试，不重启）
   观察被禁伤的人是否都是作弊侧
4) 最后开 kick（阈值 30，留 4 点警告缓冲）
```

## 5. 为何不是「检测到就踢」

- Align/Snap 受网络、击退、传送影响，单次不可靠 → 必须 VL + 衰减 + 冷却
- 取消伤害是**可逆**中间档，比直接踢更适合灰度
- 踢人前有警告档，给高 ping/高感玩家留申诉空间

## 6. 已知误报源与缓解

| 场景 | 缓解 |
|---|---|
| 传送 / 重生 | `teleport_exempt_ms=2000` |
| 挖方块时乱转 | `require_recent_attack_ms=3000`（Snap 只在攻击后评分） |
| **正常横扫多目标** | AuraSwitch 改为「同簇 120ms ≥3 目标 **且** 含 ≥60° 视野外命中」；横扫目标都在视野内 → 不触发 |
| 高灵敏度玩家 | Align 需 `mean≤8°` 且 `tight≥6/20` |
| 群怪连打 | 若仍触发 Aura，把 `min_distinct` 提到 4 或 `cluster_ms` 降到 80 |

## 7. 测试清单

```
□ 合法近战 2 分钟：/ac vl 应 < 12，无 cancel/kick
□ B1 Always：vl 稳定增长，跨 12 后出现 cancel 提示
□ B2 OnHit：AimSnap 加分，vl 上涨
□ C1 max3：AuraSwitch 加分
□ 重进服：vl 不清零（看 vl.yml）
□ bypass 账号：有告警但不 cancel/不 kick
```
