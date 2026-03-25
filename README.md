# Card-Tab

Cloudflare Worker 版卡片式书签导航，支持公开/私密书签、分类管理、拖拽排序、自动主题、边缘时区天气和随机背景图。

完整更新历史见 [CHANGELOG.md](./CHANGELOG.md)。
自定义配置说明见 [README.CUSTOM.md](./README.CUSTOM.md)。

## 当前状态

- 首屏书签由 Worker 直接注入，页面不再依赖前端先请求 `/api/getLinks` 才能显示
- 主题支持 `自动 / 浅色 / 深色` 三态
- 天气使用 Cloudflare 边缘节点时区映射城市，并通过 Open-Meteo 获取实时天气和 3 日预报
- 背景支持随机图片轮播；未配置自定义图片时，默认使用内置 SVG 背景组
- 背景图片可通过 Cloudflare Worker 的“变量与机密”自定义

## 功能概览

### 界面

- 卡片式书签布局，支持桌面和移动端
- 毛玻璃顶部栏、天气胶囊、天气弹窗
- 黑白主题自动切换，并优化了暗色过渡
- 随机背景图片 + 动态光晕 + 网格背景

### 书签管理

- 分类新增、重命名、移动、删除
- 卡片新增、编辑、删除
- 公开/私密书签分离展示
- 拖拽排序并写回 KV

### 搜索

- 百度、必应、谷歌、DuckDuckGo
- 页面内书签搜索
- 分类快捷跳转和当前分类高亮

### 安全

- 基于 `ADMIN_PASSWORD` 的登录验证
- `TOKEN_EXPIRY_MINUTES` 控制登录有效期
- KV 自动备份最近 10 份数据

## 部署

### 1. 创建 Worker

在 Cloudflare Workers 中创建一个 Worker，把 [workers.js](./workers.js) 的内容部署上去。

### 2. 创建并绑定 KV

创建名为 `CARD_ORDER` 的 KV Namespace，并绑定到 Worker。

绑定名必须是：

- `CARD_ORDER`

### 3. 配置变量与机密

至少添加下面这些配置：

| 名称 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ADMIN_PASSWORD` | Secret | 是 | 后台管理密码，建议放机密 |
| `TOKEN_EXPIRY_MINUTES` | Variable | 否 | 登录有效期，默认 `30` |
| `BACKGROUND_IMAGE_URLS` | Variable / Secret | 否 | 自定义随机背景图列表，支持 JSON 数组或换行/逗号分隔 |

说明：

- `BACKGROUND_IMAGE_URLS` 不配也能用，页面会自动退回到内置背景图
- 如果你使用带签名参数的私有图片地址，可以把 `BACKGROUND_IMAGE_URLS` 放到 Secret
- 天气现在默认走 Open-Meteo，不需要额外天气 Key

### 4. 连接 Git 自动部署

如果你是通过 GitHub 仓库自动部署 Worker，确保 Cloudflare 跟踪的是你要发布的分支，通常是：

- `main`

### 5. 打开站点初始化

首次部署后，用配置的 `ADMIN_PASSWORD` 登录，再添加或导入书签数据。

## 数据兼容说明

- 旧版 2024.09.02 的轻量版代码仍保留在 [history](./history)
- 如果你从非常早期版本直接升级，旧数据结构可能不兼容，需要重新导入或整理数据

## 自定义配置

详细自定义项、背景图示例和变量写法见：

- [README.CUSTOM.md](./README.CUSTOM.md)

## 备注

- 天气接口来源：Open-Meteo
- 访问定位来源：Cloudflare `request.cf.timezone`
- 书签图标默认走 `faviconextractor`

适合轻量自托管和自行魔改。
