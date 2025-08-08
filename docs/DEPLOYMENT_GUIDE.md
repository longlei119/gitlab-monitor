# GitLab研发度量系统部署和运维手册

## 目录

1. [系统要求](#系统要求)
2. [环境准备](#环境准备)
3. [快速部署](#快速部署)
4. [生产环境部署](#生产环境部署)
5. [配置说明](#配置说明)
6. [监控和告警](#监控和告警)
7. [备份和恢复](#备份和恢复)
8. [故障排除](#故障排除)
9. [性能调优](#性能调优)
10. [安全配置](#安全配置)

## 系统要求

### 硬件要求

**最小配置:**
- CPU: 2核心
- 内存: 4GB RAM
- 存储: 50GB SSD
- 网络: 100Mbps

**推荐配置:**
- CPU: 4核心
- 内存: 8GB RAM
- 存储: 200GB SSD
- 网络: 1Gbps

**生产环境配置:**
- CPU: 8核心
- 内存: 16GB RAM
- 存储: 500GB SSD (数据库) + 100GB SSD (应用)
- 网络: 1Gbps

### 软件要求

- **操作系统**: Linux (Ubuntu 20.04+, CentOS 7+) 或 Windows Server 2019+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Java**: OpenJDK 8 (仅开发环境需要)
- **Maven**: 3.6+ (仅开发环境需要)

## 环境准备

### 1. 安装Docker和Docker Compose

**Ubuntu/Debian:**
```bash
# 更新包索引
sudo apt update

# 安装Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.12.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 将用户添加到docker组
sudo usermod -aG docker $USER
```

**CentOS/RHEL:**
```bash
# 安装Docker
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# 启动Docker服务
sudo systemctl start docker
sudo systemctl enable docker

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.12.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

**Windows:**
1. 下载并安装Docker Desktop for Windows
2. 启用WSL2支持
3. 重启系统

### 2. 创建项目目录

```bash
# 创建项目目录
sudo mkdir -p /opt/gitlab-metrics
cd /opt/gitlab-metrics

# 设置目录权限
sudo chown -R $USER:$USER /opt/gitlab-metrics
```

### 3. 下载项目文件

```bash
# 克隆项目代码
git clone https://github.com/your-org/gitlab-metrics-backend.git .

# 或者下载发布包
wget https://github.com/your-org/gitlab-metrics-backend/releases/download/v1.0.0/gitlab-metrics-backend-1.0.0.tar.gz
tar -xzf gitlab-metrics-backend-1.0.0.tar.gz
```

## 快速部署

### 1. 开发环境部署

```bash
# 进入项目目录
cd /opt/gitlab-metrics

# 复制环境配置文件
cp .env.dev .env

# 编辑配置文件
nano .env

# 构建应用
mvn clean package -DskipTests

# 启动服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f app
```

### 2. 验证部署

```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health

# 检查API响应
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 生产环境部署

### 1. 环境配置

```bash
# 复制生产环境配置
cp .env.prod .env

# 编辑生产环境配置
nano .env
```

**重要配置项:**
```bash
# 数据库密码
DATABASE_PASSWORD=your-secure-password

# JWT密钥
JWT_SECRET=your-jwt-secret-key-change-this-in-production

# Webhook密钥
GITLAB_WEBHOOK_SECRET=your-production-webhook-secret

# 加密密钥
ENCRYPTION_KEY=your-32-character-encryption-key
```

### 2. SSL证书配置

```bash
# 生成自签名证书（测试用）
./scripts/generate-ssl-cert.sh

# 或者使用Let's Encrypt证书
sudo certbot certonly --standalone -d your-domain.com
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem docker/nginx/ssl/server.crt
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem docker/nginx/ssl/server.key
```

### 3. 部署应用

```bash
# 使用部署脚本
chmod +x scripts/deploy.sh
./scripts/deploy.sh prod

# 或者手动部署
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### 4. 配置反向代理（可选）

如果使用外部Nginx作为反向代理：

```nginx
# /etc/nginx/sites-available/gitlab-metrics
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /path/to/your/certificate.crt;
    ssl_certificate_key /path/to/your/private.key;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 配置说明

### 1. 应用配置

**application.yml核心配置:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

### 2. 数据库配置

**MySQL配置优化:**
```ini
[mysqld]
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
max_connections = 200
query_cache_size = 64M
tmp_table_size = 64M
max_heap_table_size = 64M
```

### 3. Redis配置

**redis.conf关键配置:**
```ini
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

### 4. RabbitMQ配置

**rabbitmq.conf关键配置:**
```ini
vm_memory_high_watermark.relative = 0.6
disk_free_limit.relative = 2.0
heartbeat = 60
```

## 监控和告警

### 1. 健康检查

```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# 数据库连接检查
docker-compose exec mysql mysqladmin ping

# Redis连接检查
docker-compose exec redis redis-cli ping

# RabbitMQ状态检查
docker-compose exec rabbitmq rabbitmq-diagnostics status
```

### 2. Prometheus监控

访问Prometheus界面：http://localhost:9090

**关键指标查询:**
```promql
# 应用响应时间
http_server_requests_seconds_sum / http_server_requests_seconds_count

# JVM内存使用率
jvm_memory_used_bytes / jvm_memory_max_bytes

# 数据库连接池使用率
hikaricp_connections_active / hikaricp_connections_max

# 队列消息积压
rabbitmq_queue_messages_ready
```

### 3. Grafana仪表板

访问Grafana界面：http://localhost:3000 (admin/admin)

**预配置仪表板:**
- 应用性能监控
- JVM监控
- 数据库监控
- 消息队列监控
- 业务指标监控

### 4. 日志监控

```bash
# 查看应用日志
docker-compose logs -f app

# 查看错误日志
docker-compose logs app | grep ERROR

# 查看访问日志
tail -f logs/nginx/access.log

# 日志轮转配置
logrotate -d /etc/logrotate.d/gitlab-metrics
```

## 备份和恢复

### 1. 数据库备份

```bash
# 创建备份脚本
cat > backup-db.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/gitlab-metrics/backups"
mkdir -p $BACKUP_DIR

# 备份数据库
docker-compose exec -T mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD gitlab_metrics > $BACKUP_DIR/gitlab_metrics_$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/gitlab_metrics_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Database backup completed: gitlab_metrics_$DATE.sql.gz"
EOF

chmod +x backup-db.sh

# 设置定时备份
crontab -e
# 添加以下行（每天凌晨2点备份）
0 2 * * * /opt/gitlab-metrics/backup-db.sh
```

### 2. 数据恢复

```bash
# 恢复数据库
gunzip gitlab_metrics_20240101_020000.sql.gz
docker-compose exec -T mysql mysql -u root -p$MYSQL_ROOT_PASSWORD gitlab_metrics < gitlab_metrics_20240101_020000.sql
```

### 3. 配置文件备份

```bash
# 备份配置文件
tar -czf config-backup-$(date +%Y%m%d).tar.gz \
  .env.prod \
  docker-compose.prod.yml \
  docker/nginx/nginx.conf \
  docker/mysql/my.cnf \
  docker/redis/redis.conf \
  docker/rabbitmq/rabbitmq.conf
```

## 故障排除

### 1. 常见问题

**应用启动失败:**
```bash
# 查看详细日志
docker-compose logs app

# 检查配置文件
docker-compose config

# 检查端口占用
netstat -tlnp | grep 8080
```

**数据库连接失败:**
```bash
# 检查数据库状态
docker-compose ps mysql

# 检查数据库日志
docker-compose logs mysql

# 测试数据库连接
docker-compose exec mysql mysql -u gitlab_user -p
```

**Redis连接失败:**
```bash
# 检查Redis状态
docker-compose ps redis

# 测试Redis连接
docker-compose exec redis redis-cli ping
```

**RabbitMQ连接失败:**
```bash
# 检查RabbitMQ状态
docker-compose ps rabbitmq

# 查看RabbitMQ管理界面
http://localhost:15672
```

### 2. 性能问题

**内存不足:**
```bash
# 检查内存使用
free -h
docker stats

# 调整JVM参数
JAVA_OPTS="-Xms1g -Xmx2g"
```

**磁盘空间不足:**
```bash
# 检查磁盘使用
df -h

# 清理Docker镜像
docker system prune -a

# 清理日志文件
find logs/ -name "*.log" -mtime +30 -delete
```

### 3. 网络问题

**端口访问问题:**
```bash
# 检查防火墙设置
sudo ufw status
sudo firewall-cmd --list-all

# 开放端口
sudo ufw allow 8080
sudo firewall-cmd --add-port=8080/tcp --permanent
```

## 性能调优

### 1. JVM调优

```bash
# 生产环境JVM参数
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:G1HeapRegionSize=16m \
  -XX:+UseStringDeduplication \
  -XX:+PrintGCDetails \
  -XX:+PrintGCTimeStamps \
  -Xloggc:logs/gc.log"
```

### 2. 数据库调优

```sql
-- 创建索引
CREATE INDEX idx_commits_developer_timestamp ON commits(developer_id, timestamp);
CREATE INDEX idx_quality_metrics_project_timestamp ON quality_metrics(project_id, timestamp);

-- 分析表统计信息
ANALYZE TABLE commits;
ANALYZE TABLE quality_metrics;

-- 优化查询
EXPLAIN SELECT * FROM commits WHERE developer_id = 'dev001' AND timestamp BETWEEN '2024-01-01' AND '2024-01-31';
```

### 3. 缓存优化

```yaml
# Redis缓存配置
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      cache-null-values: false
```

### 4. 连接池调优

```yaml
# HikariCP连接池配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## 安全配置

### 1. 网络安全

```bash
# 配置防火墙
sudo ufw enable
sudo ufw allow ssh
sudo ufw allow 80
sudo ufw allow 443
sudo ufw deny 3306  # 禁止外部访问数据库
sudo ufw deny 6379  # 禁止外部访问Redis
```

### 2. SSL/TLS配置

```nginx
# Nginx SSL配置
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
ssl_prefer_server_ciphers off;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;

# 安全头
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

### 3. 应用安全

```yaml
# Spring Security配置
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-auth-server.com
          
# 敏感信息加密
encryption:
  key: ${ENCRYPTION_KEY}
  algorithm: AES/GCB/PKCS5Padding
```

### 4. 数据库安全

```sql
-- 创建专用用户
CREATE USER 'gitlab_metrics'@'%' IDENTIFIED BY 'strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON gitlab_metrics.* TO 'gitlab_metrics'@'%';

-- 启用SSL
ALTER USER 'gitlab_metrics'@'%' REQUIRE SSL;
```

## 升级和维护

### 1. 应用升级

```bash
# 备份当前版本
docker-compose down
cp -r /opt/gitlab-metrics /opt/gitlab-metrics-backup-$(date +%Y%m%d)

# 下载新版本
wget https://github.com/your-org/gitlab-metrics-backend/releases/download/v1.1.0/gitlab-metrics-backend-1.1.0.tar.gz
tar -xzf gitlab-metrics-backend-1.1.0.tar.gz

# 更新应用
docker-compose pull
docker-compose up -d

# 验证升级
curl http://localhost:8080/actuator/health
```

### 2. 数据库迁移

```bash
# 运行数据库迁移
docker-compose exec app java -jar app.jar --spring.flyway.migrate

# 验证迁移
docker-compose exec mysql mysql -u root -p -e "SELECT version FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5;"
```

### 3. 定期维护

```bash
# 创建维护脚本
cat > maintenance.sh << 'EOF'
#!/bin/bash

echo "Starting maintenance tasks..."

# 清理Docker资源
docker system prune -f

# 清理旧日志
find logs/ -name "*.log" -mtime +30 -delete

# 优化数据库
docker-compose exec mysql mysql -u root -p$MYSQL_ROOT_PASSWORD -e "OPTIMIZE TABLE gitlab_metrics.commits;"

# 重启服务（如果需要）
# docker-compose restart app

echo "Maintenance tasks completed."
EOF

chmod +x maintenance.sh

# 设置定期维护
crontab -e
# 添加以下行（每周日凌晨3点执行维护）
0 3 * * 0 /opt/gitlab-metrics/maintenance.sh
```

## 联系支持

如遇到问题，请按以下步骤获取支持：

1. 查看本文档的故障排除部分
2. 检查GitHub Issues: https://github.com/your-org/gitlab-metrics-backend/issues
3. 联系技术支持: support@your-company.com
4. 提供以下信息：
   - 系统版本和配置
   - 错误日志
   - 重现步骤
   - 环境信息

---

**文档版本**: v1.0.0  
**最后更新**: 2024-01-01  
**维护者**: 开发团队