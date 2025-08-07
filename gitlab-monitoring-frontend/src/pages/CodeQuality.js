import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Select, Spin, message, Progress, Table } from 'antd';
import ReactECharts from 'echarts-for-react';
import { getLeaderboard, getQualityDistribution } from '../services/api';

const { Option } = Select;

const CodeQuality = ({ projectId }) => {
  const [loading, setLoading] = useState(true);
  const [qualityData, setQualityData] = useState([]);
  const [distribution, setDistribution] = useState({});
  const [timeRange, setTimeRange] = useState(30);

  useEffect(() => {
    if (projectId) {
      loadQualityData();
    }
  }, [projectId, timeRange]);

  const loadQualityData = async () => {
    setLoading(true);
    try {
      const [leaderboardData, distributionData] = await Promise.all([
        getLeaderboard(projectId, timeRange, 'codequality'),
        getQualityDistribution(projectId, timeRange)
      ]);

      setQualityData(leaderboardData);
      setDistribution(distributionData);
    } catch (error) {
      message.error('加载代码质量数据失败');
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

  const getQualityTrendOption = () => {
    const developers = qualityData.slice(0, 10);
    return {
      title: {
        text: '代码质量排行',
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
      yAxis: {
        type: 'value',
        name: '质量评分',
        min: 0,
        max: 100
      },
      series: [
        {
          name: '代码质量评分',
          type: 'bar',
          data: developers.map(dev => ({
            value: dev.codeQualityScore,
            itemStyle: {
              color: getScoreColor(dev.codeQualityScore)
            }
          })),
          label: {
            show: true,
            position: 'top',
            formatter: '{c}'
          }
        }
      ]
    };
  };

  const getDistributionOption = () => {
    const data = distribution.distribution || {};
    return {
      title: {
        text: '质量分布统计',
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
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          data: [
            { 
              value: data.excellent || 0, 
              name: '优秀 (90-100分)', 
              itemStyle: { color: '#52c41a' } 
            },
            { 
              value: data.good || 0, 
              name: '良好 (70-89分)', 
              itemStyle: { color: '#1890ff' } 
            },
            { 
              value: data.average || 0, 
              name: '一般 (50-69分)', 
              itemStyle: { color: '#faad14' } 
            },
            { 
              value: data.poor || 0, 
              name: '待改进 (0-49分)', 
              itemStyle: { color: '#ff4d4f' } 
            }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: {
            show: false,
            position: 'center'
          },
          labelLine: {
            show: false
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
      title: '代码质量评分',
      dataIndex: 'codeQualityScore',
      key: 'codeQualityScore',
      sorter: (a, b) => a.codeQualityScore - b.codeQualityScore,
      render: (score) => (
        <div>
          <Progress
            percent={score}
            strokeColor={getScoreColor(score)}
            format={percent => `${percent}`}
          />
        </div>
      )
    },
    {
      title: '提交次数',
      dataIndex: 'commits',
      key: 'commits',
      sorter: (a, b) => a.commits - b.commits,
      render: (commits) => (
        <div style={{ textAlign: 'center', fontWeight: 'bold' }}>
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
        <div style={{ textAlign: 'center', color: '#52c41a', fontWeight: 'bold' }}>
          +{lines.toLocaleString()}
        </div>
      )
    },
    {
      title: '综合评分',
      dataIndex: 'overallScore',
      key: 'overallScore',
      sorter: (a, b) => a.overallScore - b.overallScore,
      render: (score) => (
        <div style={{ 
          textAlign: 'center', 
          color: getScoreColor(score), 
          fontWeight: 'bold',
          fontSize: 16
        }}>
          {score}
        </div>
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

  const avgQuality = qualityData.length > 0 
    ? qualityData.reduce((sum, dev) => sum + dev.codeQualityScore, 0) / qualityData.length 
    : 0;

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={18}>
          <h2>代码质量分析</h2>
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

      {/* 质量概览卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: getScoreColor(avgQuality) }}>
                {avgQuality.toFixed(1)}
              </div>
              <div className="metric-label">平均质量评分</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: '#52c41a' }}>
                {distribution.distribution?.excellent || 0}
              </div>
              <div className="metric-label">优秀开发者</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: '#ff4d4f' }}>
                {distribution.distribution?.poor || 0}
              </div>
              <div className="metric-label">待改进开发者</div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="dashboard-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: '#1890ff' }}>
                {distribution.totalDevelopers || 0}
              </div>
              <div className="metric-label">总开发者数</div>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {/* 质量排行图表 */}
        <Col span={16}>
          <Card title="代码质量排行" className="dashboard-card">
            <div className="chart-container">
              <ReactECharts option={getQualityTrendOption()} style={{ height: '100%' }} />
            </div>
          </Card>
        </Col>

        {/* 质量分布图 */}
        <Col span={8}>
          <Card title="质量分布" className="dashboard-card">
            <div className="chart-container">
              <ReactECharts option={getDistributionOption()} style={{ height: '100%' }} />
            </div>
          </Card>
        </Col>
      </Row>

      {/* 详细排行表格 */}
      <Row gutter={16}>
        <Col span={24}>
          <Card title="代码质量详细排行" className="dashboard-card">
            <Table
              columns={columns}
              dataSource={qualityData}
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

export default CodeQuality;