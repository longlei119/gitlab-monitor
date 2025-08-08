-- Partitioning strategy for large tables to improve performance
-- This script adds partitioning for time-series data

USE gitlab_metrics;

-- Note: This script should be run after the application has created the initial tables
-- Partitioning will be applied to tables with time-series data

-- Create partitioned commits table (by month)
-- This is a template - actual partitioning should be done based on data volume

-- Add partition management stored procedure
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS CreateMonthlyPartition(
    IN table_name VARCHAR(64),
    IN partition_date DATE
)
BEGIN
    DECLARE partition_name VARCHAR(64);
    DECLARE partition_description VARCHAR(64);
    
    SET partition_name = CONCAT('p', DATE_FORMAT(partition_date, '%Y%m'));
    SET partition_description = DATE_FORMAT(DATE_ADD(partition_date, INTERVAL 1 MONTH), '%Y-%m-01');
    
    SET @sql = CONCAT('ALTER TABLE ', table_name, 
                     ' ADD PARTITION (PARTITION ', partition_name,
                     ' VALUES LESS THAN (TO_DAYS(''', partition_description, ''')))');
    
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END //

DELIMITER ;

-- Create procedure to automatically create partitions for the next 12 months
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS CreateFuturePartitions(
    IN table_name VARCHAR(64)
)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE partition_date DATE;
    
    WHILE i < 12 DO
        SET partition_date = DATE_ADD(CURDATE(), INTERVAL i MONTH);
        CALL CreateMonthlyPartition(table_name, partition_date);
        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

-- Create procedure to drop old partitions (older than 2 years)
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS DropOldPartitions(
    IN table_name VARCHAR(64)
)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE partition_name VARCHAR(64);
    DECLARE partition_cursor CURSOR FOR
        SELECT PARTITION_NAME 
        FROM INFORMATION_SCHEMA.PARTITIONS 
        WHERE TABLE_SCHEMA = 'gitlab_metrics' 
        AND TABLE_NAME = table_name
        AND PARTITION_NAME IS NOT NULL
        AND PARTITION_DESCRIPTION < TO_DAYS(DATE_SUB(CURDATE(), INTERVAL 2 YEAR));
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN partition_cursor;
    
    read_loop: LOOP
        FETCH partition_cursor INTO partition_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET @sql = CONCAT('ALTER TABLE ', table_name, ' DROP PARTITION ', partition_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    
    CLOSE partition_cursor;
END //

DELIMITER ;

-- Create event scheduler for automatic partition management
-- Note: This requires EVENT_SCHEDULER to be enabled

-- Create monthly partition maintenance event
CREATE EVENT IF NOT EXISTS monthly_partition_maintenance
ON SCHEDULE EVERY 1 MONTH
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    -- Create future partitions for commits table
    CALL CreateFuturePartitions('commits');
    
    -- Create future partitions for quality_metrics table
    CALL CreateFuturePartitions('quality_metrics');
    
    -- Create future partitions for test_coverage table
    CALL CreateFuturePartitions('test_coverage');
    
    -- Drop old partitions
    CALL DropOldPartitions('commits');
    CALL DropOldPartitions('quality_metrics');
    CALL DropOldPartitions('test_coverage');
END;