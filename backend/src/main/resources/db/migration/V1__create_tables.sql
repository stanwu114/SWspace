-- AI员工协作系统 - Flyway迁移脚本 V1
-- 创建数据库表结构

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 项目表
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50),
    customer_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'LEAD',
    stage VARCHAR(50),
    estimated_value DECIMAL(15, 2),
    contract_value DECIMAL(15, 2),
    bid_deadline TIMESTAMP,
    contract_date DATE,
    description TEXT,
    requirements JSONB,
    tech_stack JSONB DEFAULT '[]'::jsonb,
    risk_assessment JSONB,
    tags JSONB DEFAULT '[]'::jsonb,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 客户表
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    short_name VARCHAR(100),
    type VARCHAR(20) NOT NULL,
    industry VARCHAR(100),
    region VARCHAR(100),
    address VARCHAR(500),
    level VARCHAR(20) DEFAULT 'NORMAL',
    org_structure JSONB,
    relationship_score INTEGER DEFAULT 50,
    tags JSONB DEFAULT '[]'::jsonb,
    notes TEXT,
    website VARCHAR(500),
    last_contact_at TIMESTAMP,
    next_follow_up_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 联系人表
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    title VARCHAR(100),
    department VARCHAR(100),
    role VARCHAR(20),
    importance VARCHAR(20) DEFAULT 'NORMAL',
    phone VARCHAR(50),
    mobile VARCHAR(50),
    email VARCHAR(200),
    wechat VARCHAR(100),
    birthday DATE,
    preferences JSONB,
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 交互记录表
CREATE TABLE interactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    project_id UUID,
    contact_ids JSONB DEFAULT '[]'::jsonb,
    type VARCHAR(20) NOT NULL,
    subject VARCHAR(500),
    content TEXT,
    summary TEXT,
    key_points JSONB,
    sentiment VARCHAR(20),
    next_actions JSONB,
    next_action_at TIMESTAMP,
    interaction_at TIMESTAMP NOT NULL,
    duration INTEGER,
    location VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 文档表
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    customer_id UUID,
    name VARCHAR(500) NOT NULL,
    original_name VARCHAR(500),
    type VARCHAR(20) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    file_ext VARCHAR(20),
    mime_type VARCHAR(100),
    content_text TEXT,
    ai_analysis JSONB,
    ai_summary TEXT,
    embedding vector(1536),
    version INTEGER DEFAULT 1,
    is_latest BOOLEAN DEFAULT true,
    parent_id UUID,
    tags JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 知识库表
CREATE TABLE knowledge (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50),
    tags JSONB DEFAULT '[]'::jsonb,
    keywords JSONB DEFAULT '[]'::jsonb,
    source_type VARCHAR(20),
    source_id UUID,
    source_name VARCHAR(500),
    author VARCHAR(100),
    effective_date DATE,
    expire_date DATE,
    embedding vector(1536),
    view_count INTEGER DEFAULT 0,
    use_count INTEGER DEFAULT 0,
    rating DECIMAL(3, 2),
    is_verified BOOLEAN DEFAULT false,
    is_featured BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI会话表
CREATE TABLE ai_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_type VARCHAR(20) NOT NULL,
    title VARCHAR(500),
    project_id UUID,
    customer_id UUID,
    document_id UUID,
    context JSONB,
    summary TEXT,
    message_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI消息表
CREATE TABLE ai_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES ai_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    tool_name VARCHAR(100),
    tool_input JSONB,
    tool_output JSONB,
    model VARCHAR(50),
    tokens_input INTEGER,
    tokens_output INTEGER,
    latency_ms INTEGER,
    feedback VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 提醒表
CREATE TABLE reminders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    project_id UUID,
    customer_id UUID,
    interaction_id UUID,
    remind_at TIMESTAMP NOT NULL,
    repeat_type VARCHAR(20) DEFAULT 'NONE',
    repeat_config JSONB,
    priority VARCHAR(10) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'PENDING',
    snoozed_until TIMESTAMP,
    notification_channels JSONB DEFAULT '["desktop"]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 添加外键约束
ALTER TABLE projects ADD CONSTRAINT fk_projects_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;
ALTER TABLE interactions ADD CONSTRAINT fk_interactions_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;
ALTER TABLE documents ADD CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;
ALTER TABLE documents ADD CONSTRAINT fk_documents_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;
ALTER TABLE documents ADD CONSTRAINT fk_documents_parent FOREIGN KEY (parent_id) REFERENCES documents(id);
ALTER TABLE ai_sessions ADD CONSTRAINT fk_sessions_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;
ALTER TABLE ai_sessions ADD CONSTRAINT fk_sessions_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;
ALTER TABLE ai_sessions ADD CONSTRAINT fk_sessions_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL;
ALTER TABLE reminders ADD CONSTRAINT fk_reminders_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;
ALTER TABLE reminders ADD CONSTRAINT fk_reminders_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE reminders ADD CONSTRAINT fk_reminders_interaction FOREIGN KEY (interaction_id) REFERENCES interactions(id) ON DELETE SET NULL;

-- 创建索引
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_customer_id ON projects(customer_id);
CREATE INDEX idx_customers_type ON customers(type);
CREATE INDEX idx_contacts_customer_id ON contacts(customer_id);
CREATE INDEX idx_interactions_customer_id ON interactions(customer_id);
CREATE INDEX idx_documents_project_id ON documents(project_id);
CREATE INDEX idx_knowledge_category ON knowledge(category);
CREATE INDEX idx_ai_sessions_agent_type ON ai_sessions(agent_type);
CREATE INDEX idx_ai_messages_session_id ON ai_messages(session_id);
CREATE INDEX idx_reminders_remind_at ON reminders(remind_at);

-- 向量索引
CREATE INDEX idx_documents_embedding ON documents USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_knowledge_embedding ON knowledge USING hnsw (embedding vector_cosine_ops);
