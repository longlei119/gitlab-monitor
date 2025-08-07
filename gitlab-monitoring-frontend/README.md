# GitLab监控系统前端

这是GitLab监控系统的React前端应用，提供直观的数据可视化界面。

## 功能特性

- 📊 **仪表板**: 项目概览和关键指标展示
- 👥 **团队概览**: 团队成员排行榜和统计
- 👤 **开发者详情**: 个人详细报告和趋势分析
- 🎯 **代码质量**: 代码质量分析和排行
- 🐛 **Bug分析**: Bug处理效率和统计
- ⚙️ **设置**: 系统配置管理

## 技术栈

- **React 18**: 前端框架
- **Ant Design**: UI组件库
- **ECharts**: 数据可视化
- **Axios**: HTTP客户端
- **React Router**: 路由管理

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm start
```

应用将在 http://localhost:3000 启动。

### 3. 构建生产版本

```bash
npm run build
```

## 项目结构

```
src/
├── components/          # 可复用组件
├── pages/              # 页面组件
│   ├── Dashboard.js    # 仪表板
│   ├── TeamOverview.js # 团队概览
│   ├── DeveloperDetail.js # 开发者详情
│   ├── CodeQuality.js  # 代码质量
│   ├── BugAnalysis.js  # Bug分析
│   └── Settings.js     # 设置
├── services/           # API服务
│   └── api.js         # API接口定义
├── App.js             # 主应用组件
├── index.js           # 应用入口
└── index.css          # 全局样式
```

## 页面说明

### 仪表板 (Dashboard)

- 项目概览统计
- 团队活动趋势图表
- 开发者排行榜
- 关键指标卡片

### 团队概览 (Team Overview)

- 团队成员详细排行表格
- 代码质量分布饼图
- 多维度排序功能
- 团队统计汇总

### 开发者详情 (Developer Detail)

- 个人详细报告
- 代码指标和Bug处理指标
- 提交趋势图表
- 代码分布分析

### 代码质量 (Code Quality)

- 代码质量排行图表
- 质量分布统计
- 详细排行表格
- 质量评分分析

### Bug分析 (Bug Analysis)

- Bug处理统计
- 解决效率排行
- Bug状态分布
- 效率评分分析

### 设置 (Settings)

- GitLab API配置
- 通知设置
- 监控阈值配置
- 报告配置

## API集成

前端通过 `src/services/api.js` 与后端API交互：

```javascript
// 获取仪表板概览
const overview = await getDashboardOverview(projectId, days);

// 获取开发者排行榜
const leaderboard = await getLeaderboard(projectId, days, sortBy);

// 获取开发者详情
const details = await getDeveloperDetails(userId, projectId, days);
```

## 自定义配置

### 环境变量

创建 `.env` 文件：

```env
REACT_APP_API_URL=http://localhost:8080/api
```

### 主题定制

在 `src/index.css` 中修改样式变量：

```css
:root {
  --primary-color: #1890ff;
  --success-color: #52c41a;
  --warning-color: #faad14;
  --error-color: #ff4d4f;
}
```

## 组件开发

### 创建新页面

1. 在 `src/pages/` 目录下创建新组件
2. 在 `App.js` 中添加路由
3. 在菜单中添加导航项

示例：

```javascript
// src/pages/NewPage.js
import React from 'react';
import { Card } from 'antd';

const NewPage = ({ projectId }) => {
  return (
    <div>
      <h2>新页面</h2>
      <Card>
        页面内容
      </Card>
    </div>
  );
};

export default NewPage;
```

### 添加新的API接口

在 `src/services/api.js` 中添加：

```javascript
export const getNewData = (projectId, params) => 
  api.get(`/new-endpoint?projectId=${projectId}`, { params });
```

### 创建图表组件

使用ECharts创建图表：

```javascript
import ReactECharts from 'echarts-for-react';

const MyChart = ({ data }) => {
  const option = {
    title: { text: '图表标题' },
    xAxis: { data: data.categories },
    yAxis: {},
    series: [{
      type: 'bar',
      data: data.values
    }]
  };

  return <ReactECharts option={option} />;
};
```

## 样式指南

### 颜色规范

- 主色调: `#1890ff` (蓝色)
- 成功色: `#52c41a` (绿色)
- 警告色: `#faad14` (橙色)
- 错误色: `#ff4d4f` (红色)

### 评分颜色

```javascript
const getScoreColor = (score) => {
  if (score >= 90) return '#52c41a';  // 优秀 - 绿色
  if (score >= 70) return '#1890ff';  // 良好 - 蓝色
  if (score >= 50) return '#faad14';  // 一般 - 橙色
  return '#ff4d4f';                   // 待改进 - 红色
};
```

## 部署

### 开发环境

```bash
npm start
```

### 生产环境

```bash
# 构建
npm run build

# 使用nginx部署
# 将build目录内容复制到nginx的html目录
```

### Docker部署

创建 `Dockerfile`：

```dockerfile
FROM node:16-alpine as build

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 性能优化

### 代码分割

使用React.lazy进行代码分割：

```javascript
const Dashboard = React.lazy(() => import('./pages/Dashboard'));
```

### 图表优化

- 使用ECharts的按需加载
- 合理设置图表刷新频率
- 数据量大时使用虚拟滚动

### 缓存策略

- 使用React Query或SWR进行数据缓存
- 合理设置API缓存时间
- 使用浏览器缓存

## 故障排除

### 常见问题

1. **API请求失败**
   - 检查后端服务是否启动
   - 验证API地址配置
   - 查看浏览器控制台错误

2. **图表不显示**
   - 检查数据格式是否正确
   - 验证ECharts配置
   - 查看控制台错误信息

3. **样式问题**
   - 检查CSS类名是否正确
   - 验证Ant Design版本兼容性
   - 清除浏览器缓存

### 调试技巧

1. 使用React Developer Tools
2. 在API调用处添加console.log
3. 使用浏览器网络面板检查请求

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 创建Pull Request

## 许可证

本项目基于MIT许可证开源。