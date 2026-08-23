# 软考智笔官网 + Redis Manager

一次 `docker compose up` 部署两个静态站，用路径区分：

| 路径 | 站点 |
|---|---|
| https://www.fondia.top/ | 软考智笔 |
| https://www.fondia.top/redishtml/ | Redis Manager |

## 本地预览

```bash
cd html
python3 -m http.server 8080
```

- 软考智笔：http://127.0.0.1:8080/
- Redis Manager：http://127.0.0.1:8080/redishtml/

## Docker 部署（域名 www.fondia.top）

证书放到服务器 `/home/docker/ssl/`：

- `www.fondia.top.pem`
- `www.fondia.top.key`

DNS 把 `www.fondia.top`（建议同时加 `fondia.top`）A 记录指到本机。宿主机 **80 / 443** 不要被占用。

```bash
cd html
docker compose up -d --build
```

证书不在默认路径时：

```bash
SSL_DIR=/你的证书目录 docker compose up -d --build
```

停止：

```bash
docker compose down
```

## 目录说明

| 目录 | 用途 |
|---|---|
| `./`（index.html、css） | 软考智笔官网 |
| `./redishtml/` | Redis Manager 宣传页（已挂载，改完刷新即可） |
| `./images/` | 软考智笔截图 |
| `./downloads/` | 软考智笔安装包 |

### 软考智笔截图

| 文件 | 用途 |
|---|---|
| workspace.png | 首页大图 |
| notes.png | 知识点 |
| essay.png | 论文 |
| case.png | 案例 |
| ai.png | AI 面板 |

### 软考智笔安装包

| 文件 | 平台 |
|---|---|
| ruankao-zhibi-windows-setup.exe | Windows 安装版 |
| ruankao-zhibi-windows-portable.exe | Windows 便携版 |
| ruankao-zhibi-macos.dmg | macOS |

Redis Manager 下载链目前指向 OSS；logo 放在 `redishtml/assets/logo.png`。
