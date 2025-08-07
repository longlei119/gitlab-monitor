package com.gitlab.metrics.repository;

import com.gitlab.metrics.entity.Commit;
import com.gitlab.metrics.entity.FileChange;
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
 * FileChangeRepository单元测试
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class FileChangeRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private FileChangeRepository fileChangeRepository;
    
    private Commit testCommit1;
    private Commit testCommit2;
    private FileChange testFileChange1;
    private FileChange testFileChange2;
    private FileChange testFileChange3;
    private FileChange testFileChange4;
    
    @Before
    public void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        
        // 创建测试用的Commit
        testCommit1 = new Commit("abc123", "project1", "dev1", "Developer One", now);
        testCommit2 = new Commit("def456", "project1", "dev2", "Developer Two", yesterday);
        
        entityManager.persistAndFlush(testCommit1);
        entityManager.persistAndFlush(testCommit2);
        
        // 创建测试用的FileChange
        testFileChange1 = new FileChange(testCommit1, "src/main/java/Service.java", "modified");
        testFileChange1.setLinesAdded(50);
        testFileChange1.setLinesDeleted(10);
        
        testFileChange2 = new FileChange(testCommit1, "src/test/java/ServiceTest.java", "added");
        testFileChange2.setLinesAdded(100);
        testFileChange2.setLinesDeleted(0);
        
        testFileChange3 = new FileChange(testCommit2, "src/main/java/Service.java", "modified");
        testFileChange3.setLinesAdded(20);
        testFileChange3.setLinesDeleted(5);
        
        testFileChange4 = new FileChange(testCommit2, "README.md", "deleted");
        testFileChange4.setLinesAdded(0);
        testFileChange4.setLinesDeleted(30);
        
        entityManager.persistAndFlush(testFileChange1);
        entityManager.persistAndFlush(testFileChange2);
        entityManager.persistAndFlush(testFileChange3);
        entityManager.persistAndFlush(testFileChange4);
    }
    
    @Test
    public void testFindByCommitId() {
        List<FileChange> changes = fileChangeRepository.findByCommitId(testCommit1.getId());
        
        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(FileChange::getFilePath)
            .containsExactlyInAnyOrder("src/main/java/Service.java", "src/test/java/ServiceTest.java");
    }
    
    @Test
    public void testFindByFilePathOrderByCommitTimestampDesc() {
        List<FileChange> changes = fileChangeRepository.findByFilePathOrderByCommitTimestampDesc("src/main/java/Service.java");
        
        assertThat(changes).hasSize(2);
        // 应该按提交时间倒序排列
        assertThat(changes.get(0).getCommit().getCommitSha()).isEqualTo("abc123"); // 最新的在前
        assertThat(changes.get(1).getCommit().getCommitSha()).isEqualTo("def456");
    }
    
    @Test
    public void testFindByChangeTypeOrderByCommitTimestampDesc() {
        List<FileChange> changes = fileChangeRepository.findByChangeTypeOrderByCommitTimestampDesc("modified");
        
        assertThat(changes).hasSize(2);
        assertThat(changes.get(0).getCommit().getCommitSha()).isEqualTo("abc123"); // 最新的在前
    }
    
    @Test
    public void testGetFileChangeFrequency() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> frequency = fileChangeRepository.getFileChangeFrequency(start, end);
        
        assertThat(frequency).hasSize(3);
        
        // 验证Service.java的变更频率（变更次数最多）
        Object[] serviceStats = frequency.stream()
            .filter(stat -> "src/main/java/Service.java".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(serviceStats).isNotNull();
        assertThat(serviceStats[0]).isEqualTo("src/main/java/Service.java"); // filePath
        assertThat(serviceStats[1]).isEqualTo(2L); // 变更次数
        assertThat(serviceStats[2]).isEqualTo(70L); // 总新增行数 (50+20)
        assertThat(serviceStats[3]).isEqualTo(15L); // 总删除行数 (10+5)
    }
    
    @Test
    public void testGetFileChangeFrequencyByProject() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> frequency = fileChangeRepository.getFileChangeFrequencyByProject("project1", start, end);
        
        assertThat(frequency).hasSize(3);
        
        // 验证项目内的文件变更频率
        Object[] serviceStats = frequency.stream()
            .filter(stat -> "src/main/java/Service.java".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(serviceStats).isNotNull();
        assertThat(serviceStats[1]).isEqualTo(2L); // 变更次数
    }
    
    @Test
    public void testGetChangeTypeStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = fileChangeRepository.getChangeTypeStats("project1", start, end);
        
        assertThat(stats).hasSize(3); // modified, added, deleted
        
        // 验证modified类型统计
        Object[] modifiedStats = stats.stream()
            .filter(stat -> "modified".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(modifiedStats).isNotNull();
        assertThat(modifiedStats[1]).isEqualTo(2L); // modified数量
        
        // 验证added类型统计
        Object[] addedStats = stats.stream()
            .filter(stat -> "added".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(addedStats).isNotNull();
        assertThat(addedStats[1]).isEqualTo(1L); // added数量
        
        // 验证deleted类型统计
        Object[] deletedStats = stats.stream()
            .filter(stat -> "deleted".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(deletedStats).isNotNull();
        assertThat(deletedStats[1]).isEqualTo(1L); // deleted数量
    }
    
    @Test
    public void testGetHotspotFiles() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> hotspots = fileChangeRepository.getHotspotFiles("project1", 1, start, end);
        
        assertThat(hotspots).hasSize(1); // 只有Service.java变更次数>1
        
        Object[] hotspot = hotspots.get(0);
        assertThat(hotspot[0]).isEqualTo("src/main/java/Service.java"); // filePath
        assertThat(hotspot[1]).isEqualTo(2L); // changeCount
    }
    
    @Test
    public void testGetChangeStatsByFileExtension() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = fileChangeRepository.getChangeStatsByFileExtension("project1", start, end);
        
        assertThat(stats).hasSize(2); // java和md扩展名
        
        // 验证java文件统计
        Object[] javaStats = stats.stream()
            .filter(stat -> "java".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(javaStats).isNotNull();
        assertThat(javaStats[0]).isEqualTo("java"); // extension
        assertThat(javaStats[1]).isEqualTo(3L); // 变更次数
        assertThat(javaStats[2]).isEqualTo(170L); // 总新增行数
        assertThat(javaStats[3]).isEqualTo(15L); // 总删除行数
    }
    
    @Test
    public void testGetChangeStatsByDirectory() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> stats = fileChangeRepository.getChangeStatsByDirectory("project1", start, end);
        
        assertThat(stats).hasSize(1); // 只有src目录
        
        Object[] srcStats = stats.get(0);
        assertThat(srcStats[0]).isEqualTo("src"); // directory
        assertThat(srcStats[1]).isEqualTo(3L); // 变更次数
        assertThat(srcStats[2]).isEqualTo(170L); // 总新增行数
        assertThat(srcStats[3]).isEqualTo(15L); // 总删除行数
    }
    
    @Test
    public void testGetDeveloperContributionToFile() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> contributions = fileChangeRepository.getDeveloperContributionToFile("src/main/java/Service.java", start, end);
        
        assertThat(contributions).hasSize(2); // dev1和dev2都修改了这个文件
        
        // 验证dev1的贡献
        Object[] dev1Contribution = contributions.stream()
            .filter(stat -> "dev1".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(dev1Contribution).isNotNull();
        assertThat(dev1Contribution[0]).isEqualTo("dev1"); // developerId
        assertThat(dev1Contribution[1]).isEqualTo("Developer One"); // developerName
        assertThat(dev1Contribution[2]).isEqualTo(1L); // 变更次数
        assertThat(dev1Contribution[3]).isEqualTo(50L); // 新增行数
        assertThat(dev1Contribution[4]).isEqualTo(10L); // 删除行数
    }
    
    @Test
    public void testGetLargeFileChanges() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> large = fileChangeRepository.getLargeFileChanges("project1", 50, start, end);
        
        assertThat(large).hasSize(2); // ServiceTest.java(100行)和Service.java(60行)
        
        // 验证最大的变更在前
        Object[] largest = large.get(0);
        assertThat(largest[0]).isInstanceOf(FileChange.class);
        assertThat(largest[1]).isEqualTo(100L); // totalLines
        
        FileChange largestChange = (FileChange) largest[0];
        assertThat(largestChange.getFilePath()).isEqualTo("src/test/java/ServiceTest.java");
    }
    
    @Test
    public void testGetFileRenameOperations() {
        // 添加一个重命名操作
        FileChange renameChange = new FileChange(testCommit1, "src/main/java/NewService.java", "renamed");
        renameChange.setOldPath("src/main/java/OldService.java");
        entityManager.persistAndFlush(renameChange);
        
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> renames = fileChangeRepository.getFileRenameOperations("project1", start, end);
        
        assertThat(renames).hasSize(1);
        
        Object[] rename = renames.get(0);
        assertThat(rename[0]).isEqualTo("src/main/java/OldService.java"); // oldPath
        assertThat(rename[1]).isEqualTo("src/main/java/NewService.java"); // filePath
        assertThat(rename[2]).isEqualTo("dev1"); // developerId
    }
    
    @Test
    public void testGetNewFiles() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> newFiles = fileChangeRepository.getNewFiles("project1", start, end);
        
        assertThat(newFiles).hasSize(1);
        
        Object[] newFile = newFiles.get(0);
        assertThat(newFile[0]).isEqualTo("src/test/java/ServiceTest.java"); // filePath
        assertThat(newFile[1]).isEqualTo("dev1"); // developerId
        assertThat(newFile[2]).isEqualTo("Developer One"); // developerName
    }
    
    @Test
    public void testGetDeletedFiles() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> deletedFiles = fileChangeRepository.getDeletedFiles("project1", start, end);
        
        assertThat(deletedFiles).hasSize(1);
        
        Object[] deletedFile = deletedFiles.get(0);
        assertThat(deletedFile[0]).isEqualTo("README.md"); // filePath
        assertThat(deletedFile[1]).isEqualTo("dev2"); // developerId
        assertThat(deletedFile[2]).isEqualTo("Developer Two"); // developerName
    }
    
    @Test
    public void testGetFileChangeTrendByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> trend = fileChangeRepository.getFileChangeTrendByDate("project1", start, end);
        
        assertThat(trend).isNotEmpty();
        // 验证趋势数据包含日期和文件变更统计
        Object[] firstDay = trend.get(0);
        assertThat(firstDay).hasSize(5); // 日期 + 4个统计指标
    }
    
    @Test
    public void testGetUnchangedFiles() {
        // 添加一个早期的文件变更
        Commit oldCommit = new Commit("old123", "project1", "dev1", "Developer One", LocalDateTime.now().minusDays(10));
        entityManager.persistAndFlush(oldCommit);
        
        FileChange oldChange = new FileChange(oldCommit, "src/main/java/OldFile.java", "added");
        entityManager.persistAndFlush(oldChange);
        
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<String> unchanged = fileChangeRepository.getUnchangedFiles("project1", start, end);
        
        assertThat(unchanged).contains("src/main/java/OldFile.java");
    }
    
    @Test
    public void testGetAverageFileChangeSize() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> avgSizes = fileChangeRepository.getAverageFileChangeSize("project1", 1, start, end);
        
        assertThat(avgSizes).hasSize(3);
        
        // 验证Service.java的平均变更大小
        Object[] serviceAvg = avgSizes.stream()
            .filter(stat -> "src/main/java/Service.java".equals(stat[0]))
            .findFirst()
            .orElse(null);
        
        assertThat(serviceAvg).isNotNull();
        assertThat(serviceAvg[0]).isEqualTo("src/main/java/Service.java"); // filePath
        assertThat(serviceAvg[1]).isEqualTo(2L); // 变更次数
        assertThat(serviceAvg[2]).isEqualTo(42.5); // 平均变更大小 ((50+10)+(20+5))/2
    }
    
    @Test
    public void testGetMostCollaborativeFiles() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> collaborative = fileChangeRepository.getMostCollaborativeFiles("project1", 2, start, end);
        
        assertThat(collaborative).hasSize(1); // 只有Service.java被多个开发者修改
        
        Object[] mostCollaborative = collaborative.get(0);
        assertThat(mostCollaborative[0]).isEqualTo("src/main/java/Service.java"); // filePath
        assertThat(mostCollaborative[1]).isEqualTo(2L); // developerCount
        assertThat(mostCollaborative[2]).isEqualTo(2L); // changeCount
    }
}