# cheat-mod（第一期 REA 对象）

本目录存放**待分析的作弊客户端模组**。第一期只针对下面这一份构建：

| 项 | 值 |
|---|---|
| 文件 | `meteor-client-26.3-15.jar` |
| 名称 | Meteor Client |
| 版本 | **26.3-15**（build 15） |
| commit | `e3a2e77967629d216e127b5ceecf71fb15f9d2e5` |
| 环境 | **客户端** Fabric mod（`environment: client`） |
| 依赖 | Minecraft **~26.3**，Java **≥ 25**，Fabric Loader ≥ 0.19.5 |
| 许可 | GPL-3.0 |
| 上游 | https://github.com/MeteorDevelopment/meteor-client |

## 瞄准相关模块（jar 内已确认）

| 模块 | 类 | Phase 1 优先级 |
|---|---|---|
| **KillAura** | `.../combat/KillAura` | **P0**（旋转模式 `Always` / `OnHit` / `None`） |
| **BowAimbot** | `.../combat/BowAimbot` | P1 |
| **Hitboxes**（客户端扩 hitbox） | `.../combat/Hitboxes` | P1（与服务端 Reach 相关） |
| **Rotations** 工具 | `.../utils/player/Rotations` | P0 配套（含 `clientSide` / `sendPacket`） |
| CrystalAura / AnchorAura / BedAura 等 | 同类 aura | 非第一期瞄准主路径 |

> 此版本 **没有** 独立 `AimAssist` 类；近战自动瞄准能力主要落在 **KillAura + Rotations**。

## 使用约定

1. **只在隔离 VM 中运行**该 jar；默认禁外网。
2. 二进制尽量**不要提交到公开远程**（见根目录 `.gitignore`）；本地分析可保留。
3. 开源项目优先对照同 commit 的 GitHub 源码做静态分析，jar 用于版本锁定与动态验证。
4. **版本对齐**：该 mod 只能进 **MC 26.3** 服务端；当前简幻欢实例是 **Spigot 1.21**，协议不兼容，联调前必须解决版本问题（见 `docs/platform/simpfun.md` 与 `HANDOFF.md`）。
