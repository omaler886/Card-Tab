# Card-Tab 自定义配置

这份文档只写当前版本实际可用的自定义项，默认面向 Cloudflare Worker 的“变量与机密”配置页。

## 可配置项

| 名称 | 建议类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ADMIN_PASSWORD` | Secret | 是 | 后台管理密码 |
| `TOKEN_EXPIRY_MINUTES` | Variable | 否 | 登录有效期，单位分钟，默认 `30` |
| `BACKGROUND_IMAGE_URLS` | Variable / Secret | 否 | 随机背景图列表 |
| `BACKGROUND_BING_MARKET` | Variable | 否 | 默认 Bing 壁纸市场，默认 `zh-CN` |

## `BACKGROUND_IMAGE_URLS` 写法

支持两种格式。

### 1. JSON 数组

```json
[
  "https://example.com/bg-01.jpg",
  "https://example.com/bg-02.webp",
  "https://example.com/bg-03.avif"
]
```

### 2. 换行或逗号分隔

```text
https://example.com/bg-01.jpg
https://example.com/bg-02.webp
https://example.com/bg-03.avif
```

或者：

```text
https://example.com/bg-01.jpg,https://example.com/bg-02.webp,https://example.com/bg-03.avif
```

## 背景图建议

- 尽量使用可直接访问的原图地址，不要用需要二次跳转的页面链接
- 建议横图，分辨率不低于 `1600x900`
- 建议图片数量至少 `3` 张，这样随机切换观感更自然
- 如果图片地址带签名参数或不想公开，可以把 `BACKGROUND_IMAGE_URLS` 放到 Secret

## 不配置背景图时会怎样

- Worker 会先拉取 Bing 最近一组壁纸并随机轮播
- 如果 Bing 源不可用，再回退到内置的 SVG 背景图组
- 仍然会保留随机切换和缓慢漂移动画

## Bing 壁纸市场

如果你不想自己提供 `BACKGROUND_IMAGE_URLS`，可以只设置：

```text
BACKGROUND_BING_MARKET=zh-CN
```

常见值：

- `zh-CN`
- `en-US`
- `ja-JP`
- `en-GB`

## 天气说明

当前版本天气默认使用：

- [Open-Meteo Forecast API](https://open-meteo.com/en/docs)
- [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api)

因此不再需要额外配置天气 API Key。

## Cloudflare 配置建议

- `ADMIN_PASSWORD` 放 Secret
- `TOKEN_EXPIRY_MINUTES` 放 Variable
- `BACKGROUND_IMAGE_URLS`：
  - 公共图片地址放 Variable
  - 私有或签名图片地址放 Secret

## 一句话示例

最小可用配置：

- Secret: `ADMIN_PASSWORD`
- KV 绑定: `CARD_ORDER`

带自定义背景的配置：

- Secret: `ADMIN_PASSWORD`
- Variable: `TOKEN_EXPIRY_MINUTES=60`
- Variable 或 Secret: `BACKGROUND_IMAGE_URLS=[...]`
- KV 绑定: `CARD_ORDER`
