-- 创建文档向量表
CREATE TABLE document_vectors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    vector_data JSON NOT NULL,  -- 存储向量数组
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50),
    vector_dimension INT NOT NULL,
    processing_mode ENUM('OFFLINE', 'ONLINE') NOT NULL,
    processing_time_ms INT,
    quality_score DECIMAL(5,3),  -- 向量质量评分 0-1
    chunk_index INT DEFAULT 0,   -- 文档分块索引
    chunk_text TEXT,             -- 对应的文本块
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    INDEX idx_document_model (document_id, model_name),
    INDEX idx_model_dimension (model_name, vector_dimension),
    INDEX idx_processing_mode (processing_mode),
    INDEX idx_quality_score (quality_score),
    INDEX idx_created_at (created_at)
);

-- 创建向量处理任务队列表
CREATE TABLE vector_processing_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    task_type ENUM('INITIAL', 'REPROCESS', 'QUALITY_CHECK', 'UPDATE') NOT NULL,
    priority INT DEFAULT 5,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
    processing_mode ENUM('OFFLINE', 'ONLINE', 'AUTO') DEFAULT 'AUTO',
    model_name VARCHAR(100),
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message TEXT,
    error_code VARCHAR(50),
    scheduled_at TIMESTAMP NULL,  -- 计划执行时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    INDEX idx_status_priority (status, priority, created_at),
    INDEX idx_document_task (document_id, task_type),
    INDEX idx_processing_mode (processing_mode),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_retry_count (retry_count)
);

-- 创建向量处理指标表
CREATE TABLE vector_processing_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    date_hour DATETIME NOT NULL,  -- 按小时聚合的时间戳
    processing_mode ENUM('OFFLINE', 'ONLINE') NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    total_requests INT DEFAULT 0,
    successful_requests INT DEFAULT 0,
    failed_requests INT DEFAULT 0,
    avg_processing_time_ms INT,
    min_processing_time_ms INT,
    max_processing_time_ms INT,
    total_cost_cents INT DEFAULT 0,  -- 以分为单位存储成本
    avg_quality_score DECIMAL(5,3), -- 平均质量评分
    total_tokens_used INT DEFAULT 0, -- 使用的token总数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_metrics (date_hour, processing_mode, model_name),
    INDEX idx_date_mode (date_hour, processing_mode),
    INDEX idx_model_performance (model_name, avg_processing_time_ms),
    INDEX idx_cost_analysis (total_cost_cents, date_hour)
);

-- 创建向量相似度搜索缓存表
CREATE TABLE vector_search_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    query_hash VARCHAR(64) NOT NULL,  -- 查询文本的hash值
    query_text VARCHAR(1000) NOT NULL,
    query_vector JSON NOT NULL,       -- 查询向量
    model_name VARCHAR(100) NOT NULL,
    search_results JSON,              -- 缓存的搜索结果
    hit_count INT DEFAULT 0,          -- 命中次数
    expires_at TIMESTAMP NOT NULL,    -- 缓存过期时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_query_model (query_hash, model_name),
    INDEX idx_expires_at (expires_at),
    INDEX idx_hit_count (hit_count),
    INDEX idx_model_cache (model_name, created_at)
);

-- 创建向量处理配置表
CREATE TABLE vector_processing_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    config_type ENUM('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON') NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key),
    INDEX idx_is_active (is_active)
);

-- 插入默认配置
INSERT INTO vector_processing_config (config_key, config_value, config_type, description) VALUES
('default_model', 'text-embedding-3-small', 'STRING', '默认向量化模型'),
('default_dimension', '1536', 'INTEGER', '默认向量维度'),
('batch_size', '50', 'INTEGER', '批处理大小'),
('max_concurrent_tasks', '10', 'INTEGER', '最大并发任务数'),
('cache_ttl_hours', '24', 'INTEGER', '缓存TTL（小时）'),
('quality_threshold', '0.8', 'DECIMAL', '向量质量阈值'),
('cost_threshold_cents', '1000', 'INTEGER', '每小时成本阈值（分）'),
('auto_switch_enabled', 'true', 'BOOLEAN', '是否启用自动模式切换'),
('retry_delay_seconds', '60', 'INTEGER', '重试延迟（秒）'),
('max_chunk_size', '512', 'INTEGER', '最大文本块大小');