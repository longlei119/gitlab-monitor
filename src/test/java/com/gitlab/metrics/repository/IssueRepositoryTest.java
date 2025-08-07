package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.Issue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IssueRepository单元测试
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class IssueRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private IssueRepository issueRepository;
    
    private Issue testIssue1;
    private Issue testIssue2;
    private Issue testIssue3;
    
    @Before
    public void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);
        
        testIssue1 = new Issue("1", "project1", "Critical Bug", "dev1", "Developer One", now, "opened");
        testIssue1.setDescription("This is a critical bug");
        testIssue1.setAssigneeId("dev2");
        testIssue1.setAssigneeName("Developer Two");
        testIssue1.setIssueType("bug");
        testIssue1.setPriority("high");
        testIssue1.setSeverity("critical");
        testIssue1.setLabels("bug,critical");
        testIssue1.setWeight(5);
        testIssue1.setTimeEstimate(7200); // 2 hours
        testIssue1.setMilestoneTitle("v1.0");
        
        testIssue2 = new Issue("2", "project1", "Feature Request", "dev3", "Developer Three", yesterday, "closed");
        testIssue2.setDescription("Add new feature");
        testIssue2.setAssigneeId("dev1");
        testIssue2.setAssigneeName("Developer One");
        testIssue2.setClosedAt(yesterday.plusHours(4));
        testIssue2.setIssueType("feature");
        testIssue2.setPriority("medium");
        testIssue2.setSeverity("minor");
        testIssue2.setLabels("feature,enhancement");
        testIssue2.setWeight(3);
        testIssue2.setTimeEstimate(14400); // 4 hours
        testIssue2.setTimeSpent(10800); // 3 hours
        testIssue2.setFirstResponseAt(yesterday.plusHours(1));
        testIssue2.setResolutionAt(yesterday.plusHours(4));
        testIssue2.setResponseTimeMinutes(60L);
        testIssue2.setResolutionTimeMinutes(240L);
        testIssue2.setMilestoneTitle("v1.0");
        
        testIssue3 = new Issue("3", "project2", "Performance Issue", "dev2", "Developer Two", lastWeek, "closed");
        testIssue3.setDescription("System is slow");
        testIssue3.setAssigneeId("dev3");
        testIssue3.setAssigneeName("Developer Three");
        testIssue3.setClosedAt(lastWeek.plusDays(2));
        testIssue3.setIssueType("bug");
        testIssue3.setPriority("low");
        testIssue3.setSeverity("major");
        testIssue3.setLabels("performance,bug");
        testIssue3.setWeight(2);
        testIssue3.setTimeEstimate(3600); // 1 hour
        testIssue3.setTimeSpent(5400); // 1.5 hours
        testIssue3.setFirstResponseAt(lastWeek.plusHours(2));
        testIssue3.setResolutionAt(lastWeek.plusDays(2));
        testIssue3.setResponseTimeMinutes(120L);
        testIssue3.setResolutionTimeMinutes(2880L); // 48 hours
        testIssue3.setMilestoneTitle("v1.1");
        
        entityManager.persistAndFlush(testIssue1);
        entityManager.persistAndFlush(testIssue2);
        entityManager.persistAndFlush(testIssue3);
    }
    
    @Test
    public void testFindByIssueId() {
        Optional<Issue> found = issueRepository.findByIssueId("1");
        
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Critical Bug");
        assertThat(found.get().getProjectId()).isEqualTo("project1");
    }
    
    @Test
    public void testFindByIssueId_NotFound() {
        Optional<Issue> found = issueRepository.findByIssueId("999");
        
        assertThat(found).isNotPresent();
    }
    
    @Test
    public void testFindByProjectIdOrderByCreatedAtDesc() {
        List<Issue> issues = issueRepository.findByProjectIdOrderByCreatedAtDesc("project1");
        
        assertThat(issues).hasSize(2);
        assertThat(issues.get(0).getIssueId()).isEqualTo("1"); // 最新的在前
        assertThat(issues.get(1).getIssueId()).isEqualTo("2");
    }
    
    @Test
    public void testFindByAssigneeIdOrderByCreatedAtDesc() {
        List<Issue> issues = issueRepository.findByAssigneeIdOrderByCreatedAtDesc("dev1");
        
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getIssueId()).isEqualTo("2");
    }
    
    @Test
    public void testFindByStatusOrderByCreatedAtDesc() {
        List<Issue> issues = issueRepository.findByStatusOrderByCreatedAtDesc("opened");
        
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getIssueId()).isEqualTo("1");
    }
    
    @Test
    public void testFindByIssueTypeOrderByCreatedAtDesc() {
        List<Issue> issues = issueRepository.findByIssueTypeOrderByCreatedAtDesc("bug");
        
        assertThat(issues).hasSize(2);
        assertThat(issues.get(0).getIssueId()).isEqualTo("1"); // 最新的在前
        assertThat(issues.get(1).getIssueId()).isEqualTo("3");
    }
    
    @Test
    public void testFindByProjectIdAndCreatedAtBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Issue> issues = issueRepository.findByProjectIdAndCreatedAtBetween("project1", start, end);
        
        assertThat(issues).hasSize(2);
    }
    
    @Test
    public void testFindByAssigneeIdAndCreatedAtBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Issue> issues = issueRepository.findByAssigneeIdAndCreatedAtBetween("dev2", start, end);
        
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getIssueId()).isEqualTo("1");
    }
    
    @Test
    public void testGetDeveloperIssueStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getDeveloperIssueStats(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1的统计数据
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[0]).isEqualTo("dev1"); // assigneeId
        assertThat(dev1Stats[1]).isEqualTo("Developer One"); // assigneeName
        assertThat(dev1Stats[2]).isEqualTo(1L); // Issue总数
        assertThat(dev1Stats[3]).isEqualTo(1L); // 已关闭数量
        assertThat(dev1Stats[4]).isEqualTo(4.0); // 平均解决时间（小时）
    }
    
    @Test
    public void testGetProjectIssueStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getProjectIssueStats(start, end);
        
        assertThat(stats).hasSize(1); // 只有project1在时间范围内
        
        Object[] project1Stats = stats.get(0);
        assertThat(project1Stats[0]).isEqualTo("project1"); // projectId
        assertThat(project1Stats[1]).isEqualTo(2L); // Issue总数
        assertThat(project1Stats[2]).isEqualTo(1L); // 已关闭数量
        assertThat(project1Stats[3]).isEqualTo(1L); // 开放数量
        assertThat(project1Stats[4]).isEqualTo(1L); // Bug数量
    }
    
    @Test
    public void testGetDeveloperIssueStatsByProject() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getDeveloperIssueStatsByProject("project1", start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1在project1的统计数据
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[2]).isEqualTo(1L); // 在project1中的Issue数量
    }
    
    @Test
    public void testGetIssueTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = issueRepository.getIssueTrendByDate(start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证趋势数据包含日期和Issue统计
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 日期 + 3个统计指标
    }
    
    @Test
    public void testGetIssueTrendByProjectAndDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = issueRepository.getIssueTrendByProjectAndDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证项目Issue趋势数据
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 项目ID + 日期 + 2个统计指标
    }
    
    @Test
    public void testGetBugFixEfficiencyStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = issueRepository.getBugFixEfficiencyStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(240.0); // 平均解决时间（分钟）
        assertThat(stats[1]).isEqualTo(240L); // 最短解决时间
        assertThat(stats[2]).isEqualTo(240L); // 最长解决时间
        assertThat(stats[3]).isEqualTo(60.0); // 平均响应时间
    }
    
    @Test
    public void testGetBugFixTimesBySeverity() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getBugFixTimesBySeverity("project1", start, end);
        
        assertThat(stats).hasSize(1); // 只有minor严重程度的已关闭bug
        
        Object[] minorStats = stats.get(0);
        assertThat(minorStats[0]).isEqualTo("minor"); // severity
        assertThat(minorStats[1]).isEqualTo(1L); // bug数量
        assertThat(minorStats[2]).isEqualTo(240.0); // 平均解决时间
        assertThat(minorStats[3]).isEqualTo(60.0); // 平均响应时间
    }
    
    @Test
    public void testGetBugFixTimesByPriority() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getBugFixTimesByPriority("project1", start, end);
        
        assertThat(stats).hasSize(1); // 只有medium优先级的已关闭bug
        
        Object[] mediumStats = stats.get(0);
        assertThat(mediumStats[0]).isEqualTo("medium"); // priority
        assertThat(mediumStats[1]).isEqualTo(1L); // bug数量
        assertThat(mediumStats[2]).isEqualTo(240.0); // 平均解决时间
        assertThat(mediumStats[3]).isEqualTo(60.0); // 平均响应时间
    }
    
    @Test
    public void testFindLongPendingIssues() {
        List<Issue> pending = issueRepository.findLongPendingIssues(1);
        
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getIssueId()).isEqualTo("1");
        assertThat(pending.get(0).getStatus()).isEqualTo("opened");
    }
    
    @Test
    public void testFindLongPendingIssuesByProject() {
        List<Issue> pending = issueRepository.findLongPendingIssuesByProject("project1", 1);
        
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getIssueId()).isEqualTo("1");
    }
    
    @Test
    public void testFindOverdueIssues() {
        // 设置一个过期的Issue
        testIssue1.setDueDate(LocalDateTime.now().minusDays(1));
        entityManager.persistAndFlush(testIssue1);
        
        List<Issue> overdue = issueRepository.findOverdueIssues();
        
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getIssueId()).isEqualTo("1");
    }
    
    @Test
    public void testFindOverdueIssuesByProject() {
        // 设置一个过期的Issue
        testIssue1.setDueDate(LocalDateTime.now().minusDays(1));
        entityManager.persistAndFlush(testIssue1);
        
        List<Issue> overdue = issueRepository.findOverdueIssuesByProject("project1");
        
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getIssueId()).isEqualTo("1");
    }
    
    @Test
    public void testGetIssueTypeStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getIssueTypeStats("project1", start, end);
        
        assertThat(stats).hasSize(2); // bug和feature
        
        // 验证bug类型统计
        Object[] bugStats = stats.stream()
            .filter(stat -> "bug".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(bugStats).isNotNull();
        assertThat(bugStats[1]).isEqualTo(1L); // bug数量
        
        // 验证feature类型统计
        Object[] featureStats = stats.stream()
            .filter(stat -> "feature".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(featureStats).isNotNull();
        assertThat(featureStats[1]).isEqualTo(1L); // feature数量
    }
    
    @Test
    public void testGetIssueStatusStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getIssueStatusStats("project1", start, end);
        
        assertThat(stats).hasSize(2); // opened和closed
        
        // 验证opened状态统计
        Object[] openedStats = stats.stream()
            .filter(stat -> "opened".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(openedStats).isNotNull();
        assertThat(openedStats[1]).isEqualTo(1L); // opened数量
    }
    
    @Test
    public void testGetIssuePriorityStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getIssuePriorityStats("project1", start, end);
        
        assertThat(stats).hasSize(2); // high和medium
        
        // 验证high优先级统计
        Object[] highStats = stats.stream()
            .filter(stat -> "high".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(highStats).isNotNull();
        assertThat(highStats[1]).isEqualTo(1L); // high优先级数量
    }
    
    @Test
    public void testGetMostActiveIssueCreators() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> creators = issueRepository.getMostActiveIssueCreators(start, end);
        
        assertThat(creators).hasSize(2);
        
        // 验证最活跃的Issue创建者
        Object[] topCreator = creators.get(0);
        assertThat(topCreator[2]).isEqualTo(1L); // Issue创建数量
    }
    
    @Test
    public void testGetMostEfficientIssueResolvers() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> resolvers = issueRepository.getMostEfficientIssueResolvers(start, end, 1);
        
        assertThat(resolvers).hasSize(1);
        
        Object[] topResolver = resolvers.get(0);
        assertThat(topResolver[0]).isEqualTo("dev1"); // assigneeId
        assertThat(topResolver[1]).isEqualTo("Developer One"); // assigneeName
        assertThat(topResolver[2]).isEqualTo(1L); // 解决的Issue数量
        assertThat(topResolver[3]).isEqualTo(240.0); // 平均解决时间
    }
    
    @Test
    public void testGetMilestoneIssueStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = issueRepository.getMilestoneIssueStats("project1", start, end);
        
        assertThat(stats).hasSize(1); // 只有v1.0里程碑
        
        Object[] v1Stats = stats.get(0);
        assertThat(v1Stats[0]).isEqualTo("v1.0"); // milestoneTitle
        assertThat(v1Stats[1]).isEqualTo(2L); // Issue总数
        assertThat(v1Stats[2]).isEqualTo(1L); // 已关闭数量
    }
    
    @Test
    public void testCompareIssueEfficiencyBetweenPeriods() {
        LocalDateTime period1Start = LocalDateTime.now().minusDays(2);
        LocalDateTime period1End = LocalDateTime.now().minusDays(1);
        LocalDateTime period2Start = LocalDateTime.now().minusHours(12);
        LocalDateTime period2End = LocalDateTime.now().plusDays(1);
        
        Object[] comparison = issueRepository.compareIssueEfficiencyBetweenPeriods(
            "project1", period1Start, period1End, period2Start, period2End);
        
        assertThat(comparison).isNotNull();
        assertThat(comparison).hasSize(4); // 两个时期的Issue数量和平均解决时间对比
    }
}