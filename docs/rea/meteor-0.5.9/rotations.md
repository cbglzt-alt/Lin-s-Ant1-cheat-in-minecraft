# 模块分析卡: Meteor Client 0.5.9 / Rotations

- 客户端版本 / 提交: **0.5.9**；上游 `v0.5.9`
- 分析日期 / 分析人: 2026-11-01 / REA 静态
- 对应 MC 版本: 1.21.2 / 1.21.3
- 类路径: `meteordevelopment.meteorclient.utils.player.Rotations`
- 挂钩: `ClientPlayerEntityMixin` → `SendMovementPacketsEvent.Pre/Post`
- 全局配置: `Config.rotationHoldTicks`（设置名 `rotation-hold`，**默认 4**）

## 1. 功能概述

全客户端共享的**旋转队列**：模块调用 `Rotations.rotate(yaw, pitch)` 后，在发送移动包前后短暂改写玩家 yaw/pitch，使**服务端收到的朝向**指向目标，再恢复本地朝向。  
KillAura / BowAimbot 等默认走这条路径 → 典型 **Silent Aim（发包瞄准）**。

## 2. 实现机理(静态分析)

### 触发事件

1. 任意模块 `rotate(...)` → 按 priority 插入 `rotations` 列表。
2. `SendMovementPacketsEvent.Pre`（`sendMovementPackets` HEAD / 载具 tick 特例）  
   → 取队首：`setClientRotation`（暂改 `player.yaw/pitch`）+ `setCamRotation`（更新 `serverYaw/serverPitch`）。
3. 原版继续发移动包 → **包内 look = 队列目标角**。
4. `SendMovementPacketsEvent.Post`  
   → 跑 callback；`resetPreRotation()` 还原客户端 yaw/pitch；  
   → 若队列还有剩余，对后续项可再 `sendPacket()` 额外 `PlayerMoveC2SPacket.LookAndOnGround`。

### 读取的状态

- 当前玩家 yaw/pitch（存为 `preYaw`/`prePitch`）
- `Config.rotationHoldTicks`
- 相机实体是否等于玩家（旁观跟随则跳过）

### 修改的状态 / 发送的包

| 字段/包 | 行为 |
|---|---|
| 玩家 yaw/pitch | Pre 设为目标角 → Post 恢复（`clientSide=false` 时） |
| `serverYaw` / `serverPitch` | 始终更新为本次旋转 |
| 常规移动包 | 内容已被暂改后的朝向污染（无新包类型） |
| 额外包 | `new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround, horizontalCollision)`（队列多项时） |

`clientSide=true`：Post 阶段也会 `setClientRotation`，即**客户端视角也跟着转**（非静默）。KillAura 调用的重载默认 **`clientSide=false`**。

### 关键设置项

| 来源 | 默认 | 说明 |
|---|---|---|
| `Config.rotation-hold` | **4** | 队列空后，继续用 `lastRotation` 污染后续移动包，最多 hold 这么多次 Pre |

KillAura **不传** priority/callback；priority=0。

### 角度计算（反作弊相关）

```
getYaw(entity):
  atan2(dz, dx) → deg - 90，再对当前 yaw 做 wrapDegrees 增量合成

getPitch(entity, Body):
  aimY = entity.y + height/2
  相对眼睛高度做 atan2，再 wrap 到当前 pitch
```

结果是**精确几何角**，无步进量化。

### 反检测相关逻辑

- 无 GCD、无噪声、无加速限制。
- `rotation-hold` 会在停止请求后短暂**继续保持服务端朝向**，形成「打完仍盯着」的尾巴。

## 3. 流程伪代码

```
rotate(yaw, pitch, priority=0, clientSide=false, cb=null):
  insert into rotations by priority

onPre:
  if rotations not empty:
    rotating = true
    setup(rotations[0])   # temp set player look + serverYaw/Pitch
  else if lastRotation and holdTimer < rotationHoldTicks:
    setup(lastRotation); sentLastRotation = true

# vanilla sends movement packets here #

onPost:
  if rotations not empty:
    runCallback; restore preYaw/prePitch
    for remaining: maybe setClient if clientSide; LookAndOnGround packet; callback
    keep last as lastRotation; clear queue
  else if sentLastRotation:
    restore preYaw/prePitch
```

## 4. 动态观测(数据对比)

| 特征 | 正常玩家 | Rotations 静默 (KA Always) | clientSide 真转头 |
|---|---|---|---|
| 包内 yaw/pitch | = 玩家朝向 | = 瞄准角；可与「体感视角」不一致* | = 瞄准角 |
| 攻击对齐 | 有误差 | 攻击 tick 高度对齐 Body | 同左 |
| Hold 尾巴 | 无固定 4-tick | 停瞄后仍可能保持若干包 | 同 |

\*服务端无法直接看见客户端相机；此处指反制时应用「包内角」而非假设第三人称渲染。

## 5. 服务端可观测特征

1. **Look 在攻击前后突变到几何最优**，且常在 hold 窗口内保持。
2. 同一目标连续多 tick **重复几乎相同的绝对 yaw/pitch**（追踪移动目标时则平滑跟随中心，但仍缺人手抖动）。
3. 可能出现**额外的 LookAndOnGround**（多旋转排队时）——相对少见，KillAura 通常一 tick 一项。
4. 盲区：若作弊改用纯 clientSide 或自研平滑，特征分布会变；本卡仅覆盖原版 Meteor `Rotations`。

## 6. 检测设计

- 名称: `AimSnap` / `AimAttackAlign` / `AimGCD`（与 KillAura 卡共用底座：记每个移动包的 yaw/pitch + 攻击包时序）
- 判定逻辑: 以**服务端收到的 look** 为唯一真相；对比目标 Body/AABB
- 阈值: 录包后定；注意 `rotation-hold=4` 造成的短时平台
- VL: 旋转异常与攻击对齐分计，对齐类需更高缓冲
- 误报: 传送、床/锚重生、激流、坐骑、高速转头娱乐场景

## 7. 已知绕过与应对

| 绕过 | 应对 |
|---|---|
| 不用 Rotations（None / 自写发包） | Reach + 对齐统计；版本差异另卡 |
| 加平滑/随机 fork | 本静态卡失效信号；动态重分析 |
| 调高 rotation-hold | 特征从「尖峰」变「平台」，检测用窗口而非单 tick |

## 8. 相对 Grim

Grim 移动预测不解释「为什么 look 完美贴目标中心」。本工具类说明 **服务端看到的朝向就是被污染的包内角**，专项 Aim 检查有协议依据。

## 9. 测试用例

- [ ] 仅开 Rotations 路径的 KA Always：录 Look 序列 vs Attack  
- [ ] 改 Config `rotation-hold` 0 / 4 / 10 对比尾巴长度  
- [ ] OnHit：确认仅攻击附近 tick 出现尖峰  
