# 软考智笔官网

静态宣传页，含 Windows / macOS 客户端下载入口。截图与安装包由你自行放入对应目录。

## 本地预览

用浏览器直接打开 `index.html`，或：

```bash
cd html
python3 -m http.server 8080
```

访问 http://127.0.0.1:8080

## Docker 部署

```bash
cd html
docker compose up -d --build
```

访问 http://服务器IP:8080

停止：

```bash
docker compose down
```

改端口：编辑 `docker-compose.yml` 里的 `"8080:80"`，左边是宿主机端口。

## 替换截图

把 PNG 放到 `images/`：

| 文件 | 用途 |
|---|---|
| workspace.png | 首页大图 |
| notes.png | 知识点 |
| essay.png | 论文 |
| case.png | 案例 |
| ai.png | AI 面板 |

`images` 已挂载进容器，替换后刷新浏览器即可。

## 替换安装包

把客户端放到 `downloads/`：

| 文件 | 平台 |
|---|---|
| ruankao-zhibi-windows-setup.exe | Windows 安装版 |
| ruankao-zhibi-windows-portable.exe | Windows 便携版 |
| ruankao-zhibi-macos.dmg | macOS |

打包完成后把 electron-builder 产物改成上述文件名再上传。
