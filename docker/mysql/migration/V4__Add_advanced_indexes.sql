-- Advanced performance indexes for GitLab Metrics Database
-- This script adds specialized indexes for complex queries and performance optimization

USE gitlab_metrics;

-- Advanced composite indexes for commits table
CREATE INDEX IF NOT EXISTS idx_commits_project_branch_timestamp ON commits(project_id, branch, timestamp);
CREATE INDEX IF NOT EXISTS idx_commits_developer_project_timestamp ON commits(developer_id, project_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_commits_timestamp_lines ON commits(timestamp, lines_added, lines_deleted);

-- Covering indexes for common queries
CREATE INDEX IF NOT EXISTS idx_commits_stats_covering ON commits(project_id, timestamp, developer_id, lines_added, lines_deleted, files_changed);
CREATE INDEX IF NOT EXISTS idx_commits_developer_covering ON commits(developer_id, timestamp, project_id, lines_added, lines_deleted);

-- Quality metrics advanced indexes
CREATE INDEX IF NOT EXISTS idx_quality_metrics_project_timestamp_covering ON quality_metrics(project_id, timestamp, maintainability_index, bugs, vulnerabilities);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_quality_gate ON quality_metrics(quality_gate, timestamp);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_complexity ON quality_metrics(code_complexity, timestamp);

-- Test coverage advanced indexes
CREATE INDEX IF NOT EXISTS idx_test_coverage_project_coverage ON test_coverage(project_id, line_coverage, timestamp);
CREATE INDEX IF NOT EXISTS idx_test_coverage_coverage_range ON test_coverage(line_coverage, branch_coverage, timestamp);

-- Merge requests advanced indexes
CREATE INDEX IF NOT EXISTS idx_merge_requests_author_project_status ON merge_requests(author_id, project_id, status);
CREATE INDEX IF NOT EXISTS idx_merge_requests_status_created ON merge_requests(status, created_at);
CREATE INDEX IF NOT EXISTS idx_merge_requests_merged_time ON merge_requests(merged_at, created_at) WHERE merged_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_merge_requests_target_branch ON merge_requests(target_branch, project_id, status);

-- Code reviews advanced indexes
CREATE INDEX IF NOT EXISTS idx_code_reviews_reviewer_status_time ON code_reviews(reviewer_id, status, reviewed_at);
CREATE INDEX IF NOT EXISTS idx_code_reviews_mr_reviewer ON code_reviews(merge_request_id, reviewer_id);

-- Issues advanced indexes
CREATE INDEX IF NOT EXISTS idx_issues_project_type_status ON issues(project_id, issue_type, status);
CREATE INDEX IF NOT EXISTS idx_issues_assignee_status_created ON issues(assignee_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_issues_severity_created ON issues(severity, created_at);
CREATE INDEX IF NOT EXISTS idx_issues_closed_time ON issues(closed_at, created_at) WHERE closed_at IS NOT NULL;

-- File changes advanced indexes
CREATE INDEX IF NOT EXISTS idx_file_changes_path_type ON file_changes(file_path, change_type);
CREATE INDEX IF NOT EXISTS idx_file_changes_commit_path ON file_changes(commit_id, file_path);
CREATE INDEX IF NOT EXISTS idx_file_changes_lines ON file_changes(lines_added, lines_deleted);

-- Functional indexes for calculated fields
CREATE INDEX IF NOT EXISTS idx_commits_total_lines ON commits((lines_added + lines_deleted), timestamp);
CREATE INDEX IF NOT EXISTS idx_merge_requests_processing_time ON merge_requests((TIMESTAMPDIFF(HOUR, created_at, merged_at))) WHERE merged_at IS NOT NULL;

-- Partial indexes for specific conditions
CREATE INDEX IF NOT EXISTS idx_commits_large_commits ON commits(project_id, timestamp, lines_added, lines_deleted) 
    WHERE (lines_added + lines_deleted) > 500;
CREATE INDEX IF NOT EXISTS idx_quality_metrics_failed_gate ON quality_metrics(project_id, timestamp) 
    WHERE quality_gate = 'FAILED';
CREATE INDEX IF NOT EXISTS idx_issues_open_bugs ON issues(project_id, created_at, assignee_id) 
    WHERE issue_type = 'bug' AND status = 'opened';

-- Indexes for aggregation queries
CREATE INDEX IF NOT EXISTS idx_commits_daily_stats ON commits(project_id, DATE(timestamp), developer_id);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_daily_stats ON quality_metrics(project_id, DATE(timestamp));
CREATE INDEX IF NOT EXISTS idx_test_coverage_daily_stats ON test_coverage(project_id, DATE(timestamp));

-- Full-text search indexes (if needed)
-- CREATE FULLTEXT INDEX IF NOT EXISTS idx_commits_message_fulltext ON commits(message);
-- CREATE FULLTEXT INDEX IF NOT EXISTS idx_issues_title_description_fulltext ON issues(title, description);

-- Analyze tables to update statistics
ANALYZE TABLE commits;
ANALYZE TABLE quality_metrics;
ANALYZE TABLE test_coverage;
ANALYZE TABLE merge_requests;
ANALYZE TABLE code_reviews;
ANALYZE TABLE issues;
ANALYZE TABLE file_changes;
ANALYZE TABLE users;

-- Create stored procedure for index maintenance
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS OptimizeIndexes()
BEGIN
    -- Rebuild indexes periodically for better performance
    OPTIMIZE TABLE commits;
    OPTIMIZE TABLE quality_metrics;
    OPTIMIZE TABLE test_coverage;
    OPTIMIZE TABLE merge_requests;
    OPTIMIZE TABLE code_reviews;
    OPTIMIZE TABLE issues;
    OPTIMIZE TABLE file_changes;
    
    -- Update table statistics
    ANALYZE TABLE commits;
    ANALYZE TABLE quality_metrics;
    ANALYZE TABLE test_coverage;
    ANALYZE TABLE merge_requests;
    ANALYZE TABLE code_reviews;
    ANALYZE TABLE issues;
    ANALYZE TABLE file_changes;
END //

DELIMITER ;

-- Create event for periodic index optimization (runs weekly)
CREATE EVENT IF NOT EXISTS weekly_index_optimization
ON SCHEDULE EVERY 1 WEEK
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    CALL OptimizeIndexes();
END;

-- Enable query cache for better read performance
SET GLOBAL query_cache_type = ON;
SET GLOBAL query_cache_size = 268435456; -- 256MB

-- Optimize InnoDB settings for better performance
SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB, adjust based on available memory
SET GLOBAL innodb_log_file_size = 268435456; -- 256MB
SET GLOBAL innodb_flush_log_at_trx_commit = 2; -- Better performance, slightly less durability
SET GLOBAL innodb_flush_method = O_DIRECT;