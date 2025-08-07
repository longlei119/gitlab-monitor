package com.gitlab.metrics.repository;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MergeRequestRepository单元测试
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class MergeRequestRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private MergeRequestRepository mergeRequestRepository;
    
    private MergeRequest testMR1;
    private MergeRequest testMR2;
    private MergeRequest testMR3;
    
    @Before
    public void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);
        
        testMR1 = new MergeRequest("1", "project1", "dev1", "Developer One", now, "opened", "feature/new", "main");
        testMR1.setTitle("Add new feature");
        testMR1.setAdditions(150);
        testMR1.setDeletions(20);
        testMR1.setChangedFiles(8);
        testMR1.setCommits(3);
        
        testMR2 = new MergeRequest("2", "project1", "dev2", "Developer Two", yesterday, "merged", "bugfix/issue", "main");
        testMR2.setTitle("Fix critical bug");
        testMR2.setMergedAt(yesterday.plusHours(2));
        testMR2.setAdditions(50);
        testMR2.setDeletions(10);
        testMR2.setChangedFiles(3);
        testMR2.setCommits(2);
        testMR2.setMergedById("dev3");
        testMR2.setMergedByName("Developer Three");
        
        testMR3 = new MergeRequest("3", "project2", "dev1", "Developer One", lastWeek, "closed", "feature/old", "main");
        testMR3.setTitle("Old feature");
        testMR3.setClosedAt(lastWeek.plusDays(1));
        testMR3.setAdditions(200);
        testMR3.setDeletions(50);
        testMR3.setChangedFiles(12);
        testMR3.setCommits(5);
        
        entityManager.persistAndFlush(testMR1);
        entityManager.persistAndFlush(testMR2);
        entityManager.persistAndFlush(testMR3);
    }
    
    @Test
    public void testFindByMrId() {
        Optional<MergeRequest> found = mergeRequestRepository.findByMrId("1");
        
        assertThat(found).isPresent();
        assertThat(found.get().getAuthorId()).isEqualTo("dev1");
        assertThat(found.get().getProjectId()).isEqualTo("project1");
    }
    
    @Test
    public void testFindByMrId_NotFound() {
        Optional<MergeRequest> found = mergeRequestRepository.findByMrId("999");
        
        assertThat(found).isNotPresent();
    }
    
    @Test
    public void testFindByProjectIdOrderByCreatedAtDesc() {
        List<MergeRequest> mrs = mergeRequestRepository.findByProjectIdOrderByCreatedAtDesc("project1");
        
        assertThat(mrs).hasSize(2);
        assertThat(mrs.get(0).getMrId()).isEqualTo("1"); // 最新的在前
        assertThat(mrs.get(1).getMrId()).isEqualTo("2");
    }
    
    @Test
    public void testFindByAuthorIdOrderByCreatedAtDesc() {
        List<MergeRequest> mrs = mergeRequestRepository.findByAuthorIdOrderByCreatedAtDesc("dev1");
        
        assertThat(mrs).hasSize(2);
        assertThat(mrs.get(0).getMrId()).isEqualTo("1"); // 最新的在前
        assertThat(mrs.get(1).getMrId()).isEqualTo("3");
    }
    
    @Test
    public void testFindByStatusOrderByCreatedAtDesc() {
        List<MergeRequest> mrs = mergeRequestRepository.findByStatusOrderByCreatedAtDesc("opened");
        
        assertThat(mrs).hasSize(1);
        assertThat(mrs.get(0).getMrId()).isEqualTo("1");
    }
    
    @Test
    public void testFindByProjectIdAndCreatedAtBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<MergeRequest> mrs = mergeRequestRepository.findByProjectIdAndCreatedAtBetween("project1", start, end);
        
        assertThat(mrs).hasSize(2);
    }
    
    @Test
    public void testFindByAuthorIdAndCreatedAtBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<MergeRequest> mrs = mergeRequestRepository.findByAuthorIdAndCreatedAtBetween("dev1", start, end);
        
        assertThat(mrs).hasSize(1);
        assertThat(mrs.get(0).getMrId()).isEqualTo("1");
    }
    
    @Test
    public void testGetDeveloperMergeRequestStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = mergeRequestRepository.getDeveloperMergeRequestStats(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1的统计数据
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[0]).isEqualTo("dev1"); // authorId
        assertThat(dev1Stats[1]).isEqualTo("Developer One"); // authorName
        assertThat(dev1Stats[2]).isEqualTo(1L); // MR总数
        assertThat(dev1Stats[3]).isEqualTo(0L); // 已合并数量
        assertThat(dev1Stats[4]).isNull(); // 平均合并时间（没有合并的MR）
    }
    
    @Test
    public void testGetProjectMergeRequestStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = mergeRequestRepository.getProjectMergeRequestStats(start, end);
        
        assertThat(stats).hasSize(1); // 只有project1在时间范围内
        
        Object[] project1Stats = stats.get(0);
        assertThat(project1Stats[0]).isEqualTo("project1"); // projectId
        assertThat(project1Stats[1]).isEqualTo(2L); // MR总数
        assertThat(project1Stats[2]).isEqualTo(1L); // 已合并数量
        assertThat(project1Stats[3]).isEqualTo(0L); // 已关闭数量
        assertThat(project1Stats[4]).isEqualTo(1L); // 开放数量
    }
    
    @Test
    public void testGetDeveloperMergeRequestStatsByProject() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = mergeRequestRepository.getDeveloperMergeRequestStatsByProject("project1", start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1在project1的统计数据
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[2]).isEqualTo(1L); // 在project1中的MR数量
    }
    
    @Test
    public void testGetMergeRequestTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = mergeRequestRepository.getMergeRequestTrendByDate(start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证趋势数据包含日期和MR统计
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 日期 + 3个统计指标
    }
    
    @Test
    public void testGetMergeRequestTrendByProjectAndDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = mergeRequestRepository.getMergeRequestTrendByProjectAndDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证项目趋势数据
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4); // 项目ID + 日期 + 2个统计指标
    }
    
    @Test
    public void testGetMergeRequestProcessingTimeStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = mergeRequestRepository.getMergeRequestProcessingTimeStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(2.0); // 平均处理时间（2小时）
        assertThat(stats[1]).isEqualTo(2L); // 最短处理时间
        assertThat(stats[2]).isEqualTo(2L); // 最长处理时间
    }
    
    @Test
    public void testFindLongPendingMergeRequests() {
        List<MergeRequest> pending = mergeRequestRepository.findLongPendingMergeRequests(1);
        
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getMrId()).isEqualTo("1");
        assertThat(pending.get(0).getStatus()).isEqualTo("opened");
    }
    
    @Test
    public void testFindLongPendingMergeRequestsByProject() {
        List<MergeRequest> pending = mergeRequestRepository.findLongPendingMergeRequestsByProject("project1", 1);
        
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getMrId()).isEqualTo("1");
    }
    
    @Test
    public void testFindLargeMergeRequests() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<MergeRequest> large = mergeRequestRepository.findLargeMergeRequests(100, start, end);
        
        assertThat(large).hasSize(1);
        assertThat(large.get(0).getMrId()).isEqualTo("1"); // 170行变更
    }
    
    @Test
    public void testGetDeveloperAverageMergeRequestSize() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = mergeRequestRepository.getDeveloperAverageMergeRequestSize(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1的平均MR大小
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[2]).isEqualTo(170.0); // 平均变更行数
        assertThat(dev1Stats[3]).isEqualTo(8.0); // 平均修改文件数
    }
    
    @Test
    public void testGetBranchMergeActivity() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> activity = mergeRequestRepository.getBranchMergeActivity("project1", start, end);
        
        assertThat(activity).hasSize(1); // 只有main分支
        
        Object[] mainActivity = activity.get(0);
        assertThat(mainActivity[0]).isEqualTo("main"); // targetBranch
        assertThat(mainActivity[1]).isEqualTo(2L); // MR总数
        assertThat(mainActivity[2]).isEqualTo(1L); // 已合并数量
    }
    
    @Test
    public void testGetMergeRequestStatusStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = mergeRequestRepository.getMergeRequestStatusStats("project1", start, end);
        
        assertThat(stats).hasSize(2); // opened和merged
        
        // 验证opened状态统计
        Object[] openedStats = stats.stream()
            .filter(stat -> "opened".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(openedStats).isNotNull();
        assertThat(openedStats[1]).isEqualTo(1L); // opened数量
        
        // 验证merged状态统计
        Object[] mergedStats = stats.stream()
            .filter(stat -> "merged".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(mergedStats).isNotNull();
        assertThat(mergedStats[1]).isEqualTo(1L); // merged数量
    }
    
    @Test
    public void testGetMostActiveMergers() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> mergers = mergeRequestRepository.getMostActiveMergers(start, end);
        
        assertThat(mergers).hasSize(1);
        
        Object[] topMerger = mergers.get(0);
        assertThat(topMerger[0]).isEqualTo("dev3"); // mergedById
        assertThat(topMerger[1]).isEqualTo("Developer Three"); // mergedByName
        assertThat(topMerger[2]).isEqualTo(1L); // 合并次数
    }
    
    @Test
    public void testCountWorkInProgressMergeRequests() {
        // 设置一个WIP的MR
        testMR1.setWorkInProgress(true);
        entityManager.persistAndFlush(testMR1);
        
        Long wipCount = mergeRequestRepository.countWorkInProgressMergeRequests("project1");
        
        assertThat(wipCount).isEqualTo(1L);
    }
    
    @Test
    public void testGetReviewCoverageStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = mergeRequestRepository.getReviewCoverageStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(2L); // 总MR数
        assertThat(stats[1]).isEqualTo(0L); // 有评审的MR数（测试数据中没有评审）
        assertThat(stats[2]).isEqualTo(0.0); // 平均评审数
    }
    
    @Test
    public void testCompareMergeRequestEfficiencyBetweenPeriods() {
        LocalDateTime period1Start = LocalDateTime.now().minusDays(2);
        LocalDateTime period1End = LocalDateTime.now().minusDays(1);
        LocalDateTime period2Start = LocalDateTime.now().minusHours(12);
        LocalDateTime period2End = LocalDateTime.now().plusDays(1);
        
        Object[] comparison = mergeRequestRepository.compareMergeRequestEfficiencyBetweenPeriods(
            "project1", period1Start, period1End, period2Start, period2End);
        
        assertThat(comparison).isNotNull();
        assertThat(comparison).hasSize(4); // 两个时期的MR数量和平均处理时间对比
    }
}