-- AI员工协作系统 - 数据库初始化脚本
-- PostgreSQL 15+ with PGVector 0.5+

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- ==============================================
-- 1. 项目表 (projects)
-- ==============================================
CREATE TABLE IF NOT EXISTS projects (
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

COMMENT ON TABLE projects IS '项目表';
COMMENT ON COLUMN projects.status IS '项目状态: LEAD(线索), OPPORTUNITY(机会), EXECUTION(执行), COMPLETED(完成), CANCELLED(取消)';

-- ==============================================
-- 2. 客户表 (customers)
-- ==============================================
CREATE TABLE IF NOT EXISTS customers (
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

COMMENT ON TABLE customers IS '客户表';
COMMENT ON COLUMN customers.type IS '客户类型: GOVERNMENT(政府), ENTERPRISE(企业), OTHER(其他)';
COMMENT ON COLUMN customers.level IS '客户级别: KEY(重点), NORMAL(普通), POTENTIAL(潜在)';

-- ==============================================
-- 3. 联系人表 (contacts)
-- ==============================================
CREATE TABLE IF NOT EXISTS contacts (
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

COMMENT ON TABLE contacts IS '联系人表';
COMMENT ON COLUMN contacts.role IS '角色: DECISION_MAKER(决策者), INFLUENCER(影响者), USER(使用者), CHAMPION(支持者)';

-- ==============================================
-- 4. 交互记录表 (interactions)
-- ==============================================
CREATE TABLE IF NOT EXISTS interactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
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

COMMENT ON TABLE interactions IS '交互记录表';
COMMENT ON COLUMN interactions.type IS '交互类型: CALL(电话), MEETING(会议), EMAIL(邮件), WECHAT(微信), VISIT(拜访), OTHER(其他)';

-- ==============================================
-- 5. 文档表 (documents)
-- ==============================================
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
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
    parent_id UUID REFERENCES documents(id),
    tags JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE documents IS '文档表';
COMMENT ON COLUMN documents.type IS '文档类型: BID(招标文件), SOLUTION(方案), REPORT(报告), CONTRACT(合同), POLICY(政策), OTHER(其他)';
COMMENT ON COLUMN documents.embedding IS '文档向量嵌入 (1536维)';

-- ==============================================
-- 6. 知识库表 (knowledge)
-- ==============================================
CREATE TABLE IF NOT EXISTS knowledge (
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

COMMENT ON TABLE knowledge IS '知识库表';
COMMENT ON COLUMN knowledge.category IS '知识分类: INDUSTRY(行业), SOLUTION(方案), CASE(案例), SALES(销售), LESSON(教训), TEMPLATE(模板), OTHER(其他)';

-- ==============================================
-- 7. AI会话表 (ai_sessions)
-- ==============================================
CREATE TABLE IF NOT EXISTS ai_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_type VARCHAR(20) NOT NULL,
    title VARCHAR(500),
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    context JSONB,
    summary TEXT,
    message_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_sessions IS 'AI会话表';
COMMENT ON COLUMN ai_sessions.agent_type IS 'Agent类型: INTEL(情报分析师), DOC(文档写手), CRM(客户助理), KNOWLEDGE(知识管家)';

-- ==============================================
-- 8. AI消息表 (ai_messages)
-- ==============================================
CREATE TABLE IF NOT EXISTS ai_messages (
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

COMMENT ON TABLE ai_messages IS 'AI消息表';
COMMENT ON COLUMN ai_messages.role IS '消息角色: USER(用户), ASSISTANT(助手), SYSTEM(系统), TOOL(工具)';

-- ==============================================
-- 9. 提醒表 (reminders)
-- ==============================================
CREATE TABLE IF NOT EXISTS reminders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    interaction_id UUID REFERENCES interactions(id) ON DELETE SET NULL,
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

COMMENT ON TABLE reminders IS '提醒表';
COMMENT ON COLUMN reminders.type IS '提醒类型: FOLLOW_UP(跟进), DEADLINE(截止), PAYMENT(付款), MEETING(会议), CUSTOM(自定义)';

-- ==============================================
-- 添加外键约束
-- ==============================================
ALTER TABLE projects 
    ADD CONSTRAINT fk_projects_customer 
    FOREIGN KEY (customer_id) 
    REFERENCES customers(id) 
    ON DELETE SET NULL;
