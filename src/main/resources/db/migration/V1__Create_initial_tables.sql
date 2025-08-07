-- GitLab研发度量系统数据库初始化脚本
-- 创建核心数据表结构

-- 代码提交表
CREATE TABLE commits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commit_sha VARCHAR(40) NOT NULL UNIQUE,
    project_id VARCHAR(100) NOT NULL,
    developer_id VARCHAR(100) NOT NULL,
    developer_name VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL,
    message TEXT,
    branch VARCHAR(255),
    lines_added INT,
    lines_deleted INT,
    files_changed INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_commit_project_developer (project_id, developer_id),
    INDEX idx_commit_timestamp (timestamp),
    INDEX idx_commit_sha (commit_sha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文件变更表
CREATE TABLE file_changes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commit_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    change_type VARCHAR(20),
    lines_added INT,
    lines_deleted INT,
    old_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_change_commit (commit_id),
    INDEX idx_file_change_path (file_path),
    FOREIGN KEY (commit_id) REFERENCES commits(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 代码质量指标表
CREATE TABLE quality_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    commit_sha VARCHAR(40) NOT NULL,
    timestamp DATETIME NOT NULL,
    code_complexity DECIMAL(10,2),
    duplicate_rate DECIMAL(5,2),
    maintainability_index DECIMAL(5,2),
    security_issues INT,
    performance_issues INT,
    code_smells INT,
    bugs INT,
    vulnerabilities INT,
    hotspots INT,
    technical_debt DECIMAL(5,2),
    quality_gate VARCHAR(20),
    analysis_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_quality_project (project_id),
    INDEX idx_quality_commit (commit_sha),
    INDEX idx_quality_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试覆盖率表
CREATE TABLE test_coverage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(100) NOT NULL,
    commit_sha VARCHAR(40) NOT NULL,
    timestamp DATETIME NOT NULL,
    line_coverage DECIMAL(5,2),
    branch_coverage DECIMAL(5,2),
    function_coverage DECIMAL(5,2),
    total_lines INT,
    covered_lines INT,
    total_branches INT,
    covered_branches INT,
    total_functions INT,
    covered_functions INT,
    total_classes INT,
    covered_classes INT,
    report_type VARCHAR(50),
    report_path TEXT,
    status VARCHAR(20),
    threshold DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coverage_project (project_id),
    INDEX idx_coverage_commit (commit_sha),
    INDEX idx_coverage_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 合并请求表
CREATE TABLE merge_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mr_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    author_id VARCHAR(100) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    merged_at DATETIME,
    closed_at DATETIME,
    updated_at DATETIME,
    status VARCHAR(20) NOT NULL,
    source_branch VARCHAR(255) NOT NULL,
    target_branch VARCHAR(255) NOT NULL,
    title TEXT,
    description TEXT,
    changed_files INT,
    additions INT,
    deletions INT,
    commits INT,
    assignee_id VARCHAR(100),
    assignee_name VARCHAR(255),
    merged_by_id VARCHAR(100),
    merged_by_name VARCHAR(255),
    work_in_progress BOOLEAN,
    squash BOOLEAN,
    web_url TEXT,
    db_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    db_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mr_project (project_id),
    INDEX idx_mr_author (author_id),
    INDEX idx_mr_status (status),
    INDEX idx_mr_created (created_at),
    UNIQUE KEY uk_mr_project (mr_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 代码评审表
CREATE TABLE code_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merge_request_id BIGINT NOT NULL,
    reviewer_id VARCHAR(100) NOT NULL,
    reviewer_name VARCHAR(255) NOT NULL,
    reviewed_at DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL,
    comment TEXT,
    comments_count INT,
    submitted_at DATETIME,
    review_type VARCHAR(50),
    is_required BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_review_mr (merge_request_id),
    INDEX idx_review_reviewer (reviewer_id),
    INDEX idx_review_timestamp (reviewed_at),
    FOREIGN KEY (merge_request_id) REFERENCES merge_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 问题（Issue）表
CREATE TABLE issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    author_id VARCHAR(100) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    assignee_id VARCHAR(100),
    assignee_name VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    closed_at DATETIME,
    due_date DATETIME,
    status VARCHAR(20) NOT NULL,
    issue_type VARCHAR(30),
    priority VARCHAR(20),
    severity VARCHAR(20),
    labels TEXT,
    weight INT,
    time_estimate INT,
    time_spent INT,
    milestone_id VARCHAR(100),
    milestone_title VARCHAR(255),
    web_url TEXT,
    upvotes INT,
    downvotes INT,
    confidential BOOLEAN,
    first_response_at DATETIME,
    resolution_at DATETIME,
    response_time_minutes BIGINT,
    resolution_time_minutes BIGINT,
    db_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    db_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_issue_project (project_id),
    INDEX idx_issue_assignee (assignee_id),
    INDEX idx_issue_status (status),
    INDEX idx_issue_created (created_at),
    INDEX idx_issue_type (issue_type),
    UNIQUE KEY uk_issue_project (issue_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建视图：开发者提交统计
CREATE VIEW developer_commit_stats AS
SELECT 
    developer_id,
    developer_name,
    project_id,
    DATE(timestamp) as commit_date,
    COUNT(*) as commit_count,
    SUM(lines_added) as total_lines_added,
    SUM(lines_deleted) as total_lines_deleted,
    SUM(files_changed) as total_files_changed
FROM commits 
GROUP BY developer_id, developer_name, project_id, DATE(timestamp);

-- 创建视图：项目质量趋势
CREATE VIEW project_quality_trends AS
SELECT 
    project_id,
    DATE(timestamp) as analysis_date,
    AVG(code_complexity) as avg_complexity,
    AVG(duplicate_rate) as avg_duplicate_rate,
    AVG(maintainability_index) as avg_maintainability,
    SUM(security_issues) as total_security_issues,
    SUM(bugs) as total_bugs,
    COUNT(*) as analysis_count
FROM quality_metrics 
GROUP BY project_id, DATE(timestamp);

-- 创建视图：Bug修复效率统计
CREATE VIEW bug_fix_efficiency AS
SELECT 
    project_id,
    assignee_id,
    assignee_name,
    issue_type,
    priority,
    severity,
    COUNT(*) as total_issues,
    COUNT(CASE WHEN status = 'closed' THEN 1 END) as closed_issues,
    AVG(response_time_minutes) as avg_response_time_minutes,
    AVG(resolution_time_minutes) as avg_resolution_time_minutes,
    AVG(CASE WHEN status = 'closed' AND resolution_time_minutes IS NOT NULL 
        THEN resolution_time_minutes END) as avg_fix_time_minutes
FROM issues 
WHERE issue_type = 'bug'
GROUP BY project_id, assignee_id, assignee_name, issue_type, priority, severity;

-- 创建视图：代码评审效率统计
CREATE VIEW code_review_efficiency AS
SELECT 
    mr.project_id,
    cr.reviewer_id,
    cr.reviewer_name,
    COUNT(DISTINCT mr.id) as reviewed_mrs,
    AVG(TIMESTAMPDIFF(HOUR, mr.created_at, cr.reviewed_at)) as avg_review_time_hours,
    COUNT(CASE WHEN cr.status = 'approved' THEN 1 END) as approved_count,
    COUNT(CASE WHEN cr.status = 'changes_requested' THEN 1 END) as changes_requested_count
FROM merge_requests mr
JOIN code_reviews cr ON mr.id = cr.merge_request_id
GROUP BY mr.project_id, cr.reviewer_id, cr.reviewer_name;