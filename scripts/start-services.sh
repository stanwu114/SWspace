#!/bin/bash
# AI员工协作系统 - 服务启动脚本

set -e

echo "=========================================="
echo "  AI员工协作系统 - 启动服务"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 获取脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 启动数据库
start_db() {
    echo -e "${YELLOW}启动数据库...${NC}"
    cd "$PROJECT_DIR"
    docker-compose up -d postgres
    echo -e "${GREEN}✓${NC} 数据库已启动"
}

# 启动后端
start_backend() {
    echo -e "${YELLOW}启动后端服务...${NC}"
    cd "$PROJECT_DIR/backend"
    mvn spring-boot:run &
    echo -e "${GREEN}✓${NC} 后端服务启动中..."
}

# 启动前端
start_frontend() {
    echo -e "${YELLOW}启动前端服务...${NC}"
    cd "$PROJECT_DIR/frontend"
    npm run dev &
    echo -e "${GREEN}✓${NC} 前端服务启动中..."
}

# 停止所有服务
stop_all() {
    echo -e "${YELLOW}停止所有服务...${NC}"
    cd "$PROJECT_DIR"
    docker-compose down
    pkill -f "spring-boot:run" || true
    pkill -f "vite" || true
    echo -e "${GREEN}✓${NC} 所有服务已停止"
}

# 使用说明
usage() {
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  db        只启动数据库"
    echo "  backend   只启动后端"
    echo "  frontend  只启动前端"
    echo "  all       启动所有服务"
    echo "  stop      停止所有服务"
    echo ""
}

case "$1" in
    db)
        start_db
        ;;
    backend)
        start_backend
        ;;
    frontend)
        start_frontend
        ;;
    all)
        start_db
        sleep 3
        start_backend
        sleep 5
        start_frontend
        echo ""
        echo "所有服务已启动!"
        echo "  前端: http://localhost:3000"
        echo "  后端: http://localhost:8080"
        ;;
    stop)
        stop_all
        ;;
    *)
        usage
        ;;
esac
