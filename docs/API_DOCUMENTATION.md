# GitLab研发度量系统API文档

## 概述

GitLab研发度量系统提供RESTful API接口，用于查询和管理研发效率相关的各种指标数据。所有API都需要JWT认证，并支持JSON格式的请求和响应。

## 基础信息

- **Base URL**: `https://your-domain.com/api`
- **认证方式**: JWT Bearer Token
- **内容类型**: `application/json`
- **字符编码**: UTF-8

## 认证

### 获取访问令牌

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "your_username",
  "password": "your_password"
}
```

**响应示例:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 86400
}
```

### 使用访问令牌

在所有API请求的Header中包含认证信息：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## API接口

### 1. Webhook接口

#### 1.1 接收GitLab Webhook

```http
POST /api/webhook/gitlab
Content-Type: application/json
X-Gitlab-Token: your-webhook-secret
X-Gitlab-Event: Push Hook
```

**说明**: 接收GitLab发送的Webhook事件，支持Push、Merge Request、Issue等事件类型。

### 2. 代码提交指标

#### 2.1 获取提交统计

```http
GET /api/metrics/commits?developerId={developerId}&projectId={projectId}&startDate={startDate}&endDate={endDate}
```

**参数说明:**
- `developerId` (可选): 开发者ID
- `projectId` (可选): 项目ID
- `startDate` (必需): 开始日期 (YYYY-MM-DD)
- `endDate` (必需): 结束日期 (YYYY-MM-DD)

**响应示例:**
```json
{
  "totalCommits": 156,
  "totalLinesAdded": 12450,
  "totalLinesDeleted": 3200,
  "totalFilesChanged": 890,
  "averageCommitsPerDay": 5.2,
  "topDevelopers": [
    {
      "developerId": "dev001",
      "developerName": "张三",
      "commits": 45,
      "linesAdded": 3200,
      "linesDeleted": 800
    }
  ],
  "dailyStats": [
    {
      "date": "2024-01-01",
      "commits": 8,
      "linesAdded": 450,
      "linesDeleted": 120
    }
  ]
}
```

#### 2.2 获取开发者提交详情

```http
GET /api/metrics/commits/developer/{developerId}?startDate={startDate}&endDate={endDate}
```

**响应示例:**
```json
{
  "developerId": "dev001",
  "developerName": "张三",
  "totalCommits": 45,
  "totalLinesAdded": 3200,
  "totalLinesDeleted": 800,
  "averageLinesPerCommit": 53.3,
  "commits": [
    {
      "commitSha": "abc123",
      "message": "修复用户登录问题",
      "timestamp": "2024-01-01T10:30:00Z",
      "linesAdded": 25,
      "linesDeleted": 10,
      "filesChanged": 3
    }
  ]
}
```

### 3. 代码质量指标

#### 3.1 获取质量指标

```http
GET /api/metrics/quality?projectId={projectId}&timeRange={timeRange}
```

**参数说明:**
- `projectId` (必需): 项目ID
- `timeRange` (可选): 时间范围 (7d, 30d, 90d, 默认30d)

**响应示例:**
```json
{
  "projectId": "project001",
  "currentMetrics": {
    "codeComplexity": 2.5,
    "duplicateRate": 3.2,
    "maintainabilityIndex": 85.6,
    "securityIssues": 2,
    "performanceIssues": 1,
    "codeSmells": 15
  },
  "trend": {
    "codeComplexityTrend": "improving",
    "duplicateRateTrend": "stable",
    "maintainabilityTrend": "improving"
  },
  "history": [
    {
      "date": "2024-01-01",
      "codeComplexity": 2.8,
      "duplicateRate": 3.5,
      "maintainabilityIndex": 82.1
    }
  ]
}
```

#### 3.2 获取安全问题详情

```http
GET /api/metrics/security?projectId={projectId}&severity={severity}
```

**参数说明:**
- `projectId` (必需): 项目ID
- `severity` (可选): 严重程度 (CRITICAL, HIGH, MEDIUM, LOW)

**响应示例:**
```json
{
  "totalIssues": 8,
  "criticalIssues": 1,
  "highIssues": 2,
  "mediumIssues": 3,
  "lowIssues": 2,
  "issues": [
    {
      "id": "security001",
      "severity": "HIGH",
      "type": "SQL_INJECTION",
      "description": "潜在的SQL注入漏洞",
      "file": "src/main/java/UserService.java",
      "line": 45,
      "createdAt": "2024-01-01T10:30:00Z"
    }
  ]
}
```

### 4. Bug修复效率

#### 4.1 获取Bug修复统计

```http
GET /api/metrics/bug-fix-efficiency?assigneeId={assigneeId}&projectId={projectId}&startDate={startDate}&endDate={endDate}
```

**响应示例:**
```json
{
  "totalBugs": 25,
  "fixedBugs": 20,
  "averageFixTime": 2.5,
  "averageResponseTime": 0.8,
  "fixRateByPriority": {
    "CRITICAL": {
      "total": 3,
      "fixed": 3,
      "averageFixTime": 0.5
    },
    "HIGH": {
      "total": 8,
      "fixed": 7,
      "averageFixTime": 1.2
    }
  },
  "topPerformers": [
    {
      "assigneeId": "dev001",
      "assigneeName": "张三",
      "bugsFixed": 8,
      "averageFixTime": 1.8
    }
  ]
}
```

#### 4.2 获取Bug详情

```http
GET /api/metrics/bugs/{bugId}
```

**响应示例:**
```json
{
  "id": "bug001",
  "title": "用户登录失败",
  "description": "用户无法正常登录系统",
  "severity": "HIGH",
  "status": "RESOLVED",
  "assigneeId": "dev001",
  "assigneeName": "张三",
  "createdAt": "2024-01-01T09:00:00Z",
  "assignedAt": "2024-01-01T09:30:00Z",
  "resolvedAt": "2024-01-01T15:30:00Z",
  "responseTime": 0.5,
  "fixTime": 6.0,
  "timeline": [
    {
      "status": "CREATED",
      "timestamp": "2024-01-01T09:00:00Z",
      "comment": "Bug创建"
    },
    {
      "status": "ASSIGNED",
      "timestamp": "2024-01-01T09:30:00Z",
      "comment": "分配给张三"
    }
  ]
}
```

### 5. 代码评审指标

#### 5.1 获取评审统计

```http
GET /api/metrics/code-review?projectId={projectId}&reviewerId={reviewerId}&startDate={startDate}&endDate={endDate}
```

**响应示例:**
```json
{
  "totalMergeRequests": 45,
  "reviewedMergeRequests": 42,
  "averageReviewTime": 4.2,
  "reviewCoverage": 93.3,
  "approvalRate": 85.7,
  "reviewerStats": [
    {
      "reviewerId": "reviewer001",
      "reviewerName": "李四",
      "reviewsCompleted": 15,
      "averageReviewTime": 3.8,
      "approvalRate": 80.0
    }
  ],
  "monthlyTrend": [
    {
      "month": "2024-01",
      "totalMRs": 45,
      "reviewCoverage": 93.3,
      "averageReviewTime": 4.2
    }
  ]
}
```

### 6. 测试覆盖率指标

#### 6.1 获取覆盖率统计

```http
GET /api/metrics/test-coverage?projectId={projectId}&branch={branch}
```

**响应示例:**
```json
{
  "projectId": "project001",
  "branch": "main",
  "currentCoverage": {
    "lineCoverage": 85.6,
    "branchCoverage": 78.9,
    "functionCoverage": 92.3,
    "totalLines": 12450,
    "coveredLines": 10653
  },
  "trend": {
    "lineCoverageTrend": "improving",
    "changeFromLastWeek": 2.3
  },
  "coverageByModule": [
    {
      "module": "user-service",
      "lineCoverage": 88.5,
      "branchCoverage": 82.1,
      "functionCoverage": 95.2
    }
  ],
  "history": [
    {
      "date": "2024-01-01",
      "lineCoverage": 83.2,
      "branchCoverage": 76.5,
      "functionCoverage": 90.1
    }
  ]
}
```

#### 6.2 获取覆盖率质量门禁状态

```http
GET /api/coverage/quality-gate?projectId={projectId}&commitSha={commitSha}
```

**响应示例:**
```json
{
  "projectId": "project001",
  "commitSha": "abc123",
  "status": "PASSED",
  "threshold": {
    "minLineCoverage": 80.0,
    "minBranchCoverage": 70.0,
    "minFunctionCoverage": 85.0
  },
  "actual": {
    "lineCoverage": 85.6,
    "branchCoverage": 78.9,
    "functionCoverage": 92.3
  },
  "canMerge": true,
  "message": "代码覆盖率满足要求，可以合并"
}
```

### 7. 效率看板

#### 7.1 获取综合效率看板

```http
GET /api/metrics/dashboard?teamId={teamId}&period={period}
```

**参数说明:**
- `teamId` (可选): 团队ID
- `period` (可选): 统计周期 (week, month, quarter, 默认month)

**响应示例:**
```json
{
  "period": "month",
  "teamId": "team001",
  "summary": {
    "totalCommits": 456,
    "totalBugsFixed": 89,
    "averageCodeQuality": 85.6,
    "testCoverage": 82.3,
    "reviewCoverage": 95.2
  },
  "trends": {
    "commitsTrend": "increasing",
    "qualityTrend": "stable",
    "coverageTrend": "improving"
  },
  "topPerformers": {
    "mostActiveCommitter": {
      "developerId": "dev001",
      "developerName": "张三",
      "commits": 78
    },
    "bestBugFixer": {
      "developerId": "dev002",
      "developerName": "李四",
      "bugsFixed": 25,
      "averageFixTime": 1.8
    }
  },
  "alerts": [
    {
      "type": "QUALITY_DECLINE",
      "message": "项目A的代码质量指标下降",
      "severity": "MEDIUM",
      "timestamp": "2024-01-01T10:30:00Z"
    }
  ]
}
```

### 8. 系统管理

#### 8.1 健康检查

```http
GET /actuator/health
```

**响应示例:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "6.2.6"
      }
    },
    "rabbitmq": {
      "status": "UP",
      "details": {
        "version": "3.11.0"
      }
    }
  }
}
```

#### 8.2 系统指标

```http
GET /actuator/metrics
```

#### 8.3 Prometheus指标

```http
GET /actuator/prometheus
```

## 错误处理

### 错误响应格式

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "请求参数验证失败",
    "details": [
      {
        "field": "startDate",
        "message": "开始日期不能为空"
      }
    ],
    "timestamp": "2024-01-01T10:30:00Z",
    "path": "/api/metrics/commits"
  }
}
```

### 常见错误码

| 错误码 | HTTP状态码 | 描述 |
|--------|------------|------|
| VALIDATION_ERROR | 400 | 请求参数验证失败 |
| UNAUTHORIZED | 401 | 未授权访问 |
| FORBIDDEN | 403 | 权限不足 |
| NOT_FOUND | 404 | 资源不存在 |
| RATE_LIMIT_EXCEEDED | 429 | 请求频率超限 |
| INTERNAL_ERROR | 500 | 服务器内部错误 |
| SERVICE_UNAVAILABLE | 503 | 服务不可用 |

## 限流规则

- **API接口**: 每分钟60次请求
- **Webhook接口**: 每分钟100次请求
- **认证接口**: 每分钟10次请求

## 数据格式说明

### 日期时间格式
- 日期: `YYYY-MM-DD` (如: 2024-01-01)
- 日期时间: `YYYY-MM-DDTHH:mm:ssZ` (如: 2024-01-01T10:30:00Z)

### 分页参数
```http
GET /api/metrics/commits?page=1&size=20&sort=timestamp,desc
```

### 分页响应格式
```json
{
  "content": [...],
  "pageable": {
    "page": 1,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8
  }
}
```

## SDK和示例

### JavaScript示例

```javascript
// 认证
const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'your_username',
    password: 'your_password'
  })
});

const { token } = await response.json();

// 获取提交统计
const metricsResponse = await fetch('/api/metrics/commits?startDate=2024-01-01&endDate=2024-01-31', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const metrics = await metricsResponse.json();
console.log(metrics);
```

### Python示例

```python
import requests
import json

# 认证
auth_response = requests.post('/api/auth/login', json={
    'username': 'your_username',
    'password': 'your_password'
})

token = auth_response.json()['token']

# 获取提交统计
headers = {'Authorization': f'Bearer {token}'}
metrics_response = requests.get('/api/metrics/commits', 
    params={
        'startDate': '2024-01-01',
        'endDate': '2024-01-31'
    },
    headers=headers
)

metrics = metrics_response.json()
print(json.dumps(metrics, indent=2))
```

## 版本信息

- **当前版本**: v1.0.0
- **API版本**: v1
- **最后更新**: 2024-01-01

## 联系方式

如有问题或建议，请联系开发团队：
- 邮箱: dev-team@company.com
- 文档: https://docs.company.com/gitlab-metrics
- 问题反馈: https://github.com/company/gitlab-metrics/issues