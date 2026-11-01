# 模块分析卡: Meteor Client 0.5.8 / Hitboxes

- 客户端版本 / 提交: **0.5.8**
- 分析日期 / 分析人: 2026-11-01 / REA 静态
- 对应 MC 版本: 1.21 / 1.21.1
- 类路径: `...systems.modules.combat.Hitboxes`
- Mixin: `EntityMixin.onGetTargetingMargin` → `Entity.getTargetingMargin()`
- 优先级: **P1**

## 1. 功能概述

**仅客户端**扩大实体的 targeting margin（默认 expand **0.5**），让准星/部分客户端判定更容易「点到」。  
**不修改服务端 hitbox**；KillAura 距离用的是真实 AABB 钳位距离，不直接读本模块 expand。

## 2. 实现机理(静态分析)

```
getEntityValue(entity):
  if !active || !weaponOk || (ignoreFriends && friend): return 0
  if entityType in list: return expand else 0

EntityMixin.getTargetingMargin HEAD:
  v = Hitboxes.getEntityValue(this)
  if v != 0: return (float)v
```

### 关键设置

| 设置 | 默认 |
|---|---|
| `entities` | PLAYER |
| `expand` | **0.5** |
| `ignore-friends` | true |
| `only-on-weapon` | false（true 时仅剑/斧） |

`EntityRenderDispatcherMixin` 亦引用本模块（渲染侧辅助，不影响协议）。

## 3–5. 服务端含义

- **无专用 C2S 包**；服务端看不到「客户端扩了 hitbox」。
- 间接效果：玩家更容易在**视线擦边**时仍发起合法攻击；与 Reach 检查正交。
- 对默认 KillAura：主要仍靠 Rotations 贴中心，Hitboxes 不是主特征源。

## 6. 检测设计

- 不单独做「Hitboxes 模块检测」。
- 依赖底座 **Reach / 视线–AABB**；若出现「look 明显偏出原版 box 仍持续命中」，再与延迟补偿一并评估（易误报，P1 后置）。

## 7–9. 绕过 / Grim / 测试

- 关闭或不装本模块即可去掉客户端优势。
- Grim Reach/Hitboxes 已覆盖非法距离；本项目保持「至少持平」即可。
- [ ] 可选：开 Hitboxes+手动点击 vs 不开，对比擦边命中率（非 Phase 1 门禁）  
