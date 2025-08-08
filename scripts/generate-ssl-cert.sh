#!/bin/bash

# SSL证书生成脚本
# 用于开发环境生成自签名SSL证书

set -e

# 配置变量
KEYSTORE_PATH="src/main/resources/keystore.p12"
KEYSTORE_PASSWORD="changeit"
CERT_ALIAS="gitlab-metrics"
VALIDITY_DAYS=365
COMMON_NAME="localhost"
ORG_UNIT="Development"
ORGANIZATION="GitLab Metrics"
COUNTRY="CN"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== GitLab Metrics SSL证书生成脚本 ===${NC}"
echo

# 检查keytool是否可用
if ! command -v keytool &> /dev/null; then
    echo -e "${RED}错误: keytool命令未找到。请确保已安装Java JDK。${NC}"
    exit 1
fi

# 检查是否已存在证书
if [ -f "$KEYSTORE_PATH" ]; then
    echo -e "${YELLOW}警告: 证书文件已存在: $KEYSTORE_PATH${NC}"
    read -p "是否覆盖现有证书? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "操作已取消。"
        exit 0
    fi
    rm -f "$KEYSTORE_PATH"
fi

# 创建目录
mkdir -p "$(dirname "$KEYSTORE_PATH")"

echo -e "${GREEN}正在生成SSL证书...${NC}"
echo "证书信息:"
echo "  - 通用名称 (CN): $COMMON_NAME"
echo "  - 组织单位 (OU): $ORG_UNIT"
echo "  - 组织 (O): $ORGANIZATION"
echo "  - 国家 (C): $COUNTRY"
echo "  - 有效期: $VALIDITY_DAYS 天"
echo "  - 密钥库路径: $KEYSTORE_PATH"
echo

# 生成自签名证书
keytool -genkeypair \
    -alias "$CERT_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_PATH" \
    -validity $VALIDITY_DAYS \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEYSTORE_PASSWORD" \
    -dname "CN=$COMMON_NAME,OU=$ORG_UNIT,O=$ORGANIZATION,C=$COUNTRY" \
    -ext "SAN=DNS:localhost,DNS:127.0.0.1,IP:127.0.0.1"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ SSL证书生成成功!${NC}"
    echo
    echo "证书详情:"
    keytool -list -v -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD" -alias "$CERT_ALIAS" | head -20
    echo
    echo -e "${GREEN}配置信息:${NC}"
    echo "在 application-prod.yml 中添加以下配置:"
    echo
    echo "server:"
    echo "  ssl:"
    echo "    enabled: true"
    echo "    key-store: classpath:keystore.p12"
    echo "    key-store-password: $KEYSTORE_PASSWORD"
    echo "    key-store-type: PKCS12"
    echo "    key-alias: $CERT_ALIAS"
    echo "  port: 8443"
    echo
    echo -e "${YELLOW}注意: 这是自签名证书，仅适用于开发环境。${NC}"
    echo -e "${YELLOW}生产环境请使用由可信CA签发的证书。${NC}"
else
    echo -e "${RED}✗ SSL证书生成失败!${NC}"
    exit 1
fi

echo
echo -e "${GREEN}=== 证书生成完成 ===${NC}"