# REA · Meteor Client 0.5.8（MC 1.21 / 1.21.1）

锁定对象见 [`cheat-mod/README.md`](../../../cheat-mod/README.md)。

| 项 | 值 |
|---|---|
| 主 jar | `cheat-mod/meteor-client-0.5.8-1.21.1.jar` |
| 私服 | 简幻欢 Spigot **1.21** |
| 旧 jar `26.3-15` | 不纳入第一期联机 |

## 第一期范围

**目标作弊行为**：自动瞄准 / 自动锁定攻击（以 KillAura 为主，BowAimbot 为辅）。  
**交付物**：部署到私服的**服务端反作弊插件**（不是 Fabric 客户端 mod）。

## 分析顺序

1. `KillAura`（含旋转模式枚举，以 0.5.8 字节码/同源源码为准）
2. `utils/player/Rotations`（发包旋转 vs 仅客户端旋转）
3. 动态录包：默认配置 vs 各旋转模式（连简幻欢或本地 1.21）
4. （可选）`BowAimbot`、`Hitboxes`

## 产出文件

| 文件 | 状态 |
|---|---|
| `killaura.md` | 待写（静态 → 动态） |
| `rotations.md` | 待写 |
| `bowaimbot.md` | 可选 |

模板：[`../TEMPLATE.md`](../TEMPLATE.md)。
