package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.TestCoverage;
import com.gitlab.metrics.repository.TestCoverageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试覆盖率服务
 * 负责解析各种格式的测试覆盖率报告并存储到数据库
 */
@Service
@Transactional
public class TestCoverageService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestCoverageService.class);
    
    @Autowired
    private TestCoverageRepository testCoverageRepository;
    
    /**
     * 解析并保存测试覆盖率报告
     * 
     * @param projectId 项目ID
     * @param commitSha 提交SHA
     * @param reportContent 报告内容
     * @param reportType 报告类型 (jacoco, cobertura, lcov)
     * @param reportPath 报告文件路径
     * @return 解析后的测试覆盖率对象
     */
    public TestCoverage parseCoverageReport(String projectId, String commitSha, 
                                          String reportContent, String reportType, String reportPath) {
        logger.info("开始解析测试覆盖率报告: projectId={}, commitSha={}, reportType={}", 
                   projectId, commitSha, reportType);
        
        TestCoverage coverage = new TestCoverage(projectId, commitSha, LocalDateTime.now());
        coverage.setReportType(reportType.toLowerCase());
        coverage.setReportPath(reportPath);
        
        try {
            switch (reportType.toLowerCase()) {
                case "jacoco":
                    parseJaCoCoReport(coverage, reportContent);
                    break;
                case "cobertura":
                    parseCoberturaReport(coverage, reportContent);
                    break;
                case "lcov":
                    parseLcovReport(coverage, reportContent);
                    break;
                default:
                    logger.warn("不支持的报告类型: {}", reportType);
                    throw new IllegalArgumentException("不支持的报告类型: " + reportType);
            }
            
            // 保存到数据库
            TestCoverage savedCoverage = testCoverageRepository.save(coverage);
            logger.info("测试覆盖率报告解析完成: id={}, lineCoverage={}", 
                       savedCoverage.getId(), savedCoverage.getLineCoverage());
            
            return savedCoverage;
            
        } catch (Exception e) {
            logger.error("解析测试覆盖率报告失败: projectId={}, commitSha={}, error={}", 
                        projectId, commitSha, e.getMessage(), e);
            coverage.setStatus("FAILED");
            testCoverageRepository.save(coverage);
            throw new RuntimeException("解析测试覆盖率报告失败", e);
        }
    }
    
    /**
     * 解析JaCoCo XML报告
     */
    private void parseJaCoCoReport(TestCoverage coverage, String reportContent) throws Exception {
        logger.debug("解析JaCoCo报告");
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(reportContent.getBytes()));
        
        // 获取根节点的counter信息
        Element root = doc.getDocumentElement();
        NodeList counters = root.getElementsByTagName("counter");
        
        int totalLines = 0, coveredLines = 0;
        int totalBranches = 0, coveredBranches = 0;
        int totalMethods = 0, coveredMethods = 0;
        int totalClasses = 0, coveredClasses = 0;
        
        for (int i = 0; i < counters.getLength(); i++) {
            Element counter = (Element) counters.item(i);
            String type = counter.getAttribute("type");
            int missed = Integer.parseInt(counter.getAttribute("missed"));
            int covered = Integer.parseInt(counter.getAttribute("covered"));
            
            switch (type) {
                case "LINE":
                    totalLines = missed + covered;
                    coveredLines = covered;
                    break;
                case "BRANCH":
                    totalBranches = missed + covered;
                    coveredBranches = covered;
                    break;
                case "METHOD":
                    totalMethods = missed + covered;
                    coveredMethods = covered;
                    break;
                case "CLASS":
                    totalClasses = missed + covered;
                    coveredClasses = covered;
                    break;
            }
        }
        
        // 计算覆盖率百分比
        coverage.setTotalLines(totalLines);
        coverage.setCoveredLines(coveredLines);
        coverage.setLineCoverage(totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0.0);
        
        coverage.setTotalBranches(totalBranches);
        coverage.setCoveredBranches(coveredBranches);
        coverage.setBranchCoverage(totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0.0);
        
        coverage.setTotalFunctions(totalMethods);
        coverage.setCoveredFunctions(coveredMethods);
        coverage.setFunctionCoverage(totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0.0);
        
        coverage.setTotalClasses(totalClasses);
        coverage.setCoveredClasses(coveredClasses);
        
        coverage.setStatus("PASSED");
        
        logger.debug("JaCoCo报告解析完成: 行覆盖率={}%, 分支覆盖率={}%, 方法覆盖率={}%", 
                    coverage.getLineCoverage(), coverage.getBranchCoverage(), coverage.getFunctionCoverage());
    }
    
    /**
     * 解析Cobertura XML报告
     */
    private void parseCoberturaReport(TestCoverage coverage, String reportContent) throws Exception {
        logger.debug("解析Cobertura报告");
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(reportContent.getBytes()));
        
        Element root = doc.getDocumentElement();
        
        // 从coverage元素获取总体覆盖率
        String lineRate = root.getAttribute("line-rate");
        String branchRate = root.getAttribute("branch-rate");
        
        if (!lineRate.isEmpty()) {
            coverage.setLineCoverage(Double.parseDouble(lineRate) * 100);
        }
        
        if (!branchRate.isEmpty()) {
            coverage.setBranchCoverage(Double.parseDouble(branchRate) * 100);
        }
        
        // 统计详细信息
        NodeList packages = root.getElementsByTagName("package");
        int totalLines = 0, coveredLines = 0;
        int totalBranches = 0, coveredBranches = 0;
        
        for (int i = 0; i < packages.getLength(); i++) {
            Element pkg = (Element) packages.item(i);
            NodeList classes = pkg.getElementsByTagName("class");
            
            for (int j = 0; j < classes.getLength(); j++) {
                Element clazz = (Element) classes.item(j);
                NodeList lines = clazz.getElementsByTagName("line");
                
                for (int k = 0; k < lines.getLength(); k++) {
                    Element line = (Element) lines.item(k);
                    int hits = Integer.parseInt(line.getAttribute("hits"));
                    String branch = line.getAttribute("branch");
                    
                    totalLines++;
                    if (hits > 0) {
                        coveredLines++;
                    }
                    
                    if ("true".equals(branch)) {
                        String conditionCoverage = line.getAttribute("condition-coverage");
                        if (!conditionCoverage.isEmpty()) {
                            // 解析分支覆盖率 "50% (1/2)"
                            Pattern pattern = Pattern.compile("\\((\\d+)/(\\d+)\\)");
                            Matcher matcher = pattern.matcher(conditionCoverage);
                            if (matcher.find()) {
                                int covered = Integer.parseInt(matcher.group(1));
                                int total = Integer.parseInt(matcher.group(2));
                                totalBranches += total;
                                coveredBranches += covered;
                            }
                        }
                    }
                }
            }
        }
        
        coverage.setTotalLines(totalLines);
        coverage.setCoveredLines(coveredLines);
        coverage.setTotalBranches(totalBranches);
        coverage.setCoveredBranches(coveredBranches);
        
        coverage.setStatus("PASSED");
        
        logger.debug("Cobertura报告解析完成: 行覆盖率={}%, 分支覆盖率={}%", 
                    coverage.getLineCoverage(), coverage.getBranchCoverage());
    }
    
    /**
     * 解析LCOV报告
     */
    private void parseLcovReport(TestCoverage coverage, String reportContent) {
        logger.debug("解析LCOV报告");
        
        String[] lines = reportContent.split("\n");
        int totalLines = 0, coveredLines = 0;
        int totalFunctions = 0, coveredFunctions = 0;
        int totalBranches = 0, coveredBranches = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("LF:")) {
                // Lines found
                totalLines += Integer.parseInt(line.substring(3));
            } else if (line.startsWith("LH:")) {
                // Lines hit
                coveredLines += Integer.parseInt(line.substring(3));
            } else if (line.startsWith("FNF:")) {
                // Functions found
                totalFunctions += Integer.parseInt(line.substring(4));
            } else if (line.startsWith("FNH:")) {
                // Functions hit
                coveredFunctions += Integer.parseInt(line.substring(4));
            } else if (line.startsWith("BRF:")) {
                // Branches found
                totalBranches += Integer.parseInt(line.substring(4));
            } else if (line.startsWith("BRH:")) {
                // Branches hit
                coveredBranches += Integer.parseInt(line.substring(4));
            }
        }
        
        coverage.setTotalLines(totalLines);
        coverage.setCoveredLines(coveredLines);
        coverage.setLineCoverage(totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0.0);
        
        coverage.setTotalFunctions(totalFunctions);
        coverage.setCoveredFunctions(coveredFunctions);
        coverage.setFunctionCoverage(totalFunctions > 0 ? (double) coveredFunctions / totalFunctions * 100 : 0.0);
        
        coverage.setTotalBranches(totalBranches);
        coverage.setCoveredBranches(coveredBranches);
        coverage.setBranchCoverage(totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0.0);
        
        coverage.setStatus("PASSED");
        
        logger.debug("LCOV报告解析完成: 行覆盖率={}%, 分支覆盖率={}%, 函数覆盖率={}%", 
                    coverage.getLineCoverage(), coverage.getBranchCoverage(), coverage.getFunctionCoverage());
    }
    
    /**
     * 从文件路径解析覆盖率报告
     */
    public TestCoverage parseCoverageReportFromFile(String projectId, String commitSha, 
                                                   String filePath, String reportType) {
        logger.info("从文件解析测试覆盖率报告: filePath={}", filePath);
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("覆盖率报告文件不存在: " + filePath);
            }
            
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            return parseCoverageReport(projectId, commitSha, content, reportType, filePath);
            
        } catch (Exception e) {
            logger.error("从文件解析覆盖率报告失败: filePath={}, error={}", filePath, e.getMessage(), e);
            throw new RuntimeException("从文件解析覆盖率报告失败", e);
        }
    }
    
    /**
     * 获取项目的覆盖率历史记录
     */
    @Transactional(readOnly = true)
    public List<TestCoverage> getCoverageHistory(String projectId) {
        return testCoverageRepository.findByProjectIdOrderByTimestampDesc(projectId);
    }
    
    /**
     * 获取项目的最新覆盖率
     */
    @Transactional(readOnly = true)
    public Optional<TestCoverage> getLatestCoverage(String projectId) {
        List<TestCoverage> coverages = testCoverageRepository.findLatestByProject(projectId);
        return coverages.isEmpty() ? Optional.empty() : Optional.of(coverages.get(0));
    }
    
    /**
     * 获取指定时间范围内的覆盖率记录
     */
    @Transactional(readOnly = true)
    public List<TestCoverage> getCoverageByDateRange(String projectId, LocalDateTime start, LocalDateTime end) {
        return testCoverageRepository.findByProjectIdAndTimestampBetween(projectId, start, end);
    }
    
    /**
     * 获取覆盖率趋势分析
     */
    @Transactional(readOnly = true)
    public CoverageTrend getCoverageTrend(String projectId, LocalDateTime start, LocalDateTime end) {
        Object[] result = testCoverageRepository.getCoverageTrend(projectId, start, end);
        
        if (result != null && result.length >= 3) {
            CoverageTrend trend = new CoverageTrend();
            trend.setProjectId(projectId);
            trend.setStartDate(start);
            trend.setEndDate(end);
            trend.setAverageLineCoverage(result[0] != null ? (Double) result[0] : 0.0);
            trend.setAverageBranchCoverage(result[1] != null ? (Double) result[1] : 0.0);
            trend.setAverageFunctionCoverage(result[2] != null ? (Double) result[2] : 0.0);
            return trend;
        }
        
        return new CoverageTrend(projectId, start, end, 0.0, 0.0, 0.0);
    }
    
    /**
     * 删除旧的覆盖率记录
     */
    @Transactional
    public void cleanupOldCoverageRecords(LocalDateTime before) {
        logger.info("清理旧的覆盖率记录: before={}", before);
        
        List<TestCoverage> oldRecords = testCoverageRepository.findAll()
            .stream()
            .filter(coverage -> coverage.getTimestamp().isBefore(before))
            .collect(java.util.stream.Collectors.toList());
        
        if (!oldRecords.isEmpty()) {
            testCoverageRepository.deleteAll(oldRecords);
            logger.info("已清理 {} 条旧的覆盖率记录", oldRecords.size());
        }
    }
    
    /**
     * 覆盖率趋势数据类
     */
    public static class CoverageTrend {
        private String projectId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Double averageLineCoverage;
        private Double averageBranchCoverage;
        private Double averageFunctionCoverage;
        
        public CoverageTrend() {}
        
        public CoverageTrend(String projectId, LocalDateTime startDate, LocalDateTime endDate,
                           Double averageLineCoverage, Double averageBranchCoverage, Double averageFunctionCoverage) {
            this.projectId = projectId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.averageLineCoverage = averageLineCoverage;
            this.averageBranchCoverage = averageBranchCoverage;
            this.averageFunctionCoverage = averageFunctionCoverage;
        }
        
        // Getters and Setters
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        
        public Double getAverageLineCoverage() { return averageLineCoverage; }
        public void setAverageLineCoverage(Double averageLineCoverage) { this.averageLineCoverage = averageLineCoverage; }
        
        public Double getAverageBranchCoverage() { return averageBranchCoverage; }
        public void setAverageBranchCoverage(Double averageBranchCoverage) { this.averageBranchCoverage = averageBranchCoverage; }
        
        public Double getAverageFunctionCoverage() { return averageFunctionCoverage; }
        public void setAverageFunctionCoverage(Double averageFunctionCoverage) { this.averageFunctionCoverage = averageFunctionCoverage; }
    }
}