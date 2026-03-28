# CalorieLens Sync Backend

一个本地可运行的账号同步后端，提供：

- 用户注册 / 登录
- 刷新令牌轮换
- 邮箱验证
- 找回密码 / 重置密码
- 访问令牌鉴权
- 上传加密备份快照
- 拉取最新快照
- 多设备最近同步记录
- `health` 健康检查
- 结构化同步冲突响应

## 启动

```powershell
python -m venv .venv
.\.venv\Scripts\python -m pip install -r requirements.txt
.\.venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

默认会把验证邮件和重置密码邮件写到：

```text
data/outbox/
```

如果配置了 SMTP 环境变量，也会同时发送真实邮件。

## 安卓端默认地址

模拟器可直接使用：

```text
http://10.0.2.2:8000
```

本机调试健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

## Docker

本地容器运行：

```powershell
docker compose up --build
```

环境变量模板：

```text
.env.example
```

## 公网部署

已提供：

- [Dockerfile](/D:/New%20project/calorielens-sync-backend/Dockerfile)
- [docker-compose.yml](/D:/New%20project/calorielens-sync-backend/docker-compose.yml)
- [docker-compose.public.yml](/D:/New%20project/calorielens-sync-backend/deploy/docker-compose.public.yml)
- [Caddyfile](/D:/New%20project/calorielens-sync-backend/deploy/Caddyfile)

公网部署建议步骤：

1. 复制 `.env.example` 为 `.env` 并填好域名和 SMTP。
2. 把 `deploy/Caddyfile` 中的域名 `sync.example.com` 改成你的真实域名。
3. 在服务器上运行：

```powershell
cd deploy
docker compose -f docker-compose.public.yml up -d --build
```

## 同步冲突策略

后端无法解密客户端快照，因此不在服务端做内容级 merge。当前策略是：

- 客户端上传时带上 `base_revision`
- 如果服务器已有更新且 `base_revision` 落后，就返回 `409 sync_conflict`
- 客户端可以选择先 `pull` 最新版本，或在确认后使用 `force=true` 强制覆盖
- 服务端会保留每次成功 push 的版本历史元数据
