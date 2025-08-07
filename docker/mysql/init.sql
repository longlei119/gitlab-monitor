-- GitLab Metrics Database Initialization Script

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS gitlab_metrics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE gitlab_metrics;

-- Create user if not exists (for MySQL 8.0+)
CREATE USER IF NOT EXISTS 'gitlab_user'@'%' IDENTIFIED BY 'gitlab_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON gitlab_metrics.* TO 'gitlab_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Create indexes for better performance (tables will be created by JPA)
-- These will be applied after JPA creates the tables

-- Set timezone
SET time_zone = '+08:00';