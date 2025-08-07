import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Select, Spin, message, Progress, Table, Statistic } from 'antd';
import { BugOutlined, ClockCircleOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getBugStats, getLeaderboard } from '../services/api';

const { Option } = Select;

const BugAnalysis = ({ projectId }) => {
  const [loading, setLoading] = useState(true);
  const [bugStats, setBugStats] = useState({});
  const [bugLeaderboard, setBugLeaderboard] = useState([]);
  const [timeRange, setTimeRange] = useState(30);

  useEffect(() => {
    if (projectId) {
      loadBugData();
    }
  }, [projectId, timeRange]);

  const loadBugData = async () => {
    setLoading(true);
    try {
      const [statsData, leaderboardData] = await Promise.all([
        getBugStats(projectId, timeRange),
        getLeaderboard(projectId, timeRange, 'bugresolution')
      ]);

      setBugStats(statsData);
      setBugLeaderboard(leaderboardData);
    } catch (error) {
      message.error('加载Bug分析数据失败');
    } finally {
      setLoading(false);
    }
  };

  const getEfficiencyChartOption = () => {
    const developers = bugLeaderboard.slice(0, 10);
    return {
      title: {
        text: 'Bug解决效率排行',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      xAxis: {
        type: 'category',
        data: developers.map(dev => dev.username),
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: [
        {
          type: 'value',
          name: '解决数量',
          position: 'left'
        },
        {
          type: 'value',
          name: '效率评分',
          position: 'right',
          min: 0,
          max: 100
        }
      ],
      series: [
        {
          name: 'Bug解决数',
          type: 'bar',
          data: developers.map(dev => dev.bugsResolved),
          itemStyle: { color: '#1890ff' }
        },
        {
          name: '效率评分',
          type: 'line',
          yAxisIndex: 1,
          data: developers.map(dev => dev.bugEfficiencyScore || 0),
          itemStyle: { color: '#52c41a' },
          smooth: true
        }
      ]
    };
  };

  const getBugStatsChartOption = () => {
    return {
      title: {
        text: 'Bug处理统计',
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
          name: 'Bug数量',
          type: 'pie',
          radius: '50%',
          data: [
            { 
              value: bugStats.totalResolved || 0, 
              name: '已解决', 
              itemStyle: { color: '#52c41a' } 
            },
            { 
              value: (bugStats.totalCreated || 0) - (bugStats.totalResolved || 0), 
              name: '未解决', 
              itemStyle: { color: '#faad14' } 
            },
            { 
              value: bugStats.totalReopened || 0, 
              name: '重新打开', 
              itemStyle: { color: '#ff4d4f' } 
            }
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
      title: 'Bug解决数',
      dataIndex: 'bugsResolved',
      key: 'bugsResolved',
      sorter: (a, b) => a.bugsResolved - b.bugsResolved,
      render: (bugs) => (
        <div style={{ textAlign: 'center' }}>
          <BugOutlined style={{ color: '#52c41a', marginRight: 4 }} />
          <span style={{ fontWeight: 'bold' }}>{bugs}</span>
        </div>
      )
    },
    {
      title: '效率评分',
      dataIndex: 'bugEfficiencyScore',
      key: 'bugEfficiencyScore',
      sorter: (a, b) => (a.bugEfficiencyScore || 0) - (b.bugEfficiencyScore || 0),
      render: (score = 0) => (
        <Progress
          percent={score}
          strokeColor={score >= 80 ? '#52c41a' : score >= 60 ? '#1890ff' : '#ff4d4f'}
          format={percent => `${percent}`}
        />
      )
    },
    {
      title: '代码质量',
      dataIndex: 'codeQualityScore',
      key: 'codeQualityScore',
      sorter: (a, b) => a.codeQualityScore - b.codeQualityScore,
      render: (score) => (
        <div style={{ textAlign: 'center', fontWeight: 'bold' }}>
          {score}
        </div>
      )
    },
    {
      title: '综合评分',
      dataIndex: 'overallScore',
      key: 'overallScore',
      sorter: (a, b) => a.overallScore - b.overallScore,
      render: (score) => {
        const color = score >= 80 ? '#52c41a' : score >= 60 ? '#1890ff' : '#ff4d4f';
        return (
          <div style={{ 
            textAlign: 'center', 
            color, 
            fontWeight: 'bold',
            fontSize: 16
          }}>
            {score}
          </div>
        );
      }
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
        <Col span={18}>
          <h2>Bug分析</h2>
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

      {/* Bug统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="总创建Bug数"
              value={bugStats.totalCreated || 0}
              prefix={<BugOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="总解决Bug数"
              value={bugStats.totalResolved || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="重新打开Bug数"
              value={bugStats.totalReopened || 0}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <Statistic
              title="平均解决时间"
              value={bugStats.avgResolutionTime || 0}
              precision={1}
              suffix="小时"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ 
                color: (bugStats.avgResolutionTime || 0) <= 24 ? '#52c41a' : '#ff4d4f' 
              }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: '#52c41a' }}>
                {(bugStats.resolutionRate || 0).toFixed(1)}%
              </div>
              <div className="metric-label">Bug解决率</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ 
                color: (bugStats.reopenRate || 0) <= 10 ? '#52c41a' : '#ff4d4f' 
              }}>
                {(bugStats.reopenRate || 0).toFixed(1)}%
              </div>
              <div className="metric-label">Bug重开率</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: '#1890ff' }}>
                {bugLeaderboard.length}
              </div>
              <div className="metric-label">参与Bug处理人数</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: '#722ed1' }}>
                {bugLeaderboard.length > 0 
                  ? (bugLeaderboard.reduce((sum, dev) => sum + (dev.bugEfficiencyScore || 0), 0) / bugLeaderboard.length).toFixed(1)
                  : 0
                }
              </div>
              <div className="metric-label">平均效率评分</div>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {/* Bug解决效率图表 */}
        <Col span={16}>
          <Card title="Bug解决效率排行" className="dashboard-card">
            <div className="chart-container">
              <ReactECharts option={getEfficiencyChartOption()} style={{ height: '100%' }} />
            </div>
          </Card>
        </Col>

        {/* Bug统计饼图 */}
        <Col span={8}>
          <Card title="Bug处理统计" className="dashboard-card">
            <div className="chart-container">
              <ReactECharts option={getBugStatsChartOption()} style={{ height: '100%' }} />
            </div>
          </Card>
        </Col>
      </Row>

      {/* Bug处理排行表格 */}
      <Row gutter={16}>
        <Col span={24}>
          <Card title="Bug处理详细排行" className="dashboard-card">
            <Table
              columns={columns}
              dataSource={bugLeaderboard}
              rowKey="userId"
              pagination={{ pageSize: 15 }}
              size="middle"
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default BugAnalysis;