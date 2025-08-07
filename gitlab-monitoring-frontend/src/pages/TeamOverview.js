import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Table, Select, Spin, message, Tag, Progress } from 'antd';
import { UserOutlined, CodeOutlined, BugOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getLeaderboard, getQualityDistribution } from '../services/api';

const { Option } = Select;

const TeamOverview = ({ projectId }) => {
  const [loading, setLoading] = useState(true);
  const [teamData, setTeamData] = useState([]);
  const [qualityDistribution, setQualityDistribution] = useState({});
  const [timeRange, setTimeRange] = useState(30);
  const [sortBy, setSortBy] = useState('overall');

  useEffect(() => {
    if (projectId) {
      loadTeamData();
    }
  }, [projectId, timeRange, sortBy]);

  const loadTeamData = async () => {
    setLoading(true);
    try {
      const [leaderboardData, qualityData] = await Promise.all([
        getLeaderboard(projectId, timeRange, sortBy),
        getQualityDistribution(projectId, timeRange)
      ]);

      setTeamData(leaderboardData);
      setQualityDistribution(qualityData);
    } catch (error) {
      message.error('加载团队数据失败');
    } finally {
      setLoading(false);
    }
  };

  const getScoreColor = (score) => {
    if (score >= 90) return '#52c41a';
    if (score >= 70) return '#1890ff';
    if (score >= 50) return '#faad14';
    return '#ff4d4f';
  };

  const getScoreTag = (score) => {
    if (score >= 90) return <Tag color="green">优秀</Tag>;
    if (score >= 70) return <Tag color="blue">良好</Tag>;
    if (score >= 50) return <Tag color="orange">一般</Tag>;
    return <Tag color="red">待改进</Tag>;
  };

  const getQualityChartOption = () => {
    const data = qualityDistribution.distribution || {};
    return {
      title: {
        text: '代码质量分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle'
      },
      series: [
        {
          name: '开发者数量',
          type: 'pie',
          radius: '50%',
          data: [
            { value: data.excellent || 0, name: '优秀 (90-100分)', itemStyle: { color: '#52c41a' } },
            { value: data.good || 0, name: '良好 (70-89分)', itemStyle: { color: '#1890ff' } },
            { value: data.average || 0, name: '一般 (50-69分)', itemStyle: { color: '#faad14' } },
            { value: data.poor || 0, name: '待改进 (0-49分)', itemStyle: { color: '#ff4d4f' } }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    };
  };

  const columns = [
    {
      title: '排名',
      dataIndex: 'rank',
      key: 'rank',
      width: 60,
      render: (_, __, index) => {
        const rank = index + 1;
        let color = '#666';
        if (rank === 1) color = '#faad14';
        else if (rank === 2) color = '#a0a0a0';
        else if (rank === 3) color = '#cd7f32';
        
        return <span style={{ color, fontWeight: 'bold', fontSize: 16 }}>{rank}</span>;
      }
    },
    {
      title: '开发者',
      dataIndex: 'username',
      key: 'username',
      render: (username, record) => (
        <div>
          <div style={{ fontWeight: 'bold' }}>{username}</div>
          <div style={{ color: '#666', fontSize: 12 }}>{record.userId}</div>
        </div>
      )
    },
    {
      title: '综合评分',
      dataIndex: 'overallScore',
      key: 'overallScore',
      sorter: (a, b) => a.overallScore - b.overallScore,
      render: (score) => (
        <div>
          <div style={{ color: getScoreColor(score), fontWeight: 'bold', fontSize: 16 }}>
            {score}
          </div>
          {getScoreTag(score)}
        </div>
      )
    },
    {
      title: '代码质量',
      dataIndex: 'codeQualityScore',
      key: 'codeQualityScore',
      sorter: (a, b) => a.codeQualityScore - b.codeQualityScore,
      render: (score) => (
        <Progress
          percent={score}
          size="small"
          strokeColor={getScoreColor(score)}
          format={percent => `${percent}`}
        />
      )
    },
    {
      title: '提交数',
      dataIndex: 'commits',
      key: 'commits',
      sorter: (a, b) => a.commits - b.commits,
      render: (commits) => (
        <div style={{ textAlign: 'center' }}>
          <CodeOutlined style={{ color: '#1890ff', marginRight: 4 }} />
          {commits}
        </div>
      )
    },
    {
      title: '代码行数',
      dataIndex: 'linesAdded',
      key: 'linesAdded',
      sorter: (a, b) => a.linesAdded - b.linesAdded,
      render: (lines) => (
        <div style={{ textAlign: 'center', color: '#52c41a' }}>
          +{lines.toLocaleString()}
        </div>
      )
    },
    {
      title: 'Bug解决',
      dataIndex: 'bugsResolved',
      key: 'bugsResolved',
      sorter: (a, b) => a.bugsResolved - b.bugsResolved,
      render: (bugs) => (
        <div style={{ textAlign: 'center' }}>
          <BugOutlined style={{ color: '#722ed1', marginRight: 4 }} />
          {bugs}
        </div>
      )
    },
    {
      title: '活跃度',
      dataIndex: 'activityScore',
      key: 'activityScore',
      sorter: (a, b) => a.activityScore - b.activityScore,
      render: (score) => (
        <Progress
          percent={score}
          size="small"
          strokeColor="#52c41a"
          format={percent => `${percent}`}
        />
      )
    }
  ];

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
        <Col span={12}>
          <h2>团队概览</h2>
        </Col>
        <Col span={6}>
          <Select
            value={sortBy}
            onChange={setSortBy}
            style={{ width: '100%' }}
            placeholder="排序方式"
          >
            <Option value="overall">综合评分</Option>
            <Option value="codecontribution">代码贡献</Option>
            <Option value="codequality">代码质量</Option>
            <Option value="bugresolution">Bug解决</Option>
          </Select>
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

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {/* 团队统计卡片 */}
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value">{teamData.length}</div>
              <div className="metric-label">团队成员</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value">
                {teamData.reduce((sum, dev) => sum + dev.commits, 0)}
              </div>
              <div className="metric-label">总提交数</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value">
                {teamData.reduce((sum, dev) => sum + dev.linesAdded, 0).toLocaleString()}
              </div>
              <div className="metric-label">总代码行数</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value">
                {teamData.reduce((sum, dev) => sum + dev.bugsResolved, 0)}
              </div>
              <div className="metric-label">总Bug解决数</div>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        {/* 团队成员表格 */}
        <Col span={16}>
          <Card title="团队成员排行" className="dashboard-card">
            <Table
              columns={columns}
              dataSource={teamData}
              rowKey="userId"
              pagination={{ pageSize: 10 }}
              size="middle"
            />
          </Card>
        </Col>

        {/* 代码质量分布图 */}
        <Col span={8}>
          <Card title="代码质量分布" className="dashboard-card">
            <div className="chart-container">
              <ReactECharts option={getQualityChartOption()} style={{ height: '100%' }} />
            </div>
            <div style={{ marginTop: 16, textAlign: 'center' }}>
              <div style={{ color: '#666', fontSize: 12 }}>
                总开发者: {qualityDistribution.totalDevelopers || 0} 人
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default TeamOverview;