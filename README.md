# Clash Auto Change

自动切换 Clash 代理节点的工具，根据节点延迟自动选择最佳节点。

## 功能特点

- 支持多个策略组配置
- 自动测试节点延迟并切换到最佳节点
- 支持优先节点设置
- Web 界面管理配置
- 支持用户认证和个人资料管理
- Docker 支持

## 初始账号密码

- **用户名**: admin
- **密码**: 123456

首次登录后，建议立即修改默认密码。

## 使用说明

### 登录系统

1. 访问系统首页，输入用户名和密码登录
2. 可以勾选"记住我"选项，保持登录状态7天

### 修改个人资料

1. 点击右上角用户菜单，选择"个人资料"
2. 可以修改用户名和密码
3. 修改用户名后需要重新登录

### 配置策略组

1. 点击左侧菜单"策略组配置"
2. 添加新的策略组配置，填写以下信息：
   - 策略组名称：Clash中的策略组名称
   - 优先节点：优先使用的节点名称
   - 测试URL：用于测试节点延迟的URL
   - 超时时间：测试超时时间（毫秒）
   - 最大延迟：可接受的最大延迟（毫秒）
   - 启用状态：是否启用此配置

### 系统设置

1. 点击左侧菜单"系统设置"
2. 配置Clash API相关参数：
   - API基础URL：Clash API的地址
   - API密钥：Clash API的密钥

## Docker 部署

### 使用 Docker Compose（推荐）

1. 克隆本仓库：

```bash
git clone https://github.com/yourusername/clash-auto-change.git
cd clash-auto-change
```

2. 修改 `docker-compose.yml` 文件中的环境变量：

```yaml
environment:
  # Clash API 配置
  - CLASH_API_BASE_URL=http://localhost:9090  # Clash API 地址
  - CLASH_API_SECRET=                    # Clash API 密钥
  
  # 应用服务器配置
  - SERVER_PORT=7899                                     # 应用程序端口
  
  # 自动切换配置
  - CLASH_AUTO_CHANGE_CHECK_INTERVAL=5000                # 自动切换节点任务查询间隔（毫秒），默认为5秒
```

> 注意：`host.docker.internal` 是 Docker 中访问宿主机的特殊域名。如果您的 Clash 运行在其他主机上，请替换为相应的 IP 地址。

3. 创建数据库目录：

```bash
mkdir -p db
```

4. 启动服务：

```bash
docker-compose up -d
```

5. 访问 Web 界面：

```
http://localhost:7899
```

### 使用 Docker 命令

如果您不想使用 Docker Compose，也可以直接使用 Docker 命令：

```bash
# 构建镜像
docker build -t clash-auto-change .

# 运行容器
docker run -d \
  --name clash-auto-change \
  -p 7899:7899 \
  -v $(pwd)/db:/app/db \
  -e CLASH_API_BASE_URL=http://host.docker.internal:9090 \
  -e CLASH_API_SECRET=your-secret-key \
  -e SERVER_PORT=7899 \
  -e CLASH_AUTO_CHANGE_CHECK_INTERVAL=5000 \
  --restart unless-stopped \
  clash-auto-change
```

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| CLASH_API_BASE_URL | Clash API 的基础 URL | http://127.0.0.1:9090 |
| CLASH_API_SECRET | Clash API 的密钥 | - |
| SERVER_PORT | 应用程序端口 | 7899 |
| SPRING_DATASOURCE_URL | SQLite 数据库路径 | jdbc:sqlite:/app/db/clash_auto_change.db |
| CLASH_AUTO_CHANGE_TEST_URL | 测试URL | https://www.gstatic.com/generate_204 |
| CLASH_AUTO_CHANGE_CHECK_INTERVAL | 定时任务检查间隔(毫秒) | 5000 |

## 数据持久化

应用程序的数据存储在 SQLite 数据库中，默认位置为 `/app/db/clash_auto_change.db`。通过 Docker 卷挂载，数据会持久化到宿主机的 `./db` 目录。

## 配置说明

编辑`application.properties`进行配置：

```properties
# Clash API 配置
clash.api.base-url=${CLASH_API_BASE_URL:http://127.0.0.1:9090}  # Clash API 地址
clash.api.secret=${CLASH_API_SECRET:}                           # Clash API 密钥

# 自动切换配置
clash.auto-change.test-url=https://www.gstatic.com/generate_204  # 测试URL
clash.auto-change.timeout=5000                                   # 超时时间(毫秒)
clash.auto-change.check-interval=5000                            # 检查间隔(毫秒)

# SQLite 配置
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:sqlite:db/clash_auto_change.db}  # SQLite数据库文件路径
```

## 数据库配置

应用程序使用SQLite数据库存储配置信息。主要数据表包括：

- `proxy_group_config`: 存储策略组配置
  - `id`: 主键
  - `group_name`: 策略组名称
  - `preferred_proxy`: 优先节点名称
  - `test_url`: 测试URL
  - `timeout`: 超时时间(毫秒)
  - `max_delay`: 最大可接受延迟(毫秒)
  - `enabled`: 是否启用

- `admin_user`: 存储管理员用户信息
  - `id`: 主键
  - `username`: 用户名
  - `password`: 加密密码

## API接口

### Clash API接口

- `GET /api/clash/proxies` - 获取所有代理
- `GET /api/clash/proxies/{proxyName}/delay?url=xxx&timeout=5000` - 测试代理延迟
- `PUT /api/clash/proxies/{proxyGroup}` - 为策略组选择代理

### 配置管理接口

- `GET /api/config` - 获取所有策略组配置
- `GET /api/config/{id}` - 根据ID获取策略组配置
- `POST /api/config` - 创建或更新策略组配置
- `DELETE /api/config/{id}` - 删除策略组配置

## 构建与运行

```bash
# 构建项目
./mvnw clean package

# 运行应用
java -jar target/clash-auto-change-0.0.1-SNAPSHOT.jar
```

## 使用示例

### 添加策略组配置

```bash
curl -X POST http://localhost:8080/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "groupName": "Proxy",
    "preferredProxy": "HK01",
    "testUrl": "https://www.gstatic.com/generate_204",
    "timeout": 5000,
    "maxDelay": 500,
    "enabled": true
  }'
```

## 工作原理

1. 应用程序定期检查已配置的策略组
2. 首先测试优先节点的延迟，如果可用且延迟低于阈值，则切换到优先节点
3. 如果优先节点不可用，则测试策略组中的所有节点，并切换到延迟最低的节点
4. 所有配置和状态都保存在SQLite数据库中

## 参考资料

本项目使用 [MetaCubeX的Clash API](https://wiki.metacubex.one/api/)。 