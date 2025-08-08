# SSL/TLS 配置指南

本文档描述了如何为GitLab研发度量系统配置SSL/TLS加密传输。

## 概述

系统实现了以下安全特性：
- AES-256-GCM敏感数据加密存储
- HTTPS/TLS传输加密
- 数据库连接SSL配置
- 安全头配置

## 1. 敏感数据加密

### 1.1 AES加密配置

系统使用AES-256-GCM算法加密敏感数据，包括：
- 用户邮箱地址
- 用户最后登录IP
- 其他标记为敏感的字段

### 1.2 加密密钥配置

**开发环境：**
```yaml
app:
  security:
    encryption:
      key: dGVzdC1lbmNyeXB0aW9uLWtleS0yNTYtYml0cy1sb25nLWVub3VnaA==
```

**生产环境：**
```bash
export ENCRYPTION_KEY="your-base64-encoded-256-bit-key"
```

### 1.3 生成新的加密密钥

```java
// 使用EncryptionUtil生成新密钥
String newKey = EncryptionUtil.generateKey();
System.out.println("New encryption key: " + newKey);
```

## 2. HTTPS/TLS配置

### 2.1 SSL证书准备

**开发环境 - 生成自签名证书：**
```bash
keytool -genkeypair \
  -alias gitlab-metrics \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/keystore.p12 \
  -validity 365 \
  -dname "CN=localhost,OU=Development,O=GitLab Metrics,C=CN"
```

**生产环境 - 使用CA签发的证书：**
1. 从可信CA获取SSL证书
2. 将证书转换为PKCS12格式
3. 配置到应用中

### 2.2 应用配置

**生产环境配置 (application-prod.yml)：**
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD:changeit}
    key-store-type: PKCS12
    key-alias: gitlab-metrics
    protocol: TLS
    enabled-protocols: TLSv1.2,TLSv1.3
    ciphers: TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256,TLS_AES_128_GCM_SHA256
  port: 8443
  http:
    port: 8080  # HTTP端口，用于重定向到HTTPS
```

### 2.3 安全头配置

系统自动配置以下安全头：
- HSTS (HTTP Strict Transport Security)
- Content Security Policy
- X-Frame-Options
- X-Content-Type-Options
- Referrer Policy
- XSS Protection

## 3. 数据库SSL配置

### 3.1 MySQL SSL配置

**生产环境数据库连接：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gitlab_metrics?useSSL=true&requireSSL=true&verifyServerCertificate=true
    hikari:
      data-source-properties:
        useSSL: true
        requireSSL: true
        verifyServerCertificate: true
        trustCertificateKeyStoreUrl: ${DB_TRUST_STORE_URL:}
        trustCertificateKeyStorePassword: ${DB_TRUST_STORE_PASSWORD:}
        clientCertificateKeyStoreUrl: ${DB_CLIENT_CERT_URL:}
        clientCertificateKeyStorePassword: ${DB_CLIENT_CERT_PASSWORD:}
```

### 3.2 MySQL服务器SSL配置

在MySQL服务器上启用SSL：
```sql
-- 检查SSL状态
SHOW VARIABLES LIKE '%ssl%';

-- 启用SSL (在my.cnf中配置)
[mysqld]
ssl-ca=/path/to/ca-cert.pem
ssl-cert=/path/to/server-cert.pem
ssl-key=/path/to/server-key.pem
require_secure_transport=ON
```

## 4. 部署配置

### 4.1 Docker配置

**Dockerfile SSL支持：**
```dockerfile
FROM openjdk:8-jre-alpine

# 安装CA证书
RUN apk add --no-cache ca-certificates

# 复制SSL证书
COPY keystore.p12 /app/keystore.p12

VOLUME /tmp
COPY target/gitlab-metrics-backend-1.0.0.jar app.jar

EXPOSE 8080 8443

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
```

### 4.2 环境变量配置

**生产环境必需的环境变量：**
```bash
# SSL配置
export SSL_KEYSTORE_PASSWORD="your-keystore-password"

# 数据加密
export ENCRYPTION_KEY="your-base64-encoded-encryption-key"

# 数据库SSL
export DB_TRUST_STORE_URL="file:/path/to/truststore.jks"
export DB_TRUST_STORE_PASSWORD="truststore-password"
export DB_CLIENT_CERT_URL="file:/path/to/client-cert.p12"
export DB_CLIENT_CERT_PASSWORD="client-cert-password"

# JWT密钥
export JWT_SECRET="your-jwt-secret-key"
```

## 5. 安全最佳实践

### 5.1 证书管理
- 定期更新SSL证书（建议90天内）
- 使用强密码保护密钥库
- 定期轮换加密密钥
- 监控证书过期时间

### 5.2 配置验证
```bash
# 验证HTTPS配置
curl -I https://your-domain:8443/actuator/health

# 验证SSL证书
openssl s_client -connect your-domain:8443 -servername your-domain

# 验证数据库SSL连接
mysql -h your-db-host -u username -p --ssl-mode=REQUIRED
```

### 5.3 监控和告警
- 监控SSL证书过期时间
- 监控数据库SSL连接状态
- 记录安全相关的访问日志
- 设置安全事件告警

## 6. 故障排除

### 6.1 常见问题

**SSL握手失败：**
- 检查证书有效性
- 验证密钥库密码
- 确认TLS版本兼容性

**数据库SSL连接失败：**
- 检查MySQL SSL配置
- 验证证书路径和权限
- 确认SSL模式设置

**加密解密失败：**
- 检查加密密钥配置
- 验证密钥格式和长度
- 确认环境变量设置

### 6.2 调试命令

```bash
# 检查SSL证书信息
keytool -list -v -keystore keystore.p12 -storetype PKCS12

# 测试数据库SSL连接
mysql -h localhost -u root -p --ssl-mode=REQUIRED --ssl-verify-server-cert

# 验证加密功能
java -cp target/classes com.gitlab.metrics.util.EncryptionUtil
```

## 7. 合规性

系统SSL/TLS配置符合以下安全标准：
- TLS 1.2+ 协议支持
- 强加密算法套件
- 完美前向保密 (PFS)
- HSTS安全头
- 数据传输加密
- 敏感数据存储加密

定期进行安全审计和渗透测试以确保配置的有效性。