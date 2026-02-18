-- AI员工协作系统 - 索引创建脚本

-- ==============================================
-- 项目表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_projects_customer_id ON projects(customer_id);
CREATE INDEX IF NOT EXISTS idx_projects_bid_deadline ON projects(bid_deadline);
CREATE INDEX IF NOT EXISTS idx_projects_created_at ON projects(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_projects_name_gin ON projects USING gin(to_tsvector('simple', name));

-- ==============================================
-- 客户表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_customers_type ON customers(type);
CREATE INDEX IF NOT EXISTS idx_customers_level ON customers(level);
CREATE INDEX IF NOT EXISTS idx_customers_region ON customers(region);
CREATE INDEX IF NOT EXISTS idx_customers_next_follow_up ON customers(next_follow_up_at);
CREATE INDEX IF NOT EXISTS idx_customers_name_gin ON customers USING gin(to_tsvector('simple', name));

-- ==============================================
-- 联系人表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_contacts_customer_id ON contacts(customer_id);
CREATE INDEX IF NOT EXISTS idx_contacts_role ON contacts(role);
CREATE INDEX IF NOT EXISTS idx_contacts_is_active ON contacts(is_active);

-- ==============================================
-- 交互记录表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_interactions_customer_id ON interactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_interactions_project_id ON interactions(project_id);
CREATE INDEX IF NOT EXISTS idx_interactions_type ON interactions(type);
CREATE INDEX IF NOT EXISTS idx_interactions_interaction_at ON interactions(interaction_at DESC);

-- ==============================================
-- 文档表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_documents_project_id ON documents(project_id);
CREATE INDEX IF NOT EXISTS idx_documents_customer_id ON documents(customer_id);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(type);
CREATE INDEX IF NOT EXISTS idx_documents_is_latest ON documents(is_latest);

-- 向量索引 (HNSW索引，用于语义搜索)
CREATE INDEX IF NOT EXISTS idx_documents_embedding ON documents 
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ==============================================
-- 知识库表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_knowledge_category ON knowledge(category);
CREATE INDEX IF NOT EXISTS idx_knowledge_is_featured ON knowledge(is_featured);
CREATE INDEX IF NOT EXISTS idx_knowledge_title_gin ON knowledge USING gin(to_tsvector('simple', title));

-- 向量索引 (HNSW索引，用于语义搜索)
CREATE INDEX IF NOT EXISTS idx_knowledge_embedding ON knowledge 
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ==============================================
-- AI会话表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_ai_sessions_agent_type ON ai_sessions(agent_type);
CREATE INDEX IF NOT EXISTS idx_ai_sessions_project_id ON ai_sessions(project_id);
CREATE INDEX IF NOT EXISTS idx_ai_sessions_status ON ai_sessions(status);
CREATE INDEX IF NOT EXISTS idx_ai_sessions_created_at ON ai_sessions(created_at DESC);

-- ==============================================
-- AI消息表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_ai_messages_session_id ON ai_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_messages_role ON ai_messages(role);
CREATE INDEX IF NOT EXISTS idx_ai_messages_created_at ON ai_messages(created_at);

-- ==============================================
-- 提醒表索引
-- ==============================================
CREATE INDEX IF NOT EXISTS idx_reminders_remind_at ON reminders(remind_at);
CREATE INDEX IF NOT EXISTS idx_reminders_status ON reminders(status);
CREATE INDEX IF NOT EXISTS idx_reminders_project_id ON reminders(project_id);
CREATE INDEX IF NOT EXISTS idx_reminders_customer_id ON reminders(customer_id);
