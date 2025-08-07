package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.Commit;
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
 * CommitRepository单元测试
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class CommitRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommitRepository commitRepository;
    
    private Commit testCommit1;
    private Commit testCommit2;
    private Commit testCommit3;
    
    @Before
    public void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime lastWeek = now.minusDays(7);
        
        testCommit1 = new Commit("abc123", "project1", "dev1", "Developer One", now);
        testCommit1.setBranch("main");
        testCommit1.setLinesAdded(100);
        testCommit1.setLinesDeleted(20);
        testCommit1.setFilesChanged(5);
        testCommit1.setMessage("Add new feature");
        
        testCommit2 = new Commit("def456", "project1", "dev2", "Developer Two", yesterday);
        testCommit2.setBranch("develop");
        testCommit2.setLinesAdded(50);
        testCommit2.setLinesDeleted(10);
        testCommit2.setFilesChanged(3);
        testCommit2.setMessage("Fix bug");
        
        testCommit3 = new Commit("ghi789", "project2", "dev1", "Developer One", lastWeek);
        testCommit3.setBranch("main");
        testCommit3.setLinesAdded(200);
        testCommit3.setLinesDeleted(30);
        testCommit3.setFilesChanged(8);
        testCommit3.setMessage("Refactor code");
        
        entityManager.persistAndFlush(testCommit1);
        entityManager.persistAndFlush(testCommit2);
        entityManager.persistAndFlush(testCommit3);
    }
    
    @Test
    public void testFindByCommitSha() {
        Optional<Commit> found = commitRepository.findByCommitSha("abc123");
        
        assertThat(found).isPresent();
        assertThat(found.get().getDeveloperId()).isEqualTo("dev1");
        assertThat(found.get().getProjectId()).isEqualTo("project1");
    }
    
    @Test
    public void testFindByCommitSha_NotFound() {
        Optional<Commit> found = commitRepository.findByCommitSha("notexist");
        
        assertThat(found).isNotPresent();
    }
    
    @Test
    public void testFindByProjectIdOrderByTimestampDesc() {
        List<Commit> commits = commitRepository.findByProjectIdOrderByTimestampDesc("project1");
        
        assertThat(commits).hasSize(2);
        assertThat(commits.get(0).getCommitSha()).isEqualTo("abc123"); // 最新的在前
        assertThat(commits.get(1).getCommitSha()).isEqualTo("def456");
    }
    
    @Test
    public void testFindByDeveloperIdOrderByTimestampDesc() {
        List<Commit> commits = commitRepository.findByDeveloperIdOrderByTimestampDesc("dev1");
        
        assertThat(commits).hasSize(2);
        assertThat(commits.get(0).getCommitSha()).isEqualTo("abc123"); // 最新的在前
        assertThat(commits.get(1).getCommitSha()).isEqualTo("ghi789");
    }
    
    @Test
    public void testFindByDeveloperIdAndTimestampBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Commit> commits = commitRepository.findByDeveloperIdAndTimestampBetween("dev1", start, end);
        
        assertThat(commits).hasSize(1);
        assertThat(commits.get(0).getCommitSha()).isEqualTo("abc123");
    }
    
    @Test
    public void testFindByProjectIdAndTimestampBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Commit> commits = commitRepository.findByProjectIdAndTimestampBetween("project1", start, end);
        
        assertThat(commits).hasSize(2);
    }
    
    @Test
    public void testFindByProjectIdAndDeveloperIdAndTimestampBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Commit> commits = commitRepository.findByProjectIdAndDeveloperIdAndTimestampBetween(
            "project1", "dev1", start, end);
        
        assertThat(commits).hasSize(1);
        assertThat(commits.get(0).getCommitSha()).isEqualTo("abc123");
    }
    
    @Test
    public void testFindByBranchOrderByTimestampDesc() {
        List<Commit> commits = commitRepository.findByBranchOrderByTimestampDesc("main");
        
        assertThat(commits).hasSize(2);
        assertThat(commits.get(0).getCommitSha()).isEqualTo("abc123"); // 最新的在前
        assertThat(commits.get(1).getCommitSha()).isEqualTo("ghi789");
    }
    
    @Test
    public void testGetDeveloperCommitStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = commitRepository.getDeveloperCommitStats(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1的统计数据（2次提交）
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[0]).isEqualTo("dev1"); // developerId
        assertThat(dev1Stats[1]).isEqualTo("Developer One"); // developerName
        assertThat(dev1Stats[2]).isEqualTo(2L); // 提交次数
        assertThat(dev1Stats[3]).isEqualTo(300L); // 总新增行数
        assertThat(dev1Stats[4]).isEqualTo(50L); // 总删除行数
        assertThat(dev1Stats[5]).isEqualTo(13L); // 总修改文件数
    }
    
    @Test
    public void testGetProjectCommitStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = commitRepository.getProjectCommitStats(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证project1的统计数据
        Object[] project1Stats = stats.stream()
            .filter(stat -> "project1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(project1Stats).isNotNull();
        assertThat(project1Stats[0]).isEqualTo("project1"); // projectId
        assertThat(project1Stats[1]).isEqualTo(2L); // 提交次数
        assertThat(project1Stats[2]).isEqualTo(150L); // 总新增行数
        assertThat(project1Stats[3]).isEqualTo(30L); // 总删除行数
        assertThat(project1Stats[4]).isEqualTo(8L); // 总修改文件数
    }
    
    @Test
    public void testGetDeveloperCommitStatsByProject() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = commitRepository.getDeveloperCommitStatsByProject("project1", start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1在project1的统计数据
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[2]).isEqualTo(1L); // 在project1中的提交次数
    }
    
    @Test
    public void testFindLargeCommits() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Commit> largeCommits = commitRepository.findLargeCommits(100, start, end);
        
        assertThat(largeCommits).hasSize(2);
        // 应该按变更行数降序排列
        assertThat(largeCommits.get(0).getCommitSha()).isEqualTo("ghi789"); // 230行变更
        assertThat(largeCommits.get(1).getCommitSha()).isEqualTo("abc123"); // 120行变更
    }
    
    @Test
    public void testGetDeveloperAverageCommitSize() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = commitRepository.getDeveloperAverageCommitSize(start, end);
        
        assertThat(stats).hasSize(2);
        
        // 验证dev1的平均提交大小
        Object[] dev1Stats = stats.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Stats).isNotNull();
        assertThat(dev1Stats[2]).isEqualTo(175.0); // 平均变更行数 (120+230)/2
        assertThat(dev1Stats[3]).isEqualTo(6.5); // 平均修改文件数 (5+8)/2
    }
    
    @Test
    public void testGetProjectTotalStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        Object[] stats = commitRepository.getProjectTotalStats("project1", start, end);
        
        assertThat(stats).isNotNull();
        assertThat(stats[0]).isEqualTo(150L); // 总新增行数
        assertThat(stats[1]).isEqualTo(30L); // 总删除行数
        assertThat(stats[2]).isEqualTo(8L); // 总修改文件数
        assertThat(stats[3]).isEqualTo(2L); // 总提交次数
    }
    
    @Test
    public void testGetCommitTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = commitRepository.getCommitTrendByDate(start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证趋势数据包含日期、提交数、新增行数、删除行数
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(4);
    }
    
    @Test
    public void testGetBranchActivityStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(8);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = commitRepository.getBranchActivityStats("project1", start, end);
        
        assertThat(stats).hasSize(2); // main和develop分支
        
        // 验证main分支的统计
        Object[] mainStats = stats.stream()
            .filter(stat -> "main".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(mainStats).isNotNull();
        assertThat(mainStats[1]).isEqualTo(1L); // main分支的提交次数
    }
}