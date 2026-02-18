# AI员工协作系统

面向政企智慧城市行业的"超级个人 + AI员工团队"协作系统。

## 项目简介

本系统旨在让资深行业专家能够以一人之力，借助4个AI员工团队完成原本需要完整销售团队才能完成的工作。

### 核心 AI 员工

- **情报分析师** (Intel Agent) - 招标监控、政策解读、竞品分析
- **文档写手** (Doc Agent) - 标书编写、方案撰写、报告生成
- **客户助理** (CRM Agent) - 客户画像、组织图谱、节点提醒
- **知识管家** (Knowledge Agent) - 案例沉淀、知识检索、最佳实践

## 技术架构

| 层级 | 技术选型 |
|------|----------|
| 桌面框架 | Electron 28+ |
| 前端框架 | React 18 + TypeScript 5 |
| 状态管理 | Zustand |
| 后端框架 | Spring Boot 3.2 |
| 数据库 | PostgreSQL 15 + PGVector |

## 快速开始

### 环境要求

- Node.js 18+
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/your-repo/aispace.git
cd aispace
```

2. **运行配置脚本**
```bash
chmod +x scripts/dev-setup.sh
./scripts/dev-setup.sh
```

3. **启动服务**
```bash
# 方式1: 使用脚本启动所有服务
./scripts/start-services.sh all

# 方式2: 分别启动
docker-compose up -d postgres   # 启动数据库
cd backend && mvn spring-boot:run &  # 启动后端
cd frontend && npm run dev      # 启动前端
```

4. **访问系统**
- 前端界面: http://localhost:3000
- API 文档: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/actuator/health

## 项目结构

```
S&W-AISPACE_qoder/
├── frontend/           # Electron + React 前端
│   ├── electron/       # Electron 主进程
│   └── src/            # React 源码
├── backend/            # Spring Boot 后端
│   └── src/main/java/com/aispace/
├── database/           # 数据库脚本
├── scripts/            # 开发脚本
├── doc/                # 设计文档
└── docker-compose.yml  # 本地服务编排
```

## 开发命令

```bash
# 前端
cd frontend
npm install          # 安装依赖
npm run dev          # 启动开发服务器
npm run build        # 构建生产版本
npm run lint         # 代码检查
npm run typecheck    # 类型检查

# 后端
cd backend
mvn spring-boot:run  # 启动后端服务
mvn clean compile    # 编译项目
mvn test             # 运行测试
```

## 配置说明

### 环境变量

```bash
# AI API 配置
export OPENAI_API_KEY=sk-xxxx
export DEEPSEEK_API_KEY=xxx

# 数据库配置 (可选，默认使用 Docker)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=aispace_db
```

## 文档

- [系统架构说明书](doc/系统架构说明书.md)
- [需求分析文档](doc/AI员工协作系统需求分析.md)

## License

Private - All rights reserved
