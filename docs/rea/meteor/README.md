# REA · Meteor Client 0.5.8（MC 1.21 / 1.21.1）— Phase 1 锁定

| 项 | 值 |
|---|---|
| **锁定 jar** | `cheat-mod/meteor-client-0.5.8-1.21.1.jar` |
| 版本 | **0.5.8**（depends `1.21` / `1.21.1`） |
| 上游 tag | [`v0.5.8`](https://github.com/MeteorDevelopment/meteor-client/releases/tag/v0.5.8) @ `37c5c5c6` |
| 私服 | 简幻欢 Spigot **1.21** · `play.simpfun.cn:17184` |
| 备选 jar | `cheat-mod/` 内 0.5.9 / 26.3 **仅存档，无分析卡、不联机** |

本目录是 Phase 1 **唯一** REA 文档树。曾用 0.5.9 做过草稿对照：`KillAura` / 外层 `Rotations` 等与 0.5.8 **字节码相同**；`Rotations$Rotation.sendPacket` 在 0.5.9 多一个 `horizontalCollision` 参数（1.21.2+ 映射），不影响瞄准机制结论。动态阈值必须以 **0.5.8 真机** 录包为准。

## 门禁进度

```
☑ 锁定 Meteor 0.5.8（对齐 Spigot 1.21）
☑ KillAura + Rotations（+ BowAimbot / Hitboxes）静态分析卡
☑ 与 0.5.9 瞄准类字节码核对（结论：主路径等价）
□ 动态录包（见 captures.md）填满分析卡 §4
□ 再实现 Aim* 检测阈值
```

## 产出

| 文件 | 状态 |
|---|---|
| [`killaura.md`](killaura.md) | P0 静态完成；§4 待录包 |
| [`rotations.md`](rotations.md) | P0 静态完成；§4 待录包 |
| [`captures.md`](captures.md) | 动态录包会话矩阵 |
| [`bowaimbot.md`](bowaimbot.md) | P1 静态 |
| [`hitboxes.md`](hitboxes.md) | P1 静态 |

模板：[`../TEMPLATE.md`](../TEMPLATE.md)。  
录包插件：`plugin/`（`/ac record`）。
