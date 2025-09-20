-- 创建同义词表
CREATE TABLE synonyms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    term VARCHAR(100) NOT NULL COMMENT '原始词项',
    synonym VARCHAR(100) NOT NULL COMMENT '同义词',
    confidence FLOAT DEFAULT 1.0 COMMENT '同义词置信度，范围0.0-1.0',
    source ENUM('MANUAL', 'AUTO', 'ML', 'EXTERNAL') DEFAULT 'MANUAL' COMMENT '同义词来源类型',
    category VARCHAR(50) COMMENT '词项类别，用于银行业务分类',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    usage_count BIGINT DEFAULT 0 COMMENT '使用频次统计',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建者ID',
    updated_by BIGINT COMMENT '更新者ID',
    remarks VARCHAR(500) COMMENT '备注信息',

    INDEX idx_term (term),
    INDEX idx_synonym (synonym),
    INDEX idx_source_confidence (source, confidence),
    INDEX idx_category (category),
    INDEX idx_enabled (enabled),
    INDEX idx_usage_count (usage_count),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_term_synonym (term, synonym)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同义词表';

-- 创建同义词使用统计表
CREATE TABLE synonym_usage_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    synonym_id BIGINT NOT NULL,
    date_hour DATETIME NOT NULL COMMENT '按小时聚合的时间戳',
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    query_count INT DEFAULT 0 COMMENT '查询次数',
    expansion_rate DECIMAL(5,3) COMMENT '扩展率',
    avg_result_count INT COMMENT '平均结果数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (synonym_id) REFERENCES synonyms(id) ON DELETE CASCADE,
    UNIQUE KEY uk_synonym_hour (synonym_id, date_hour),
    INDEX idx_date_hour (date_hour),
    INDEX idx_hit_count (hit_count),
    INDEX idx_expansion_rate (expansion_rate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同义词使用统计表';

-- 创建查询扩展日志表
CREATE TABLE query_expansion_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_query VARCHAR(1000) NOT NULL COMMENT '原始查询',
    expanded_terms JSON COMMENT '扩展词项列表',
    query_type ENUM('PRODUCT_QUERY', 'SERVICE_QUERY', 'PROCEDURE_QUERY', 'GENERAL_QUERY') COMMENT '查询类型',
    expansion_count INT DEFAULT 0 COMMENT '扩展词项数量',
    processing_time_ms INT COMMENT '处理时间（毫秒）',
    user_id BIGINT COMMENT '用户ID',
    space_id BIGINT COMMENT '空间ID',
    channels JSON COMMENT '渠道列表',
    search_result_count INT COMMENT '搜索结果数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_original_query (original_query(100)),
    INDEX idx_query_type (query_type),
    INDEX idx_user_id (user_id),
    INDEX idx_space_id (space_id),
    INDEX idx_created_at (created_at),
    INDEX idx_processing_time (processing_time_ms),
    INDEX idx_result_count (search_result_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询扩展日志表';

-- 创建同义词审核表
CREATE TABLE synonym_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    synonym_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL COMMENT '审核者ID',
    review_action ENUM('APPROVE', 'REJECT', 'MODIFY', 'REQUEST_MORE_INFO') NOT NULL COMMENT '审核动作',
    original_confidence FLOAT COMMENT '原始置信度',
    new_confidence FLOAT COMMENT '新置信度',
    review_comments TEXT COMMENT '审核意见',
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (synonym_id) REFERENCES synonyms(id) ON DELETE CASCADE,
    INDEX idx_synonym_id (synonym_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_review_action (review_action),
    INDEX idx_review_date (review_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同义词审核表';

-- 创建同义词模板表（预定义的银行业务同义词模板）
CREATE TABLE synonym_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    category VARCHAR(50) NOT NULL COMMENT '业务分类',
    term_pattern VARCHAR(200) NOT NULL COMMENT '词项模式',
    synonym_pattern VARCHAR(200) NOT NULL COMMENT '同义词模式',
    default_confidence FLOAT DEFAULT 0.9 COMMENT '默认置信度',
    description TEXT COMMENT '模板描述',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_template_name (template_name),
    INDEX idx_category (category),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同义词模板表';

-- 插入银行业务相关的初始同义词数据
INSERT INTO synonyms (term, synonym, confidence, source, category, remarks) VALUES
-- 银行产品类同义词
('房贷', '住房贷款', 1.0, 'MANUAL', 'BANK_PRODUCT', '住房贷款产品同义词'),
('房贷', '按揭贷款', 0.95, 'MANUAL', 'BANK_PRODUCT', '住房贷款产品同义词'),
('房贷', '房屋贷款', 0.9, 'MANUAL', 'BANK_PRODUCT', '住房贷款产品同义词'),
('车贷', '汽车贷款', 1.0, 'MANUAL', 'BANK_PRODUCT', '汽车贷款产品同义词'),
('车贷', '车辆贷款', 0.95, 'MANUAL', 'BANK_PRODUCT', '汽车贷款产品同义词'),
('信用卡', '贷记卡', 0.95, 'MANUAL', 'BANK_PRODUCT', '信用卡产品同义词'),
('储蓄卡', '借记卡', 0.95, 'MANUAL', 'BANK_PRODUCT', '储蓄卡产品同义词'),
('理财', '理财产品', 0.9, 'MANUAL', 'BANK_PRODUCT', '理财产品同义词'),
('理财', '投资理财', 0.85, 'MANUAL', 'BANK_PRODUCT', '理财产品同义词'),

-- 银行服务类同义词
('转账', '汇款', 0.9, 'MANUAL', 'BANK_SERVICE', '转账服务同义词'),
('转账', '转钱', 0.85, 'MANUAL', 'BANK_SERVICE', '转账服务同义词'),
('开户', '开立账户', 0.95, 'MANUAL', 'BANK_SERVICE', '开户服务同义词'),
('销户', '注销账户', 0.95, 'MANUAL', 'BANK_SERVICE', '销户服务同义词'),
('挂失', '止付', 0.9, 'MANUAL', 'BANK_SERVICE', '挂失服务同义词'),

-- 渠道类同义词
('网银', '网上银行', 1.0, 'MANUAL', 'CHANNEL', '网上银行渠道同义词'),
('网银', '在线银行', 0.9, 'MANUAL', 'CHANNEL', '网上银行渠道同义词'),
('手机银行', '移动银行', 0.95, 'MANUAL', 'CHANNEL', '手机银行渠道同义词'),
('手机银行', 'APP银行', 0.85, 'MANUAL', 'CHANNEL', '手机银行渠道同义词'),
('ATM', '自动取款机', 1.0, 'MANUAL', 'CHANNEL', 'ATM渠道同义词'),
('ATM', '取款机', 0.9, 'MANUAL', 'CHANNEL', 'ATM渠道同义词'),

-- 业务流程类同义词
('申请', '办理', 0.85, 'MANUAL', 'PROCEDURE', '业务流程同义词'),
('查询', '查看', 0.9, 'MANUAL', 'PROCEDURE', '业务流程同义词'),
('激活', '开通', 0.9, 'MANUAL', 'PROCEDURE', '业务流程同义词'),
('密码', '口令', 0.8, 'MANUAL', 'SECURITY', '安全相关同义词'),
('身份证', 'ID卡', 0.7, 'MANUAL', 'IDENTITY', '身份认证同义词');

-- 插入同义词模板数据
INSERT INTO synonym_templates (template_name, category, term_pattern, synonym_pattern, default_confidence, description) VALUES
('银行产品通用模板', 'BANK_PRODUCT', '{产品名}', '{产品名}产品', 0.9, '银行产品通用同义词模板'),
('贷款产品模板', 'LOAN', '{类型}贷', '{类型}贷款', 0.95, '贷款产品同义词模板'),
('银行服务模板', 'BANK_SERVICE', '{服务名}', '{服务名}服务', 0.85, '银行服务同义词模板'),
('渠道访问模板', 'CHANNEL', '{渠道名}', '{渠道名}银行', 0.8, '银行渠道同义词模板'),
('业务流程模板', 'PROCEDURE', '如何{操作}', '{操作}流程', 0.9, '业务流程同义词模板');

-- 创建同义词配置表
CREATE TABLE synonym_config (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同义词配置表';

-- 插入同义词相关配置
INSERT INTO synonym_config (config_key, config_value, config_type, description) VALUES
('synonym.cache.maxSize', '10000', 'INTEGER', '同义词缓存最大大小'),
('synonym.cache.expireMinutes', '60', 'INTEGER', '同义词缓存过期时间（分钟）'),
('synonym.confidence.threshold', '0.7', 'DECIMAL', '同义词置信度阈值'),
('synonym.expansion.maxTerms', '5', 'INTEGER', '查询扩展最大词项数'),
('synonym.expansion.enablePhonetic', 'true', 'BOOLEAN', '是否启用拼音匹配'),
('synonym.review.autoApproveThreshold', '0.9', 'DECIMAL', '自动审核通过阈值'),
('synonym.usage.trackingEnabled', 'true', 'BOOLEAN', '是否启用使用统计'),
('synonym.template.autoApplyEnabled', 'true', 'BOOLEAN', '是否自动应用模板');