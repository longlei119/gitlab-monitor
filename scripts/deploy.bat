@echo off
REM GitLab Metrics Backend Deployment Script for Windows

setlocal enabledelayedexpansion

REM Configuration
set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=prod

set COMPOSE_FILE=docker-compose.yml
set ENV_FILE=.env

if "%ENVIRONMENT%"=="prod" (
    set COMPOSE_FILE=docker-compose.prod.yml
    set ENV_FILE=.env.prod
) else if "%ENVIRONMENT%"=="dev" (
    set ENV_FILE=.env.dev
)

echo Starting GitLab Metrics Backend deployment for %ENVIRONMENT% environment...

REM Check if required files exist
if not exist "%COMPOSE_FILE%" (
    echo Error: %COMPOSE_FILE% not found!
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo Error: %ENV_FILE% not found!
    exit /b 1
)

REM Create necessary directories
echo Creating necessary directories...
if not exist "logs" mkdir logs
if not exist "ssl" mkdir ssl
if not exist "docker\nginx\ssl" mkdir docker\nginx\ssl

REM Generate SSL certificates if they don't exist
if not exist "docker\nginx\ssl\server.crt" (
    echo Generating SSL certificates...
    if exist "scripts\generate-ssl-cert.bat" (
        call scripts\generate-ssl-cert.bat
    ) else (
        echo SSL certificate generation script not found. Please generate certificates manually.
    )
)

REM Build the application
echo Building the application...
if exist "pom.xml" (
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo Error: Maven build failed!
        exit /b 1
    )
) else (
    echo Error: pom.xml not found! Please run this script from the project root.
    exit /b 1
)

REM Stop existing containers
echo Stopping existing containers...
docker-compose -f "%COMPOSE_FILE%" --env-file "%ENV_FILE%" down

REM Pull latest images
echo Pulling latest images...
docker-compose -f "%COMPOSE_FILE%" --env-file "%ENV_FILE%" pull

REM Start services
echo Starting services...
if "%ENVIRONMENT%"=="prod" (
    REM Start with monitoring profile for production
    docker-compose -f "%COMPOSE_FILE%" --env-file "%ENV_FILE%" --profile monitoring up -d
) else (
    docker-compose -f "%COMPOSE_FILE%" --env-file "%ENV_FILE%" up -d
)

REM Wait for services to be healthy
echo Waiting for services to be healthy...
timeout /t 30 /nobreak >nul

REM Check service health
echo Checking service health...
set services=mysql redis rabbitmq app

for %%s in (%services%) do (
    echo Checking %%s...
    docker-compose -f "%COMPOSE_FILE%" --env-file "%ENV_FILE%" ps %%s | findstr /C:"healthy" /C:"Up" >nul
    if errorlevel 1 (
        echo Service %%s is not healthy. Check logs with: docker-compose -f %COMPOSE_FILE% logs %%s
    ) else (
        echo %%s: OK
    )
)

REM Display service URLs
echo.
echo Deployment completed!
echo Service URLs:
echo   Application: http://localhost:8080
echo   Health Check: http://localhost:8080/actuator/health
echo   RabbitMQ Management: http://localhost:15672 (admin/admin123)

if "%ENVIRONMENT%"=="prod" (
    echo   Prometheus: http://localhost:9090
    echo   Grafana: http://localhost:3000 (admin/admin)
    echo   HTTPS: https://localhost
)

echo.
echo To view logs: docker-compose -f %COMPOSE_FILE% logs -f
echo To stop services: docker-compose -f %COMPOSE_FILE% down

endlocal