# 模块分析卡: Meteor Client 0.5.8 / KillAura

- 客户端版本 / 提交: **0.5.8** jar `meteor-client-0.5.8-1.21.1.jar`；上游 tag `v0.5.8` @ `37c5c5c6`
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

> 批次 **2026-11-07**（Lincoln @ 简幻欢）。原始 jsonl：`replay/captures/`（已按操作者说明纠正 B1↔C1 标签，见同目录 `LABEL_CORRECTIONS.md` / [`batch-2026-11-07.md`](batch-2026-11-07.md)）。  
> **数据缺陷**：本批全部 `attacks=0`（客户端 `InteractEntity` 未进录包）；`alignDeg` / AuraSwitch **本批无法填实**。下表以 **hasRot 的 |dYaw|** 与 swing 邻近 |dYaw| 为主。插件已补 Bukkit 伤害回退，需重部署后补录攻击场。

| 特征 | A1 合法 | B1 Always 单目标 | B2 OnHit | B3 None | C1 max3 多目标 |
|---|---|---|---|---|---|
| rotLooks / swings | 780 / 45 | 712 / 54 | 874 / **218** | 655 / 53 | 928 / 70 |
| \|dYaw\| p50 / p90 / p99 | 0.83 / **6.0** / 31 | 1.55 / 7.1 / **136** | **4.88 / 64.9 / 170** | 1.65 / 6.6 / 42 | 1.65 / 9.1 / 92 |
| \|dYaw\|≥20° 占比 | **2.2%** | 5.6% | **29.3%** | 3.1% | 4.7% |
| swing 前≤150ms \|dYaw\| p50/p90 | 1.2 / 7.4 | 1.6 / 9.4 | 1.8 / 7.7 | 1.5 / 5.4 | 1.7 / 5.1 |
| alignDeg@attack | N/A | N/A | N/A | N/A | N/A |

**读数要点**

1. **OnHit（B2）** 在纯 look 上已与合法显著分离（≥20° 旋转包近 3 成，p90≈65°）→ `AimSnap` 第一候选。
2. **Always 单目标（B1）** 中位 Δyaw 接近合法，但 p99 出现超大尖峰；静默跟踪时多数 tick Δ 很小，需 **攻击瞬间 align** 才能稳抓（本批缺失）。
3. **None（B3）** look 分布接近合法 → 验证「不转头」盲区；应用 Reach/视线，而非 AimSnap。
4. **多目标（C1）** 在仅 look 统计上与 B1 接近；缺 attack 目标 id 时 **AuraSwitch 无法验收**。

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

| 检测 | 判定逻辑（草案） | 阈值依据（provisional） | VL/缓冲 | 误报风险 |
|---|---|---|---|---|
| `AimSnap` | 滑动窗内 hasRot 的 \|dYaw\|≥20° 占比，或 p90(\|dYaw\|) | A1≈2%/p90≈6°；B2≈29%/p90≈65° → 窗占比 **≥12%** 或 p90 **≥25°** 先告警 | 中；连续 2–3 窗 | 传送、击退、载具、橡皮筋 |
| `AimAttackAlign` | 攻击瞬间 alignDeg 滑动均值过低 | **本批无 attack 数据**；补录后定 | **高**缓冲 | 近距贴脸、高感 |
| `AimGCD` | 转头增量与 sens GCD 不一致占比 | 本批未算真 GCD；暂缓 | 中 | 改键鼠/触控 |
| `AuraSwitch` | 短窗多不同实体攻击 | **本批无 attack**；补录 C1 | 低 | 群怪清场 |
| `AttackOrder` | Look 突变与 Attack/Swing 时序 | 本批仅 swing；弱证据 | 低 | 合法急转砍 |

> 详见 [`../detections/aim-provisional.md`](../detections/aim-provisional.md)。**AimAttackAlign 未补录前不写死踢人阈值。**

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
- [ ] 阈值以本版本 1.21 录包为准（勿用其他 Meteor 构建估阈值）

## 附：关键 intermediary（本 jar）

| 混淆名 | 含义 |
|---|---|
| `class_2868` | UpdateSelectedSlotC2SPacket |
| `method_2918` | attackEntity |
| `method_6104` | swingHand |
| `class_1268.field_5808` | MAIN_HAND |
| `class_1299.field_6097` | EntityType.PLAYER |
