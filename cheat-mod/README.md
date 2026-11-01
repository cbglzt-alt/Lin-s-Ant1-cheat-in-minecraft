# cheat-mod（第一期 REA 对象）

本目录存放**待分析的作弊客户端模组**。

## 第一期主目标（已对齐私服）

| 项 | 值 |
|---|---|
| 文件 | **`meteor-client-0.5.8-1.21.1.jar`** |
| 名称 | Meteor Client |
| 版本 | **0.5.8** |
| 环境 | **客户端** Fabric mod |
| 依赖 | Minecraft **`1.21` / `1.21.1`**，Java **≥ 21**，Fabric Loader ≥ 0.15.11 |
| 许可 | GPL-3.0 |
| 来源 | [Meteor Archive](https://maninmyvan.github.io/meteor-archive/)（社区归档；官方不支持旧版） |
| 对齐 | 简幻欢实例镜像 **Spigot 1.21** |

### 瞄准相关模块（0.5.8 jar 内已确认）

| 模块 | 类 | Phase 1 优先级 |
|---|---|---|
| **KillAura** | `.../combat/KillAura` | **P0** |
| **BowAimbot** | `.../combat/BowAimbot` | P1 |
| **Hitboxes**（客户端扩 hitbox） | `.../combat/Hitboxes` | P1 |
| **Rotations** 工具 | `.../utils/player/Rotations` | P0 配套 |

> Phase 1 **已锁定本文件**。静态卡：`docs/rea/meteor/`；动态录包：`docs/rea/meteor/captures.md`（必须用本 jar，勿用 0.5.9/26.3）。

---

## 备选 / 不兼容构建

| 文件 | MC 依赖 | 用途 |
|---|---|---|
| `meteor-client-0.5.9-1.21.2-3.jar` | 1.21.2 / 1.21.3 | 仅存档；暂不分析、不联机（升私服后再开 REA） |
| `meteor-client-26.3-15.jar` | **~26.3**（Java ≥ 25） | **无法**进当前 1.21 私服；仅作对照，**非**第一期联机对象 |

---

## 使用约定

1. **只在隔离 VM 中运行**；默认禁外网。
2. `*.jar` 已 gitignore，勿推到公开远程。
3. 客户端：Fabric **1.21 或 1.21.1** + 本目录 `0.5.8` jar → 连接 `play.simpfun.cn:17184`。
4. 若进服协议报错，再考虑服务端 ViaVersion，或客户端改用精确匹配的小版本。
