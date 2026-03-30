# FitPilot Android

原生 Android 健身软件原型，覆盖以下核心链路：

- 本地注册与登录
- 训练计划自动生成
- 饮食记录与建议
- 健身目标设定与进度监控
- 训练记录、最近 7 天分析与个性化反馈
- 主代理监督子代理的任务编排与 5 分钟审计

## 目录

- `app/src/main/java/com/codex/fitnessplatform/data`：持久化模型与仓储
- `app/src/main/java/com/codex/fitnessplatform/logic`：训练、营养、反馈、代理编排规则
- `app/src/main/java/com/codex/fitnessplatform/ui`：Compose 界面与 ViewModel

## 运行

```bash
cd android-app
./gradlew.bat assembleDebug
```

如果本机默认 `JAVA_HOME` 指向较新的 JDK 25，包装脚本会优先尝试使用仓库同级的 `jdk21/jdk-21.0.10+7`。
也可以手动指定：

```powershell
$env:FITNESS_PLATFORM_JAVA_HOME='D:\path\to\jdk-21'
.\gradlew.bat assembleDebug
```

安装生成的 APK 后即可在 Android 设备或模拟器中运行。

## 代理说明

- `Master Agent`：检查子代理健康度、发现超时或失败任务并重启
- `Auth Agent`：注册/登录链路健康检查
- `Training Agent`：训练计划生成与重算
- `Nutrition Agent`：饮食建议与热量执行监控
- `Tracking Agent`：训练日志与最近 7 天分析
- `Goal Coach Agent`：目标达成率和个性化反馈

说明：当前 5 分钟巡检在应用运行期间由 ViewModel 协程驱动；每次用户操作也会触发一次即时审计。
