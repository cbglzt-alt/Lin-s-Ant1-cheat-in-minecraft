# 简幻欢(Simpfun)公益服 ↔ 本地开发对接

> 调研日期:2026-11-01  
> 面板入口:`https://simpfun.cn/console`（用户口中的 simplefun.cn console）  
> 用途:在线验证作弊客户端与反作弊插件的主测试平台

---

## 1. 当前实例快照

| 项 | 值 |
|---|---|
| 实例 | `#895060`（未命名实例） |
| 控制台 | https://simpfun.cn/console/ins/895060/ |
| 游戏入口 | `play.simpfun.cn:17184` |
| 镜像 | **MinecraftJE · Spigot · 1.21** |
| 规格 | 4 核 / 16GB / 16GB 盘 · 50 积分/天 |
| JDK | 启动脚本使用 **OpenJDK 21** |
| 插件目录 | `/plugins`（目前几乎为空，仅有 `PluginMetrics`） |
| `online-mode` | **false**（便于非正版/测试客户端进服） |
| 面板状态(调研时) | **已离线**；连续关闭 7 天会被销毁（当时提示约至 2026-11-08） |

> 面板「游戏端口」是映射口 `17184`；`server.properties` 内 `server-port` 可能是容器内端口（曾见 `13801`），进服请用面板显示的 `play.simpfun.cn:17184`。

---

## 2. 对接方式总览

简幻欢**不是**给你 SSH 进 Linux 改系统，而是「面板 + 文件 + 终端命令」。和本地开发最稳的闭环是：

```
本地改插件 → ./gradlew build → SFTP 上传 jar 到 plugins/
        → 面板「启动/重启」→ MC 客户端连 play.simpfun.cn:17184
        → 看终端日志 / 游戏内验证 → 需要时再拉配置回本地
```

| 通道 | 作用 | 推荐度 |
|---|---|---|
| **SFTP** | 上传插件 jar、拉配置/日志 | ★★★ 自动化主力 |
| **网页文件管理** | 偶发改配置、解压 | ★★ 备用 |
| **网页终端** | `reload`/`op`、看启动日志（**无需加/**） | ★★★ 联调必用 |
| 游戏客户端 | 合法号 + VM 内作弊端对打 | ★★★ 真机验收 |
| 官方开放 API | 不稳定/非官方文档为主 | 暂不依赖 |

---

## 3. SFTP（本地 ↔ 实例文件）

在实例 **管理** 页可见「SFTP文件管理服务器」：

| 项 | 值 |
|---|---|
| Host | `sfe4-connect.simpfun.cn` |
| Port | `2046` |
| 用户名 / 密码 | 面板「小眼睛」查看（**写入本地 `.secrets/`，勿提交仓库**） |

根目录即服务端根目录，关键路径：

- `plugins/` — 放反作弊 jar 与依赖（如 PacketEvents）
- `server.properties` / `spigot.yml` / `start.sh`
- 世界目录首次开服后生成

本地一键探测（需先配置 `.secrets/simpfun.env`）：

```bash
./tools/simpfun-sftp.sh ls plugins
./tools/deploy-to-simpfun.sh path/to/YourPlugin.jar
```

也可用 WinSCP / Cyberduck / Cursor 内置 SFTP。

---

## 4. 推荐开发联调流程

### 4.1 一次性准备

1. 复制 `.secrets/simpfun.env.example` → `.secrets/simpfun.env`，填入 SFTP 账号。
2. 面板启动实例，客户端验证能进 `play.simpfun.cn:17184`。
3. 终端给测试号 OP（面板命令**不要**加 `/`），例如：`op 你的ID`。
4. （强烈建议）评估是否 **重装为 Paper 1.21.x**：HANDOFF 与 PacketEvents/现代反作弊生态更贴 Paper；当前镜像是 **Spigot 1.21**。重装前先备份。

### 4.2 每日/每次改插件

1. 本地构建插件 jar。
2. `./tools/deploy-to-simpfun.sh build/libs/xxx.jar` 上传到 `plugins/`。
3. 面板 **重启**（热 `reload` 对反作弊/包监听类插件往往不够干净，优先重启）。
4. 终端确认加载成功（看启用日志 / `plugins` 列表）。
5. 合法客户端做误报基线；VM 内作弊端做检出验证。
6. 需要复盘时用 SFTP 拉 `plugins/<插件>/` 配置与自定义日志目录。

### 4.3 「在线调试」现实边界

| 能做 | 不能直接做 |
|---|---|
| 远程热替换 jar + 重启验证 | 对本机 IDE **远程 JDWP 断点调试**（面板一般不暴露调试端口） |
| 终端实时看服务端日志 | 在容器里随意装 profiler / 开任意端口 |
| SFTP 同步配置与录制文件 | 假设公网服可长期挂作弊测试而不影响公益玩家 |

若需要断点级调试：在**本地 Paper/Spigot 1.21** 镜像服调试逻辑，再用同一构建产物上简幻欢做「真网 + 真客户端」验收。

---

## 5. 与反作弊专项的配合建议

1. **公益正式时段**与**作弊对抗实验**尽量错开，或另建「测试用」实例，避免误伤玩家。
2. 当前盘占用约 0、插件空：很适合先当「干净对抗靶场」；若这就是唯一公益服，上线检测前先做备份。
3. `online-mode=false` 利于 Meteor 等测试，但公网风险更高——配合白名单/登录插件/仅内测 IP 策略按运营需要收紧。
4. 流量：管理页有「每日上行免费额度」类限制，大世界备份/频繁传大包注意别打满。
5. 积分：50 积分/天档；需保持签到/续期，避免实例因长期关机被销毁。

---

## 6. 安全

- 面板登录密码、SFTP 密码**只放** `.secrets/`（已 gitignore）。
- 聊天里发过的面板密码应视为泄露：**尽快在简幻欢改密**，并视情况重置 SFTP 密码（管理页 `lock_reset`）。
- 仓库文档只写 Host/端口/实例号等非机密信息，不写账号口令。

---

## 7. 待办 / 待确认

- [x] 作弊客户端版本对齐：Phase 1 使用 **Meteor 0.5.8**（depends `1.21`/`1.21.1`），见 `cheat-mod/`
- [ ] 是否将镜像从 Spigot 1.21 **换成 Paper 1.21.x**
- [ ] 是否单独开「对抗测试实例」，与公益游玩实例隔离
- [ ] 正式服玩家白名单 / 权限组（LuckPerms 等）是否已有，检测处罚如何对接
- [ ] 验证 `.secrets/simpfun.env` 后跑通 `deploy-to-simpfun.sh`
- [ ] VM 内 Fabric 1.21/1.21.1 + Meteor 0.5.8 实测进 `play.simpfun.cn:17184`

