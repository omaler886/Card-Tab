# CalorieLens

一个可直接打包成 Android APK 的每日卡路里记录器，支持：

- Room 本地数据库持久化饮食记录
- 手填食物并自动估算常见食物热量
- 拍照或从相册导入图片
- 真实食品条码扫描
- 连续扫码模式
- OCR + AI 识别食物并生成热量估算
- 联网食品数据库查询
- 内置中式/美式食谱，并判断加入后是否更接近日目标
- 历史统计图表
- 本地 JSON 导出 / 恢复
- WebDAV 云端备份 / 恢复
- AES-GCM 备份文件加密
- WorkManager 自动定时云备份
- 账号注册 / 登录 / 多设备云同步
- 内置隐私页、启动页和自定义应用图标
- 根据年龄、身高、体重、活动量、目标自动计算每日建议摄入

## 数据源

- `Open Food Facts`：优先用于条码包装食品查询，适合中国和全球商品
- `USDA FoodData Central`：用于美国品牌食品和文本搜索，当前默认使用 `DEMO_KEY`
- `扩展本地库`：补充常见中式菜品与美式餐食，离线也可搜索
- `Room`：保存本地饮食历史，供趋势图表和后续统计复用
- `WebDAV`：用于无需自建后端的云端备份，可对接坚果云、Nextcloud、群晖等 DAV 服务
- `FastAPI Sync Backend`：用于真正的账号体系和多设备同步

## 构建

```powershell
.\gradlew.bat assembleDebug
```

说明：

- 项目已在 `D:\New project\jdk21\jdk-21.0.10+7` 固定 Gradle 使用的 JDK 21
- 这样可以避开系统默认 JDK 25 与当前构建链的兼容问题
- release keystore 位于 `signing\calorielens-release.jks`
- release 签名配置位于 `keystore.properties`
- release 已启用 R8 混淆和资源收缩

APK 输出目录：

```text
app\build\outputs\apk\debug\app-debug.apk
```

Release 构建：

```powershell
.\gradlew.bat assembleRelease
```

Release APK 输出目录：

```text
app\build\outputs\apk\release\app-release.apk
```

## GitHub Actions

仓库根目录已提供：

```text
.github/workflows/build-calorielens.yml
```

默认行为：

- 后端先做 `health` smoke test
- 后端再做 Docker 镜像构建
- 安卓默认产出可安装的 `debug APK` artifact
- 如果仓库配置了签名 secrets，还会额外产出 `release APK` artifact

用于签名 release 的 GitHub Secrets：

- `CALORIELENS_KEYSTORE_BASE64`
- `CALORIELENS_STORE_PASSWORD`
- `CALORIELENS_KEY_ALIAS`
- `CALORIELENS_KEY_PASSWORD`

## 备份与恢复

- 设置 `备份加密口令` 后，导出、WebDAV 上传和账号云同步都会先加密再上传
- 本地导出：应用内选择 `导出 JSON`
- 本地恢复：应用内选择 `恢复 JSON`
- 云端备份：填写 WebDAV 地址、用户名、密码、远程路径后，使用 `上传云端`
- 云端恢复：同一组 WebDAV 配置下使用 `云端恢复`
- 自动备份：在应用内开启自动备份、选择目标和间隔小时，后台会通过 WorkManager 周期执行
- 账号同步：填写同步后端地址、邮箱、密码后，可在应用内注册、登录、上传同步、拉取同步

默认远程路径示例：

```text
CalorieLens/backup.json
```

本地测试同步 backend 默认地址：

```text
http://10.0.2.2:8000
```

隐私页资源：

```text
app\src\main\assets\privacy_policy.html
```

## AI 接口说明

应用默认按 OpenAI 兼容的 `chat/completions` 图像输入格式发送请求。

- 默认接口地址：`https://api.openai.com/v1/chat/completions`
- 默认模型：`gpt-4.1-mini`
- 如果你用的是其他兼容服务，请在应用内填完整接口地址和模型名

不配置 API Key 时，手填、目标计算、食谱推荐仍然可用；拍照识别会退化为 OCR 文本匹配。
