# LinAntiAim（Phase 1 · 录包）

服务端插件：先采集 yaw/pitch / 攻击 / 挥手，再写检测。  
会话矩阵见 [`../docs/rea/meteor/captures.md`](../docs/rea/meteor/captures.md)。

## 构建

```bash
cd plugin
./gradlew jar
# 产物: build/libs/LinAntiAim-0.1.0-SNAPSHOT.jar
```

## 部署（简幻欢）

1. 下载 [PacketEvents](https://modrinth.com/plugin/packetevents) **2.14.x** Spigot 构建，上传到 `plugins/`。
2. 上传本插件：
   ```bash
   ./tools/deploy-to-simpfun.sh plugin/build/libs/LinAntiAim-0.1.0-SNAPSHOT.jar LinAntiAim.jar
   ```
3. 面板**重启**实例（不要只 `reload`）。
4. 终端确认两个插件都启用。

## 游戏内

```
/ac record start B1-ka-always-default
# …战斗…
/ac record stop
/ac record status
```

文件：`plugins/LinAntiAim/records/*.jsonl`  
本地汇总：`python3 tools/summarize-capture.py replay/captures/*.jsonl`
