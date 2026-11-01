# 动态录包 · Meteor 0.5.8（Phase 1）

目标：用服务端视角序列填满 [`killaura.md`](killaura.md) / [`rotations.md`](rotations.md) 第 4 节，再定 Aim* 阈值。

## 0. 环境（锁定）

| 侧 | 要求 |
|---|---|
| 作弊客户端 | Fabric **1.21 或 1.21.1** + **`meteor-client-0.5.8-1.21.1.jar`**（仅隔离 VM） |
| 合法对照客户端 | 原版 / 无战斗作弊的 Fabric，同协议进服 |
| 服务端 | 简幻欢 Spigot **1.21** `play.simpfun.cn:17184`，或本地同版本 Paper/Spigot |
| 录包插件 | 本仓库 `plugin/` → 构建后 `./tools/deploy-to-simpfun.sh …` |
| 依赖插件 | **PacketEvents**（与 `plugin/build.gradle.kts` 同大版本）放入 `plugins/` |

## 1. 会话矩阵（必须跑完的最小集）

每场建议 **45–90s**，同一块场地、同一假人/对手距离（约 2.5–4 格）。  
标签必须与 `/ac record start <label>` **完全一致**，便于脚本汇总。

| ID | label | 客户端 | Meteor 配置 | 要观察的特征 |
|---|---|---|---|---|
| **A1** | `A1-legit-melee` | 合法 | 无 KA | 人类 Δyaw/Δpitch、攻击对齐噪声、GCD |
| **B1** | `B1-ka-always-default` | 0.5.8 | KA **默认**（`rotate=Always`） | Snap、贴 Body、无 GCD、AttackOrder |
| **B2** | `B2-ka-onhit` | 0.5.8 | 仅改 `rotate=OnHit` | 尖峰仅在攻击附近 |
| **B3** | `B3-ka-none` | 0.5.8 | 仅改 `rotate=None` | Aim 弱；Reach/视线是否仍能打中 |
| **C1** | `C1-ka-max3` | 0.5.8 | 默认 + `max-targets=3` + 多目标 | AuraSwitch |
| **C2** | `C2-ka-only-on-look` | 0.5.8 | `only-on-look=true` | 更像手动时的对齐分布 |

可选加分：`C3-ka-custom-delay-5`（`custom-delay` + 低 hit-delay）——非门禁。

HANDOFF 对照组：A = 合法，B = 默认，C = 绕过/变体（OnHit / None / only-on-look / 多目标）。

## 2. 操作步骤（每场）

1. 服务端已加载 `LinAntiAim` + PacketEvents；OP 自己。
2. 作弊 VM / 合法端进服，站到约定点；准备好假人或互砍同伴。
3. 服务端执行：
   ```
   ac record start <label>
   ```
4. 按该场配置战斗至结束（不要中途改 label）。
5. ```
   ac record stop
   ```
6. 确认生成：`plugins/LinAntiAim/records/<timestamp>_<player>_<label>.jsonl`
7. SFTP 拉回本地 `replay/captures/`（目录可自建；勿提交含玩家敏感信息的大包，可只交汇总）。

一键拉取示例（有 SFTP 凭据时）：

```bash
./tools/simpfun-sftp.sh get plugins/LinAntiAim/records ./replay/captures
```

## 3. 记录字段（插件 JSONL）

| type | 含义 |
|---|---|
| `session` | 开录元数据：label、玩家、协议时间 |
| `look` | 带朝向的移动/旋转包：`yaw,pitch,x,y,z,onGround` |
| `attack` | 攻击实体：目标 id/类型/位置、当时 look、`alignDeg`（视线到目标 Body 中心夹角） |
| `swing` | 挥手动画 |
| `session_end` | 停录：样本计数 |

## 4. 汇总 → 写回分析卡 §4

```bash
python3 tools/summarize-capture.py replay/captures/*B1*.jsonl
python3 tools/summarize-capture.py replay/captures/*A1*.jsonl
```

把输出的 Δyaw/Δpitch 分位数、攻击瞬间 `alignDeg`、可疑 GCD 比例填进 `killaura.md` §4，并注明 **数据文件名 / 日期**。

## 5. 验收（动态完成定义）

- [ ] A1 + B1 + B2 + B3 至少各 1 场有效 jsonl  
- [ ] C1 或 C2 至少 1 场  
- [ ] `killaura.md` §4 表不再是纯假设  
- [ ] 明确 1–2 个「默认 Always 可稳定分开合法」的候选阈值（仍标 provisional）  

未完成前：**不进入 S3 大规模写死阈值**（可继续完善录包插件）。
