# 模块分析卡: Meteor Client 0.5.8 / BowAimbot

- 客户端版本 / 提交: **0.5.8**
- 分析日期 / 分析人: 2026-11-01 / REA 静态
- 对应 MC 版本: 1.21 / 1.21.1
- 类路径: `...systems.modules.combat.BowAimbot`
- 优先级: **P1**（Phase 1 主路径仍是 KillAura）

## 1. 功能概述

拉弓/弩时自动计算弹道俯仰并 `Rotations.rotate` 指向目标。默认优先级 **LowestHealth**；射程默认 **20**。

## 2. 实现机理(静态分析)

- 触发: `Render3DEvent`（渲染线程/帧），非 Tick —— 旋转请求频率可能 **高于 20 TPS**。
- 条件: 存活、主手弓或弩、有箭（创造除外）、按住使用键。
- 目标: `TargetUtils.get` + 可见性 / 好友 / 命名 / babies 过滤。
- 瞄准: 用 `BowItem.getPullProgress` 得初速；重力常量 **0.006f**；解抛物线 pitch；NaN 则退回 `getYaw/getPitch(entity)`。
- 旋转: 同样走默认静默 `Rotations.rotate`。

### 关键设置

| 设置 | 默认 |
|---|---|
| `range` | 20（0–100） |
| `entities` | 可攻击类型（builder 默认集） |
| `priority` | **LowestHealth** |
| `babies` | true |
| `nametagged` | false |
| `pause-on-combat` | false（Baritone） |

## 3. 流程伪代码

```
onRender:
  if !alive || !bow/crossbow in hand || !hasArrow: return
  target = best entity in range (visible, filters)
  if useKey pressed: Rotations.rotate(yaw, ballisticPitch or fallback)
```

## 4–5. 观测与服务端特征

- 拉弓期间 look **连续贴合弹道解**，缺人手修正噪声。
- 可能出现 **亚 tick** 旋转更新（render 驱动）→ 同 tick 多 Look 或异常平滑。
- 释放箭时刻 pitch 与落点几何高度相关；可做远程 `AimAttackAlign` 变体（箭实体生成方向）。

## 6. 检测设计

- 名称草案: `AimBowBallistic`（P1）
- 与近战共用 look 采集；仅在 `useItem` 为弓弩时启用。
- 误报: 合法预瞄、水下/风弹道差异。

## 7–9. 绕过 / Grim / 测试

- 关掉模块或手动微调仍可部分绕过启发式。
- Grim 无专项弓瞄启发式 → 本检测为增量。
- [ ] 真机拉满弓默认配置录包  
