-- Performance optimization indexes for GitLab Metrics Database
-- This script adds indexes after JPA creates the initial tables

USE gitlab_metrics;

-- Commits table indexes
CREATE INDEX IF NOT EXISTS idx_commits_developer_timestamp ON commits(developer_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_commits_project_timestamp ON commits(project_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_commits_branch ON commits(branch);
CREATE INDEX IF NOT EXISTS idx_commits_timestamp ON commits(timestamp);

-- Quality Metrics table indexes
CREATE INDEX IF NOT EXISTS idx_quality_metrics_project_timestamp ON quality_metrics(project_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_commit_sha ON quality_metrics(commit_sha);

-- Test Coverage table indexes
CREATE INDEX IF NOT EXISTS idx_test_coverage_project_timestamp ON test_coverage(project_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_test_coverage_commit_sha ON test_coverage(commit_sha);

-- Merge Requests table indexes
CREATE INDEX IF NOT EXISTS idx_merge_requests_author_created ON merge_requests(author_id, created_at);
CREATE INDEX IF NOT EXISTS idx_merge_requests_project_status ON merge_requests(project_id, status);
CREATE INDEX IF NOT EXISTS idx_merge_requests_created_at ON merge_requests(created_at);

-- Code Reviews table indexes
CREATE INDEX IF NOT EXISTS idx_code_reviews_reviewer_reviewed ON code_reviews(reviewer_id, reviewed_at);
CREATE INDEX IF NOT EXISTS idx_code_reviews_mr_status ON code_reviews(merge_request_id, status);

-- Issues table indexes
CREATE INDEX IF NOT EXISTS idx_issues_assignee_created ON issues(assignee_id, created_at);
CREATE INDEX IF NOT EXISTS idx_issues_project_status ON issues(project_id, status);
CREATE INDEX IF NOT EXISTS idx_issues_severity ON issues(severity);
CREATE INDEX IF NOT EXISTS idx_issues_created_at ON issues(created_at);

-- File Changes table indexes
CREATE INDEX IF NOT EXISTS idx_file_changes_commit_id ON file_changes(commit_id);
CREATE INDEX IF NOT EXISTS idx_file_changes_file_path ON file_changes(file_path);

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_commits_project_developer_timestamp ON commits(project_id, developer_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_project_commit ON quality_metrics(project_id, commit_sha);
CREATE INDEX IF NOT EXISTS idx_test_coverage_project_commit ON test_coverage(project_id, commit_sha);