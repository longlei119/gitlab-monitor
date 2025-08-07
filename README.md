# GitLab 研发度量系统后端

基于Spring Boot 2.7.x的GitLab研发度量系统后端服务，支持JDK 1.8。

## 功能特性

- GitLab Webhook事件接收和处理
- 代码提交量统计和分析
- 代码质量分析集成
- Bug修复效率跟踪
- 代码评审制度管理
- 单元测试覆盖率考核
- RESTful API接口
- 实时数据处理和缓存
- 系统监控和健康检查

## 技术栈

- **框架**: Spring Boot 2.7.18
- **数据库**: MySQL 8.0
- **缓存**: Redis 6.2
- **消息队列**: RabbitMQ 3.11
- **构建工具**: Maven 3.6+
- **部署**: Docker + Docker Compose

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- Docker & Docker Compose (可选)

### 本地开发

1. 克隆项目
```bash
git clone <repository-url>
cd gitlab-metrics-backend
```

2. 启动依赖服务 (MySQL, Redis, RabbitMQ)
```bash
docker-compose up -d mysql redis rabbitmq
```

3. 配置环境变量 (可选)
```bash
export DATABASE_URL=jdbc:mysql://localhost:3306/gitlab_metrics_dev
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=root
export REDIS_HOST=localhost
export RABBITMQ_HOST=localhost
```

4. 运行应用
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker部署

1. 构建并启动所有服务
```bash
docker-compose up -d
```

2. 查看服务状态
```bash
docker-compose ps
```

3. 查看应用日志
```bash
docker-compose logs -f app
```

## 配置说明

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| SPRING_PROFILES_ACTIVE | dev | 运行环境 (dev/prod/test) |
| DATABASE_URL | jdbc:mysql://localhost:3306/gitlab_metrics | 数据库连接URL |
| DATABASE_USERNAME | root | 数据库用户名 |
| DATABASE_PASSWORD | root | 数据库密码 |
| REDIS_HOST | localhost | Redis主机地址 |
| REDIS_PORT | 6379 | Redis端口 |
| RABBITMQ_HOST | localhost | RabbitMQ主机地址 |
| RABBITMQ_PORT | 5672 | RabbitMQ端口 |
| RABBITMQ_USERNAME | admin | RabbitMQ用户名 |
| RABBITMQ_PASSWORD | admin | RabbitMQ密码 |
| GITLAB_WEBHOOK_SECRET | your-webhook-secret | GitLab Webhook密钥 |

### 配置文件

- `application.yml` - 主配置文件
- `application-dev.yml` - 开发环境配置
- `application-prod.yml` - 生产环境配置
- `application-test.yml` - 测试环境配置

## API接口

### 健康检查

```bash
GET /api/health/check
```

### 系统监控

```bash
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

## 开发指南

### 项目结构

```
src/main/java/com/gitlab/metrics/
├── config/          # 配置类
├── controller/      # 控制器
├── service/         # 业务服务
├── repository/      # 数据访问层
├── entity/          # 实体类
├── dto/             # 数据传输对象
└── util/            # 工具类
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ConfigurationTests
```

### 构建项目

```bash
# 编译项目
mvn compile

# 打包项目
mvn package

# 跳过测试打包
mvn package -DskipTests
```

## 监控和日志

### 日志文件

- 开发环境: 控制台输出
- 生产环境: `/app/logs/gitlab-metrics.log`
- 错误日志: `/app/logs/gitlab-metrics-error.log`

### 监控指标

- Prometheus指标: `/actuator/prometheus`
- 应用健康状态: `/actuator/health`
- 应用信息: `/actuator/info`

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库服务是否启动
   - 验证连接参数是否正确
   - 确认数据库用户权限

2. **Redis连接失败**
   - 检查Redis服务状态
   - 验证Redis配置参数
   - 检查网络连接

3. **RabbitMQ连接失败**
   - 确认RabbitMQ服务运行
   - 检查用户名密码
   - 验证虚拟主机配置

### 日志级别调整

在配置文件中调整日志级别:
```yaml
logging:
  level:
    com.gitlab.metrics: DEBUG
    org.springframework.security: DEBUG
```

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 许可证

本项目采用MIT许可证。