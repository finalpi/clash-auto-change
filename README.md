# Clash Auto Change

自动切换 Clash 代理节点的工具，根据节点延迟自动选择最佳节点。

## 功能特点

- 支持多个策略组配置
- 自动测试节点延迟并切换到最佳节点
- 支持优先节点设置
- Web 界面管理配置
- Docker 支持

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
  - CLASH_API_BASE_URL=http://host.docker.internal:9090  # Clash API 地址
  - CLASH_API_SECRET=your-secret-key                     # Clash API 密钥
  - SERVER_PORT=7899                                     # 应用程序端口
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
  -v $(pwd)/data:/app/data \
  -e CLASH_API_BASE_URL=http://host.docker.internal:9090 \
  -e CLASH_API_SECRET=your-secret-key \
  -e SERVER_PORT=7899 \
  --restart unless-stopped \
  clash-auto-change
```

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| CLASH_API_BASE_URL | Clash API 的基础 URL | http://host.docker.internal:9090 |
| CLASH_API_SECRET | Clash API 的密钥 | - |
| SERVER_PORT | 应用程序端口 | 7899 |
| SPRING_DATASOURCE_URL | SQLite 数据库路径 | jdbc:sqlite:/app/data/clash_auto_change.db |

## 数据持久化

应用程序的数据存储在 SQLite 数据库中，默认位置为 `/app/data/clash_auto_change.db`。通过 Docker 卷挂载，数据会持久化到宿主机的 `./data` 目录。

## 配置说明

编辑`application.properties`进行配置：

```properties
# Clash API 配置
clash.api.base-url=http://127.0.0.1:9097  # Clash API 地址
clash.api.secret=set-your-secret           # Clash API 密钥

# 自动切换配置
clash.auto-change.test-url=https://www.gstatic.com/generate_204  # 测试URL
clash.auto-change.timeout=5000                                   # 超时时间(毫秒)
clash.auto-change.check-interval=5000                            # 检查间隔(毫秒)

# SQLite 配置
spring.datasource.url=jdbc:sqlite:clash_auto_change.db           # SQLite数据库文件路径
```

## 数据库配置

应用程序使用SQLite数据库存储策略组和优先节点配置。数据库表结构如下：

- `proxy_group_config`: 存储策略组配置
  - `id`: 主键
  - `group_name`: 策略组名称
  - `preferred_proxy`: 优先节点名称
  - `test_url`: 测试URL
  - `timeout`: 超时时间(毫秒)
  - `max_delay`: 最大可接受延迟(毫秒)
  - `enabled`: 是否启用

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