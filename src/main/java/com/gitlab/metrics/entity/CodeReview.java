package com.gitlab.metrics.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 代码评审实体类
 * 记录合并请求的代码评审信息
 */
@Entity
@Table(name = "code_reviews", indexes = {
    @Index(name = "idx_review_mr", columnList = "merge_request_id"),
    @Index(name = "idx_review_reviewer", columnList = "reviewerId"),
    @Index(name = "idx_review_timestamp", columnList = "reviewedAt")
})
public class CodeReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merge_request_id", nullable = false)
    @NotNull
    private MergeRequest mergeRequest;
    
    @Column(nullable = false, length = 100)
    @NotNull
    private String reviewerId;
    
    @Column(nullable = false, length = 255)
    @NotNull
    private String reviewerName;
    
    @Column(nullable = false)
    @NotNull
    private LocalDateTime reviewedAt;
    
    @Column(nullable = false, length = 30)
    @NotNull
    private String status; // approved, changes_requested, commented, dismissed
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column
    private Integer commentsCount;
    
    @Column
    private LocalDateTime submittedAt;
    
    @Column(length = 50)
    private String reviewType; // manual, automatic
    
    @Column
    private Boolean isRequired;
    
    // 默认构造函数
    public CodeReview() {}
    
    // 构造函数
    public CodeReview(MergeRequest mergeRequest, String reviewerId, String reviewerName, LocalDateTime reviewedAt, String status) {
        this.mergeRequest = mergeRequest;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewedAt = reviewedAt;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public MergeRequest getMergeRequest() {
        return mergeRequest;
    }
    
    public void setMergeRequest(MergeRequest mergeRequest) {
        this.mergeRequest = mergeRequest;
    }
    
    public String getReviewerId() {
        return reviewerId;
    }
    
    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }
    
    public String getReviewerName() {
        return reviewerName;
    }
    
    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
    
    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public Integer getCommentsCount() {
        return commentsCount;
    }
    
    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public String getReviewType() {
        return reviewType;
    }
    
    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }
    
    public Boolean getIsRequired() {
        return isRequired;
    }
    
    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
    
    @Override
    public String toString() {
        return "CodeReview{" +
                "id=" + id +
                ", reviewerId='" + reviewerId + '\'' +
                ", reviewerName='" + reviewerName + '\'' +
                ", reviewedAt=" + reviewedAt +
                ", status='" + status + '\'' +
                ", commentsCount=" + commentsCount +
                ", reviewType='" + reviewType + '\'' +
                ", isRequired=" + isRequired +
                '}';
    }
}