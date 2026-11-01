# 模块分析卡: Meteor Client 0.5.8 / KillAura

- 客户端版本 / 提交: **0.5.8** jar `meteor-client-0.5.8-1.21.1.jar`；上游 tag `v0.5.8` @ `37c5c5c6`；静态与 0.5.9 常量池/设置一致，已对照锁定
- 分析日期 / 分析人: 2026-11-01 / REA 静态
- 对应 MC 版本: **1.21 / 1.21.1**（Fabric client；对齐简幻欢 Spigot 1.21）
- 类路径: `meteordevelopment.meteorclient.systems.modules.combat.KillAura`
- 方法: CFR 反编译 jar + 对照官方源码（一致）

## 1. 功能概述

自动选择周围实体并攻击。无独立 AimAssist；近战「帮瞄」= **KillAura 的 `rotate` + `Rotations`**。  
默认即开瞄准：`rotate=Always`，朝目标 **Body** 中心点算 yaw/pitch。

## 2. 实现机理(静态分析)

### 触发事件

- `TickEvent.Pre` → `onTick`：选目标、可选换手、请求旋转、按延迟攻击。
- `PacketEvent.Send`：若包为 `UpdateSelectedSlotC2SPacket`，重置 `switchTimer = switchDelay`。

### 读取的状态

- 存活 / 非旁观、是否挖方块或使用物品、左键是否按下、服务器 tick 间隔（lag）、CrystalAura 放置计时。
- 世界实体列表（经 `TargetUtils.getList`）或当前准星实体（`only-on-look`）。
- 目标 AABB 最近点距离、视线（`PlayerUtils.canSeeEntity`）、好友、创造、盾牌、驯服/被动生物过滤。

### 修改的状态 / 发送的包

| 动作 | 客户端行为 | 服务端可见包（协议层） |
|---|---|---|
| 旋转 | `Rotations.rotate(yaw, pitch)`（默认 **非 clientSide**） | 见 [`rotations.md`](rotations.md)：移动包内 **yaw/pitch 被改写**，随后客户端视角还原 → **静默瞄准** |
| 攻击 | `interactionManager.attackEntity` + `swingHand(MAIN_HAND)` | `PlayerInteractEntityC2S`（Attack）+ `HandSwingC2S` |
| 换手 | `InvUtils.swap` | `UpdateSelectedSlotC2S`（并触发 switch-delay） |

**无**：平滑插值、随机抖动、故意 miss、GCD/灵敏度伪造（本模块内）。

### 关键设置项(默认值/范围/模式)

#### General

| 设置 | 默认 | 说明 |
|---|---|---|
| `weapon` | **All** | Sword / Axe / Mace / Trident / All / Any |
| `rotate` | **Always** | Always / **OnHit** / **None** |
| `auto-switch` | false | 热键栏切到对应武器 |
| `only-on-click` | false | 需按住攻击键 |
| `only-on-look` | false | 仅打准星实体；开启后隐藏 max-targets |
| `pause-baritone` | true | 战斗时暂停寻路 |
| `shield-mode` | Break | Ignore / Break / None（需 auto-switch 且武器≠Axe） |

#### Targeting

| 设置 | 默认 | 说明 |
|---|---|---|
| `entities` | **PLAYER** | 可攻击实体类型集 |
| `priority` | **ClosestAngle** | LowestDistance / HighestDistance / LowestHealth / HighestHealth / ClosestAngle |
| `max-targets` | **1**（1–5） | 同 tick 可对列表全部 `attack` |
| `range` | **4.5**（slider≤6） | 到 AABB **钳位点** 的距离 |
| `walls-range` | **3.5** | 不可见时额外距离门限 |
| `mob-age-filter` | Adult | Baby / Adult / Both |
| `ignore-named` | false | |
| `ignore-passive` | true | Enderman/僵尸猪灵/狼未敌对则跳过 |
| `ignore-tamed` | false | |

距离判定：`MathHelper.clamp` 玩家坐标到目标 box 最近点，再 `PlayerUtils.isWithin(..., range)` —— **按扩展后的几何距离，不是视线射线**。

#### Timing

| 设置 | 默认 | 说明 |
|---|---|---|
| `pause-on-lag` | true | `timeSinceLastTick ≥ 1s` 停手 |
| `pause-on-use` | false | |
| `pause-on-CA` | true | CrystalAura 放置中停手 |
| `TPS-sync` | **true** | `delay /= (tickRate/20)` |
| `custom-delay` | **false** | false 时用原版冷却进度 |
| `hit-delay` | 11 tick | 仅 custom-delay |
| `switch-delay` | 0 | 切槽后等待 |

`delayCheck`（非 custom）：`getAttackCooldownProgress(delay) >= 1`，其中 `delay` 初值 **0.5f**，再按 TPS-sync 缩放。

### 反检测相关逻辑

- **本模块几乎没有「反启发式」**：无随机、无平滑、无假 GCD。
- 可选弱化特征：`rotate=None`（不转头）、`OnHit`（只在出手瞬间转）、`only-on-look`（更像手动）、提高 `hit-delay` / 关 TPS-sync。
- 静默旋转依赖全局 `Rotations`（见配套卡），不是 KillAura 单独实现。

## 3. 流程伪代码

```
onTick Pre:
  if dead/spectator/paused/...: return
  if onlyOnLook: targets = [crosshair] if entityCheck else []
  else: TargetUtils.getList(entityCheck, priority, maxTargets)

  if targets empty: attacking=false; resume baritone; return

  primary = targets[0]
  maybe autoSwitch / shield-break axe
  if !itemInHand: return

  attacking = true
  if rotate == Always:
      Rotations.rotate(getYaw(primary), getPitch(primary, Body))   # 每 tick

  if delayCheck():
      for t in targets:
          if rotate == OnHit:
              Rotations.rotate(getYaw(t), getPitch(t, Body))
          attackEntity(t); swingMainHand; hitTimer=0
```

瞄准点：`Target.Body` → `entity.getY() + height/2`（躯干中心）。

## 4. 动态观测(数据对比)

> 静态假设已锁定。填表步骤见 [`captures.md`](captures.md)；**阈值须录包后填实，禁止拍脑袋。**

| 特征 | 正常玩家 | 默认 KA (Always) | 绕过向配置 |
|---|---|---|---|
| 攻击瞬间视线–Body 中心角误差 | 有噪声、常不完全对准 | 极小、重复贴中心 | None：可很大；OnHit：仅攻击 tick 对准 |
| Δyaw/Δpitch 步进 | 近似鼠标 GCD 网格 | 任意浮点瞬移 | 同左（仍无 GCD） |
| 多目标 | 少见同冷却多实体 | max-targets>1 时同 delay 窗口连打 | 保持 1 |
| 旋转保持 | 人手连续 | 停转后仍可能 hold `rotation-hold` tick | — |

## 5. 服务端可观测特征

**强特征（默认 Always）**

1. **AimSnap**：有目标时每 tick 服务端视角跳到几何最优角（Body），人手少见。
2. **AimAttackAlign**：`InteractEntity(Attack)` 时刻 look 与目标 Body 中心高度对齐质量过高、方差过低。
3. **无 GCD**：转角增量不落在灵敏度量化网格上（`AimGCD`）。
4. **AttackOrder**：同 tick / 紧邻 tick 内 Look 变 → Attack → Swing 的固定模式。
5. **AuraSwitch**：`max-targets>1` 时短间隔多实体攻击。

**弱 / 盲区**

- `rotate=None`：服务端视角可完全不对目标 → 专项瞄准弱，改靠 Reach/Hitbox/视线。
- `only-on-look`：行为更接近合法瞄准后的连点，需更长统计窗。
- 高延迟 + TPS-sync：攻击间隔被拉长，单纯「打太快」不可靠。
- 静默：客户端渲染视角与包内 yaw/pitch 可不一致；**服务端只看见包内角**，静默不构成观测盲区，反而保证攻击包对齐时 look 已改。

## 6. 检测设计

| 检测 | 判定逻辑（草案） | 阈值依据 | VL/缓冲 | 误报风险 |
|---|---|---|---|---|
| `AimSnap` | 攻击前 N tick 内 |Δyaw|+|Δpitch| 过大且终点对准目标 | 动态：默认 KA vs 合法 PvP | 中缓冲 | 传送、击退、载具、橡皮筋 |
| `AimAttackAlign` | 攻击瞬间视线到 Body/AABB 的最小角误差滑动均值过低 | 默认 Always 应稳定告警 | 高缓冲 | 近距贴脸、极高感选手 |
| `AimGCD` | 转头增量与 sens GCD 不一致占比 | 默认无伪造 → 高检出 | 中 | 改键鼠/拨盘、触控 |
| `AuraSwitch` | 短窗多不同实体攻击 | max-targets 场景 | 低 | 群怪清场 |
| `AttackOrder` | Look 突变与 Attack 固定时序 | 录包后 | 低 | 合法急转砍 |

> 未完成第 4 节录包前禁止拍脑袋定阈值（HANDOFF S3）。

## 7. 已知绕过与应对

| 绕过 | 影响 | 应对 |
|---|---|---|
| `rotate=None` | Aim* 失效 | Reach / 视线外命中底座 |
| `rotate=OnHit` + 低频率 | Snap 样本变少 | 加长窗；专盯攻击 tick |
| `only-on-look` | 像手动 | Align 方差 + 连击节奏联合 |
| 第三方平滑 fork | 本卡不适用 | 版本锁定；另开分析卡 |
| 扩 `range`>合法 reach | 出距 | Reach 检查 |

## 8. 相对 Grim

- Grim 开源 Aim* 很窄（如 duplicate look / modulo）；**默认 Always 的机器贴中心**开源侧基本不专项抓。
- 本卡贡献：把 Meteor 默认旋转模式、瞄准点（Body）、静默发包路径写成可测特征，支撑 `AimSnap` / `AimAttackAlign` / `AimGCD`。

## 9. 测试用例

- [ ] 合法 Bot：人类噪声转头 + 1.9 冷却敲击  
- [ ] 作弊 Bot：重放 Always / OnHit / None；max-targets=3  
- [ ] 真机 VM：本 jar + Fabric 1.21/1.21.1；默认配置进 `play.simpfun.cn:17184` 录包  
- [ ] 对照：0.5.9 卡仅作备选；阈值以本版本 1.21 录包为准  

## 附：关键 intermediary（本 jar）

| 混淆名 | 含义 |
|---|---|
| `class_2868` | UpdateSelectedSlotC2SPacket |
| `method_2918` | attackEntity |
| `method_6104` | swingHand |
| `class_1268.field_5808` | MAIN_HAND |
| `class_1299.field_6097` | EntityType.PLAYER |
