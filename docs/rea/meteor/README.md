# REA · Meteor Client 26.3-15

锁定对象见 [`cheat-mod/README.md`](../../../cheat-mod/README.md)。

## 第一期范围

**目标作弊行为**：自动瞄准 / 自动锁定攻击（以 KillAura 为主，BowAimbot 为辅）。  
**交付物**：部署到私服的**服务端反作弊插件**（不是 Fabric 客户端 mod），在简幻欢或同版本测试服上验证。

## 分析顺序

1. `KillAura` + `KillAura$RotationMode`（Always / OnHit / None）
2. `utils/player/Rotations`（发包旋转 vs 仅客户端旋转）
3. 动态录包：默认配置 vs 各 RotationMode
4. （可选）`BowAimbot`、`Hitboxes`

## 产出文件

| 文件 | 状态 |
|---|---|
| `killaura.md` | 待写（静态 → 动态） |
| `rotations.md` | 待写 |
| `bowaimbot.md` | 可选 |

模板：[`../TEMPLATE.md`](../TEMPLATE.md)（若尚未创建则从 HANDOFF 6.4.3 复制）。
