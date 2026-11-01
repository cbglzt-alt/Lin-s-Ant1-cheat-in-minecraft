# REA · Meteor Client 0.5.8（MC 1.21 / 1.21.1）— Phase 1 锁定

| 项 | 值 |
|---|---|
| **锁定 jar** | `cheat-mod/meteor-client-0.5.8-1.21.1.jar` |
| 版本 | **0.5.8**（depends `1.21` / `1.21.1`） |
| 上游 tag | [`v0.5.8`](https://github.com/MeteorDevelopment/meteor-client/releases/tag/v0.5.8) @ `37c5c5c6` |
| 私服 | 简幻欢 Spigot **1.21** · `play.simpfun.cn:17184` |
| 对照 | [`../meteor-0.5.9/`](../meteor-0.5.9/)（瞄准逻辑静态一致；**不参与第一期联机**） |
| `26.3-15` | 不纳入第一期 |

## 门禁进度

```
☑ 锁定 Meteor 0.5.8（对齐 Spigot 1.21）
☑ KillAura + Rotations 静态分析卡
□ 动态录包（见 captures.md）填满分析卡 §4
□ 再实现 Aim* 检测阈值
```

## 产出

| 文件 | 状态 |
|---|---|
| [`killaura.md`](killaura.md) | 静态完成；§4 待录包 |
| [`rotations.md`](rotations.md) | 静态完成；§4 待录包 |
| [`captures.md`](captures.md) | **动态录包会话矩阵 / 操作清单** |
| `bowaimbot.md` / `hitboxes.md` | P1，可暂用 0.5.9 卡 |

模板：[`../TEMPLATE.md`](../TEMPLATE.md)。  
录包插件：仓库根目录 `plugin/`（`/ac record`）。
