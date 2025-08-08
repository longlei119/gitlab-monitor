#!/bin/bash

# GitLab Metrics Backend Deployment Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-prod}
COMPOSE_FILE="docker-compose.yml"
ENV_FILE=".env"

if [ "$ENVIRONMENT" = "prod" ]; then
    COMPOSE_FILE="docker-compose.prod.yml"
    ENV_FILE=".env.prod"
elif [ "$ENVIRONMENT" = "dev" ]; then
    ENV_FILE=".env.dev"
fi

echo -e "${GREEN}Starting GitLab Metrics Backend deployment for $ENVIRONMENT environment...${NC}"

# Check if required files exist
if [ ! -f "$COMPOSE_FILE" ]; then
    echo -e "${RED}Error: $COMPOSE_FILE not found!${NC}"
    exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: $ENV_FILE not found!${NC}"
    exit 1
fi

# Create necessary directories
echo -e "${YELLOW}Creating necessary directories...${NC}"
mkdir -p logs
mkdir -p ssl
mkdir -p docker/nginx/ssl

# Generate SSL certificates if they don't exist
if [ ! -f "docker/nginx/ssl/server.crt" ] || [ ! -f "docker/nginx/ssl/server.key" ]; then
    echo -e "${YELLOW}Generating SSL certificates...${NC}"
    if [ -f "scripts/generate-ssl-cert.sh" ]; then
        chmod +x scripts/generate-ssl-cert.sh
        ./scripts/generate-ssl-cert.sh
    else
        echo -e "${YELLOW}SSL certificate generation script not found. Please generate certificates manually.${NC}"
    fi
fi

# Build the application
echo -e "${YELLOW}Building the application...${NC}"
if [ -f "pom.xml" ]; then
    mvn clean package -DskipTests
else
    echo -e "${RED}Error: pom.xml not found! Please run this script from the project root.${NC}"
    exit 1
fi

# Stop existing containers
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down

# Pull latest images
echo -e "${YELLOW}Pulling latest images...${NC}"
docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull

# Start services
echo -e "${YELLOW}Starting services...${NC}"
if [ "$ENVIRONMENT" = "prod" ]; then
    # Start with monitoring profile for production
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --profile monitoring up -d
else
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
fi

# Wait for services to be healthy
echo -e "${YELLOW}Waiting for services to be healthy...${NC}"
sleep 30

# Check service health
echo -e "${YELLOW}Checking service health...${NC}"
services=("mysql" "redis" "rabbitmq" "app")

for service in "${services[@]}"; do
    echo -n "Checking $service... "
    if docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps "$service" | grep -q "healthy\|Up"; then
        echo -e "${GREEN}OK${NC}"
    else
        echo -e "${RED}FAILED${NC}"
        echo -e "${RED}Service $service is not healthy. Check logs with: docker-compose -f $COMPOSE_FILE logs $service${NC}"
    fi
done

# Display service URLs
echo -e "${GREEN}Deployment completed!${NC}"
echo -e "${GREEN}Service URLs:${NC}"
echo -e "  Application: http://localhost:8080"
echo -e "  Health Check: http://localhost:8080/actuator/health"
echo -e "  RabbitMQ Management: http://localhost:15672 (admin/admin123)"

if [ "$ENVIRONMENT" = "prod" ]; then
    echo -e "  Prometheus: http://localhost:9090"
    echo -e "  Grafana: http://localhost:3000 (admin/admin)"
    echo -e "  HTTPS: https://localhost"
fi

echo -e "${YELLOW}To view logs: docker-compose -f $COMPOSE_FILE logs -f${NC}"
echo -e "${YELLOW}To stop services: docker-compose -f $COMPOSE_FILE down${NC}"