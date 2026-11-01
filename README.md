# Lin-s-Ant1-cheat-in-minecraft

第一期：**锁定 Meteor Client 0.5.8（MC 1.21 / 1.21.1）** → REA → 服务端瞄准反作弊插件 → 简幻欢验证。

| 文档 | 内容 |
|---|---|
| [`HANDOFF.md`](HANDOFF.md) | 目标、REA、Phase 1 |
| [`cheat-mod/README.md`](cheat-mod/README.md) | 作弊 jar（**主目标 0.5.8**） |
| [`docs/rea/meteor/`](docs/rea/meteor/) | **唯一** 0.5.8 分析卡 + [`captures.md`](docs/rea/meteor/captures.md) |
| [`plugin/`](plugin/) | LinAntiAim 录包插件（`/ac record`） |
| [`docs/platform/simpfun.md`](docs/platform/simpfun.md) | 简幻欢对接 |

**当前步骤**：部署 `plugin/` + PacketEvents → 按 `captures.md` 跑 A1/B1/B2/B3 → `tools/summarize-capture.py` 回填分析卡 §4。  
进服：`play.simpfun.cn:17184`（Spigot 1.21）
