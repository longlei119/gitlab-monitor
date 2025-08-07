package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.CodeReview;
import com.gitlab.metrics.entity.MergeRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CodeReviewRepository单元测试
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class CodeReviewRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CodeReviewRepository codeReviewRepository;
    
    private MergeRequest testMR1;
    private MergeRequest testMR2;
    private CodeReview testReview1;
    private CodeReview testReview2;
    private CodeReview testReview3;
    
    @Before
    public void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);
        
        // 创建测试用的MergeRequest
        testMR1 = new MergeRequest("1", "project1", "dev1", "Developer One", yesterday, "opened", "feature/new", "main");
        testMR2 = new MergeRequest("2", "project1", "dev2", "Developer Two", lastWeek, "merged", "bugfix/issue", "main");
        
        entityManager.persistAndFlush(testMR1);
        entityManager.persistAndFlush(testMR2);
        
        // 创建测试用的CodeReview
        testReview1 = new CodeReview(testMR1, "reviewer1", "Reviewer One", now, "approved");
        testReview1.setComment("Looks good to me");
        testReview1.setCommentsCount(2);
        testReview1.setSubmittedAt(now.plusMinutes(30));
        testReview1.setReviewType("manual");
        testReview1.setIsRequired(true);
        
        testReview2 = new CodeReview(testMR1, "reviewer2", "Reviewer Two", now.minusHours(1), "changes_requested");
        testReview2.setComment("Please fix the issues");
        testReview2.setCommentsCount(5);
        testReview2.setSubmittedAt(now.minusMinutes(30));
        testReview2.setReviewType("manual");
        testReview2.setIsRequired(true);
        
        testReview3 = new CodeReview(testMR2, "reviewer1", "Reviewer One", lastWeek.plusHours(2), "approved");
        testReview3.setComment("Good work");
        testReview3.setCommentsCount(1);
        testReview3.setSubmittedAt(lastWeek.plusHours(2).plusMinutes(15));
        testReview3.setReviewType("manual");
        testReview3.setIsRequired(false);
        
        entityManager.persistAndFlush(testReview1);
        entityManager.persistAndFlush(testReview2);
        entityManager.persistAndFlush(testReview3);
    }
    
    @Test
    public void testFindByMergeRequestIdOrderByReviewedAtDesc() {
        List<CodeReview> reviews = codeReviewRepository.findByMergeRequestIdOrderByReviewedAtDesc(testMR1.getId());
        
        assertThat(reviews).hasSize(2);
        assertThat(reviews.get(0).getReviewerId()).isEqualTo("reviewer1"); // 最新的在前
        assertThat(reviews.get(1).getReviewerId()).isEqualTo("reviewer2");
    }
    
    @Test
    public void testFindByReviewerIdOrderByReviewedAtDesc() {
        List<CodeReview> reviews = codeReviewRepository.findByReviewerIdOrderByReviewedAtDesc("reviewer1");
        
        assertThat(reviews).hasSize(2);
        assertThat(reviews.get(0).getMergeRequest().getMrId()).isEqualTo("1"); // 最新的在前
        assertThat(reviews.get(1).getMergeRequest().getMrId()).isEqualTo("2");
    }
    
    @Test
    public void testFindByStatusOrderByReviewedAtDesc() {
        List<CodeReview> reviews = codeReviewRepository.findByStatusOrderByReviewedAtDesc("approved");
        
        assertThat(reviews).hasSize(2);
        assertThat(reviews.get(0).getReviewerId()).isEqualTo("reviewer1"); // 最新的在前
    }
    
    @Test
    public void testFindByReviewerIdAndReviewedAtBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<CodeReview> reviews = codeReviewRepository.findByReviewerIdAndReviewedAtBetween("reviewer1", start, end);
        
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getMergeRequest().getMrId()).isEqualTo("1");
    }
    
    @Test
    public void testGetReviewerStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = codeReviewRepository.getReviewerStats(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证reviewer1的统计数据
        Object[] reviewer1Stats = stats.stream()
            .filter(stat -> "reviewer1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(reviewer1Stats).isNotNull();
        assertThat(reviewer1Stats[0]).isEqualTo("reviewer1"); // reviewerId
        assertThat(reviewer1Stats[1]).isEqualTo("Reviewer One"); // reviewerName
        assertThat(reviewer1Stats[2]).isEqualTo(1L); // 评审总数
        assertThat(reviewer1Stats[3]).isEqualTo(1L); // 批准数量
        assertThat(reviewer1Stats[4]).isEqualTo(0L); // 要求修改数量
    }
    
    @Test
    public void testGetProjectReviewStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = codeReviewRepository.getProjectReviewStats(start, end);
        
        assertThat(stats).hasSize(1); // 只有project1在时间范围内
        
        Object[] project1Stats = stats.get(0);
        assertThat(project1Stats[0]).isEqualTo("project1"); // projectId
        assertThat(project1Stats[1]).isEqualTo(2L); // 评审总数
        assertThat(project1Stats[2]).isEqualTo(1L); // 批准数量
        assertThat(project1Stats[3]).isEqualTo(1L); // 要求修改数量
        assertThat(project1Stats[4]).isEqualTo(0L); // 评论数量
    }
    
    @Test
    public void testGetReviewerStatsByProject() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = codeReviewRepository.getReviewerStatsByProject("project1", start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证reviewer1在project1的统计数据
        Object[] reviewer1Stats = stats.stream()
            .filter(stat -> "reviewer1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(reviewer1Stats).isNotNull();
        assertThat(reviewer1Stats[2]).isEqualTo(1L); // 在project1中的评审数量
    }
    
    @Test
    public void testGetReviewTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = codeReviewRepository.getReviewTrendByDate(start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证趋势数据包含日期和评审统计
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 日期 + 3个统计指标
    }
    
    @Test
    public void testGetReviewTrendByProjectAndDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = codeReviewRepository.getReviewTrendByProjectAndDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证项目评审趋势数据
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 项目ID + 日期 + 2个统计指标
    }
    
    @Test
    public void testGetReviewStatusStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = codeReviewRepository.getReviewStatusStats("project1", start, end);
        
        assertThat(stats).hasSize(2); // approved和changes_requested
        
        // 验证approved状态统计
        Object[] approvedStats = stats.stream()
            .filter(stat -> "approved".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(approvedStats).isNotNull();
        assertThat(approvedStats[1]).isEqualTo(1L); // approved数量
        
        // 验证changes_requested状态统计
        Object[] changesStats = stats.stream()
            .filter(stat -> "changes_requested".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(changesStats).isNotNull();
        assertThat(changesStats[1]).isEqualTo(1L); // changes_requested数量
    }
    
    @Test
    public void testGetReviewTypeStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = codeReviewRepository.getReviewTypeStats("project1", start, end);
        
        assertThat(stats).hasSize(1); // 只有manual类型
        
        Object[] manualStats = stats.get(0);
        assertThat(manualStats[0]).isEqualTo("manual"); // reviewType
        assertThat(manualStats[1]).isEqualTo(2L); // manual评审数量
    }
    
    @Test
    public void testGetMostActiveReviewers() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> reviewers = codeReviewRepository.getMostActiveReviewers(start, end);
        
        assertThat(reviewers).hasSize(2);
        
        // 验证最活跃的评审者
        Object[] topReviewer = reviewers.get(0);
        assertThat(topReviewer[2]).isEqualTo(1L); // 评审次数
    }
    
    @Test
    public void testGetRequiredReviewStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = codeReviewRepository.getRequiredReviewStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(2L); // 总评审数
        assertThat(stats[1]).isEqualTo(2L); // 必需评审数
        assertThat(stats[2]).isEqualTo(1L); // 必需评审中已批准数
    }
    
    @Test
    public void testGetReviewResponseTimeStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = codeReviewRepository.getReviewResponseTimeStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isNotNull(); // 平均响应时间
        assertThat(stats[1]).isNotNull(); // 最短响应时间
        assertThat(stats[2]).isNotNull(); // 最长响应时间
    }
    
    @Test
    public void testGetLongestReviewResponseTimes() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> longest = codeReviewRepository.getLongestReviewResponseTimes("project1", start, end);
        
        assertThat(longest).hasSize(2);
        // 验证返回的数据包含CodeReview和响应时间
        Object[] firstResult = longest.get(0);
        assertThat(firstResult).hasSize(2);
        assertThat(firstResult[0]).isInstanceOf(CodeReview.class);
        assertThat(firstResult[1]).isInstanceOf(Long.class); // 响应时间（小时）
    }
    
    @Test
    public void testGetReviewerCommentStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = codeReviewRepository.getReviewerCommentStats(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证reviewer2的评论统计（评论数最多）
        Object[] reviewer2Stats = stats.stream()
            .filter(stat -> "reviewer2".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(reviewer2Stats).isNotNull();
        assertThat(reviewer2Stats[0]).isEqualTo("reviewer2"); // reviewerId
        assertThat(reviewer2Stats[1]).isEqualTo("Reviewer Two"); // reviewerName
        assertThat(reviewer2Stats[2]).isEqualTo(5.0); // 平均评论数
        assertThat(reviewer2Stats[3]).isEqualTo(1L); // 评审次数
    }
    
    @Test
    public void testGetMostRejectedMergeRequests() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> rejected = codeReviewRepository.getMostRejectedMergeRequests(start, end);
        
        assertThat(rejected).hasSize(1);
        
        Object[] mostRejected = rejected.get(0);
        assertThat(mostRejected[0]).isInstanceOf(MergeRequest.class);
        assertThat(mostRejected[1]).isEqualTo(1L); // 拒绝次数
        
        MergeRequest mr = (MergeRequest) mostRejected[0];
        assertThat(mr.getMrId()).isEqualTo("1");
    }
    
    @Test
    public void testGetReviewCoverageStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = codeReviewRepository.getReviewCoverageStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(1L); // 总MR数（在时间范围内）
        assertThat(stats[1]).isEqualTo(1L); // 有评审的MR数
    }
    
    @Test
    public void testCompareReviewEfficiencyBetweenPeriods() {
        LocalDateTime period1Start = LocalDateTime.now().minusDays(2);
        LocalDateTime period1End = LocalDateTime.now().minusDays(1);
        LocalDateTime period2Start = LocalDateTime.now().minusHours(12);
        LocalDateTime period2End = LocalDateTime.now().plusDays(1);
        
        Object[] comparison = codeReviewRepository.compareReviewEfficiencyBetweenPeriods(
            "project1", period1Start, period1End, period2Start, period2End);
        
        assertThat(comparison).isNotNull();
        assertThat(comparison).hasSize(4); // 两个时期的评审数量和平均处理时间对比
    }
}