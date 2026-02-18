#!/bin/bash
# AI员工协作系统 - 开发环境配置脚本

set -e

echo "=========================================="
echo "  AI员工协作系统 - 开发环境配置"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查必要工具
check_requirements() {
    echo -e "\n${YELLOW}检查必要工具...${NC}"
    
    # 检查 Docker
    if command -v docker &> /dev/null; then
        echo -e "${GREEN}✓${NC} Docker: $(docker --version)"
    else
        echo -e "${RED}✗${NC} Docker 未安装"
        exit 1
    fi
    
    # 检查 Node.js
    if command -v node &> /dev/null; then
        echo -e "${GREEN}✓${NC} Node.js: $(node --version)"
    else
        echo -e "${RED}✗${NC} Node.js 未安装 (需要 18+)"
        exit 1
    fi
    
    # 检查 Java
    if command -v java &> /dev/null; then
        echo -e "${GREEN}✓${NC} Java: $(java --version 2>&1 | head -n 1)"
    else
        echo -e "${RED}✗${NC} Java 未安装 (需要 17+)"
        exit 1
    fi
    
    # 检查 Maven
    if command -v mvn &> /dev/null; then
        echo -e "${GREEN}✓${NC} Maven: $(mvn --version | head -n 1)"
    else
        echo -e "${RED}✗${NC} Maven 未安装"
        exit 1
    fi
}

# 启动数据库
start_database() {
    echo -e "\n${YELLOW}启动 PostgreSQL 数据库...${NC}"
    docker-compose up -d postgres
    
    echo "等待数据库就绪..."
    sleep 5
    
    # 检查数据库状态
    if docker-compose exec -T postgres pg_isready -U aispace_user -d aispace_db &> /dev/null; then
        echo -e "${GREEN}✓${NC} 数据库启动成功"
    else
        echo -e "${RED}✗${NC} 数据库启动失败"
        exit 1
    fi
}

# 安装前端依赖
setup_frontend() {
    echo -e "\n${YELLOW}安装前端依赖...${NC}"
    cd frontend
    npm install
    cd ..
    echo -e "${GREEN}✓${NC} 前端依赖安装完成"
}

# 编译后端
setup_backend() {
    echo -e "\n${YELLOW}编译后端项目...${NC}"
    cd backend
    mvn clean compile -DskipTests
    cd ..
    echo -e "${GREEN}✓${NC} 后端编译完成"
}

# 创建数据目录
create_data_dirs() {
    echo -e "\n${YELLOW}创建数据目录...${NC}"
    mkdir -p ~/.aispace/{documents,templates,exports,backups,logs}
    echo -e "${GREEN}✓${NC} 数据目录创建完成"
}

# 主流程
main() {
    check_requirements
    start_database
    setup_frontend
    setup_backend
    create_data_dirs
    
    echo -e "\n=========================================="
    echo -e "${GREEN}开发环境配置完成!${NC}"
    echo "=========================================="
    echo ""
    echo "启动服务:"
    echo "  后端: cd backend && mvn spring-boot:run"
    echo "  前端: cd frontend && npm run dev"
    echo ""
    echo "访问地址:"
    echo "  前端: http://localhost:3000"
    echo "  后端 API: http://localhost:8080"
    echo "  API 文档: http://localhost:8080/swagger-ui.html"
    echo ""
}

main "$@"
