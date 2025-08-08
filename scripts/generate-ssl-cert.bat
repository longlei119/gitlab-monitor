@echo off
REM SSL证书生成脚本 (Windows版本)
REM 用于开发环境生成自签名SSL证书

setlocal enabledelayedexpansion

REM 配置变量
set KEYSTORE_PATH=src\main\resources\keystore.p12
set KEYSTORE_PASSWORD=changeit
set CERT_ALIAS=gitlab-metrics
set VALIDITY_DAYS=365
set COMMON_NAME=localhost
set ORG_UNIT=Development
set ORGANIZATION=GitLab Metrics
set COUNTRY=CN

echo === GitLab Metrics SSL证书生成脚本 ===
echo.

REM 检查keytool是否可用
keytool -help >nul 2>&1
if errorlevel 1 (
    echo 错误: keytool命令未找到。请确保已安装Java JDK并配置PATH环境变量。
    pause
    exit /b 1
)

REM 检查是否已存在证书
if exist "%KEYSTORE_PATH%" (
    echo 警告: 证书文件已存在: %KEYSTORE_PATH%
    set /p REPLY="是否覆盖现有证书? (y/N): "
    if /i not "!REPLY!"=="y" (
        echo 操作已取消。
        pause
        exit /b 0
    )
    del "%KEYSTORE_PATH%"
)

REM 创建目录
if not exist "src\main\resources" mkdir "src\main\resources"

echo 正在生成SSL证书...
echo 证书信息:
echo   - 通用名称 (CN): %COMMON_NAME%
echo   - 组织单位 (OU): %ORG_UNIT%
echo   - 组织 (O): %ORGANIZATION%
echo   - 国家 (C): %COUNTRY%
echo   - 有效期: %VALIDITY_DAYS% 天
echo   - 密钥库路径: %KEYSTORE_PATH%
echo.

REM 生成自签名证书
keytool -genkeypair ^
    -alias "%CERT_ALIAS%" ^
    -keyalg RSA ^
    -keysize 2048 ^
    -storetype PKCS12 ^
    -keystore "%KEYSTORE_PATH%" ^
    -validity %VALIDITY_DAYS% ^
    -storepass "%KEYSTORE_PASSWORD%" ^
    -keypass "%KEYSTORE_PASSWORD%" ^
    -dname "CN=%COMMON_NAME%,OU=%ORG_UNIT%,O=%ORGANIZATION%,C=%COUNTRY%" ^
    -ext "SAN=DNS:localhost,DNS:127.0.0.1,IP:127.0.0.1"

if errorlevel 1 (
    echo ✗ SSL证书生成失败!
    pause
    exit /b 1
)

echo ✓ SSL证书生成成功!
echo.
echo 证书详情:
keytool -list -v -keystore "%KEYSTORE_PATH%" -storepass "%KEYSTORE_PASSWORD%" -alias "%CERT_ALIAS%"
echo.
echo 配置信息:
echo 在 application-prod.yml 中添加以下配置:
echo.
echo server:
echo   ssl:
echo     enabled: true
echo     key-store: classpath:keystore.p12
echo     key-store-password: %KEYSTORE_PASSWORD%
echo     key-store-type: PKCS12
echo     key-alias: %CERT_ALIAS%
echo   port: 8443
echo.
echo 注意: 这是自签名证书，仅适用于开发环境。
echo 生产环境请使用由可信CA签发的证书。
echo.
echo === 证书生成完成 ===
pause