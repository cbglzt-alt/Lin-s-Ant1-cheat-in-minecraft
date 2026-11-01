# REA · Meteor Client 0.5.9（MC 1.21.2 / 1.21.3）

| 项 | 值 |
|---|---|
| jar | `cheat-mod/meteor-client-0.5.9-1.21.2-3.jar` |
| 版本 | **0.5.9**（`fabric.mod.json`） |
| MC | **1.21.2 / 1.21.3**，Java ≥ 21，Fabric Loader ≥ 0.16.9 |
| 上游 tag | [`v0.5.9`](https://github.com/MeteorDevelopment/meteor-client/releases/tag/v0.5.9) @ `0adc76df` |
| 许可 | GPL-3.0 |
| 与 Phase 1 | **已锁定 0.5.8 联机**；本目录仅对照 / 日后 1.21.2+。动态录包见 [`../meteor/captures.md`](../meteor/captures.md) |

## 与 0.5.8 差异（瞄准相关）

对 `KillAura.class` 常量池设置名做集合差：**无差异**（`weapon` / `rotate` / `range` / `walls-range` / 延迟与枚举均同）。  
瞄准链路（KillAura → `Rotations` → `SendMovementPackets` mixin）在 0.5.9 仍适用；Phase 1 检测逻辑可复用，**阈值须用对应 MC/协议版本重测**。

## 分析顺序与产出

| 文件 | 状态 | 优先级 |
|---|---|---|
| [`killaura.md`](killaura.md) | 静态完成；动态待录包 | **P0** |
| [`rotations.md`](rotations.md) | 静态完成；动态待录包 | **P0** |
| [`bowaimbot.md`](bowaimbot.md) | 静态完成 | P1 |
| [`hitboxes.md`](hitboxes.md) | 静态完成 | P1 |

本地反编译缓存：`.rea-work/0.5.9/`（已 gitignore，勿提交）。

模板：[`../TEMPLATE.md`](../TEMPLATE.md)。
