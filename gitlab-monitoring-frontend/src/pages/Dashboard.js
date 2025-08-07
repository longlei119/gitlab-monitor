import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Statistic, Select, Spin, message } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, TeamOutlined, CodeOutlined, BugOutlined, TrophyOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getDashboardOverview, getActivityTimeline, getLeaderboard } from '../services/api';

const { Option } = Select;

const Dashboard = ({ projectId }) => {
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState({});
  const [timeline, setTimeline] = useState([]);
  const [leaderboard, setLeaderboard] = useState([]);
  const [timeRange, setTimeRange] = useState(30);

  useEffect(() => {
    if (projectId) {
      loadDashboardData();
    }
  }, [projectId, timeRange]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const [overviewData, timelineData, leaderboardData] = await Promise.all([
        getDashboardOverview(projectId, timeRange),
        getActivityTimeline(projectId, Math.min(timeRange, 30)),
        getLeaderboard(projectId, timeRange, 'overall')
      ]);

      setOverview(overviewData);
      setTimeline(timelineData);
      setLeaderboard(leaderboardData);
    } catch (error) {
      message.error('加载仪表板数据失败');
    } finally {
      setLoading(false);
    }
  };

  const getTimelineChartOption = () => {
    return {
      title: {
        text: '团队活动趋势',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      legend: {
        data: ['提交数', '活跃用户'],
        top: 30
      },
      xAxis: {
        type: 'category',
        data: timeline.map(item => item.date)
      },
      yAxis: [
        {
          type: 'value',
          name: '提交数',
          position: 'left'
        },
        {
          type: 'value',
          name: '活跃用户',
          position: 'right'
        }
      ],
      series: [
        {
          name: '提交数',
          type: 'line',
          data: timeline.map(item => item.commits),
          smooth: true,
          itemStyle: { color: '#1890ff' }
        },
        {
          name: '活跃用户',
          type: 'bar',
          yAxisIndex: 1,
          data: timeline.map(item => item.activeUsers),
          itemStyle: { color: '#52c41a' }
        }
      ]
    };
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={18}>
          <h2>项目仪表板</h2>
        </Col>
        <Col span={6}>
          <Select
            value={timeRange}
            onChange={setTimeRange}
            style={{ width: '100%' }}
          >
            <Option value={7}>最近7天</Option>
            <Option value={30}>最近30天</Option>
            <Option value={90}>最近90天</Option>
          </Select>
        </Col>
      </Row>

      {/* 概览统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="团队规模"
              value={overview.teamSize || 0}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="总提交数"
              value={overview.totalCommits || 0}
              prefix={<CodeOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="代码行数"
              value={overview.totalLinesAdded || 0}
              prefix={<ArrowUpOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="Bug解决数"
              value={overview.totalBugsResolved || 0}
              prefix={<BugOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="平均代码质量"
              value={overview.averageQualityScore || 0}
              precision={1}
              suffix="分"
              valueStyle={{ color: overview.averageQualityScore >= 70 ? '#3f8600' : '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="项目健康度"
              value={overview.projectHealthScore || 0}
              precision={1}
              suffix="分"
              valueStyle={{ color: overview.projectHealthScore >= 70 ? '#3f8600' : '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="团队平均评分"
              value={overview.avgOverallScore || 0}
              precision={1}
              suffix="分"
              valueStyle={{ color: overview.avgOverallScore >= 70 ? '#3f8600' : '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="活跃度指数"
              value={85.6}
              precision={1}
              suffix="%"
              prefix={<ArrowUpOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        {/* 活动趋势图表 */}
        <Col span={16}>
          <Card title="团队活动趋势" className="dashboard-card">
            <div className="chart-container">
              <ReactECharts option={getTimelineChartOption()} style={{ height: '100%' }} />
            </div>
          </Card>
        </Col>

        {/* 排行榜 */}
        <Col span={8}>
          <Card title="开发者排行榜" className="dashboard-card">
            <div style={{ maxHeight: 400, overflowY: 'auto' }}>
              {leaderboard.slice(0, 10).map((developer, index) => (
                <div key={developer.userId} className="leaderboard-item">
                  <div className={`leaderboard-rank rank-${index + 1}`}>
                    {index + 1}
                  </div>
                  <div className="leaderboard-info">
                    <div className="leaderboard-name">{developer.username}</div>
                    <div className="leaderboard-stats">
                      {developer.commits}次提交 · {developer.bugsResolved}个Bug
                    </div>
                  </div>
                  <div className="leaderboard-score">
                    {developer.overallScore}
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;