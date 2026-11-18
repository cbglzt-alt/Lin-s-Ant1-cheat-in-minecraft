# LinAntiAim（Phase 1 · 录包 + Aim* 检测 + 分级处罚）

服务端插件：`/ac record` + **AimSnap / AimAttackAlign / AuraSwitch** + VL 分档处罚。  
计划：[`../docs/detections/implementation-plan.md`](../docs/detections/implementation-plan.md)  
检测阈值：[`../docs/detections/aim-provisional.md`](../docs/detections/aim-provisional.md)  
处罚分档：[`../docs/detections/punishment.md`](../docs/detections/punishment.md)

## 命令

```
/ac record start <label>   /ac record stop   /ac record status
/ac vl        # 查看累计 VL
/ac reset     # 清零（测试）
```

## 构建

```bash
cd plugin
./gradlew jar
# 产物: build/libs/LinAntiAim-0.3.0-SNAPSHOT.jar
```

## 部署（简幻欢）

1. PacketEvents **2.14.x** → `plugins/`  
2. 上传本 jar 为 `LinAntiAim.jar`  
3. 面板**重启**（不要只 reload）  
4. 确认控制台：`AimSnap=true`、`punish` 保持关  

## 游戏内

```
/ac record start B2-ka-onhit
# OnHit 战斗 — 应出现 AimSnap 中档告警（OP / linantiaim.alerts）
/ac record stop
```

- 录包：`plugins/LinAntiAim/records/*.jsonl`  
- 告警：控制台 + `plugins/LinAntiAim/alerts/alerts-YYYYMMDD.jsonl`（含 `captureLabel`）  
- Align：仅 DEBUG（`alerts.log_debug: true` 才打控制台）  

本地汇总：`python tools/summarize-capture.py replay/captures/*.jsonl`
