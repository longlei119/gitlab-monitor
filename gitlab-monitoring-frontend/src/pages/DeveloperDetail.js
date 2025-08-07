import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Row, Col, Card, Select, Spin, message, Progress, Tag, Descriptions, List } from 'antd';
import { UserOutlined, CodeOutlined, BugOutlined, TrophyOutlined } from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getDeveloperDetails, getLeaderboard } from '../services/api';

const { Option } = Select;

const DeveloperDetail = ({ projectId }) => {
  const { userId: paramUserId } = useParams();
  const [loading, setLoading] = useState(true);
  const [developerData, setDeveloperData] = useState({});
  const [developers, setDevelopers] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState(paramUserId || '');
  const [timeRange, setTimeRange] = useState(30);

  useEffect(() => {
    if (projectId) {
      loadDevelopers();
    }
  }, [projectId]);

  useEffect(() => {
    if (selectedUserId && projectId) {
      loadDeveloperData();
    }
  }, [selectedUserId, projectId, timeRange]);

  const loadDevelopers = async () => {
    try {
      const data = await getLeaderboard(projectId, 30, 'overall');
      setDevelopers(data);
      if (data.length > 0 && !selectedUserId) {
        setSelectedUserId(data[0].userId);
      }
    } catch (error) {
      message.error('加载开发者列表失败');
    }
  };

  const loadDeveloperData = async () => {
    setLoading(true);
    try {
      const data = await getDeveloperDetails(selectedUserId, projectId, timeRange);
      setDeveloperData(data);
    } catch (error) {
      message.error('加载开发者详情失败');
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

  const getCommitTrendOption = () => {
    const commitsByDate = developerData.codeMetrics?.commitsByDate || {};
    const dates = Object.keys(commitsByDate).sort();
    const commits = dates.map(date => commitsByDate[date]);

    return {
      title: {
        text: '提交趋势',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: dates
      },
      yAxis: {
        type: 'value',
        name: '提交数'
      },
      series: [
        {
          name: '提交数',
          type: 'line',
          data: commits,
          smooth: true,
          itemStyle: { color: '#1890ff' },
          areaStyle: { opacity: 0.3 }
        }
      ]
    };
  };

  const getFileTypeOption = () => {
    const linesByFileType = developerData.codeMetrics?.linesByFileType || {};
    const data = Object.entries(linesByFileType).map(([type, lines]) => ({
      name: type,
      value: lines
    }));

    return {
      title: {
        text: '代码分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      series: [
        {
          name: '代码行数',
          type: 'pie',
          radius: '60%',
          data: data,
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
        <Col span={8}>
          <h2>开发者详情</h2>
        </Col>
        <Col span={8}>
          <Select
            value={selectedUserId}
            onChange={setSelectedUserId}
            style={{ width: '100%' }}
            placeholder="选择开发者"
            showSearch
            filterOption={(input, option) =>
              option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }
          >
            {developers.map(dev => (
              <Option key={dev.userId} value={dev.userId}>
                {dev.username}
              </Option>
            ))}
          </Select>
        </Col>
        <Col span={8}>
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

      {/* 开发者基本信息 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="基本信息" className="developer-detail-card">
            <Descriptions column={4}>
              <Descriptions.Item label="用户名">{developerData.username}</Descriptions.Item>
              <Descriptions.Item label="邮箱">{developerData.email}</Descriptions.Item>
              <Descriptions.Item label="用户ID">{developerData.userId}</Descriptions.Item>
              <Descriptions.Item label="报告时间">
                {new Date(developerData.reportTime).toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>
      </Row>

      {/* 评分卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card className="developer-detail-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: getScoreColor(developerData.overallScore) }}>
                {developerData.overallScore}
              </div>
              <div className="metric-label">综合评分</div>
              <Progress
                percent={developerData.overallScore}
                strokeColor={getScoreColor(developerData.overallScore)}
                showInfo={false}
                className="score-progress"
              />
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="developer-detail-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: getScoreColor(developerData.activityScore) }}>
                {developerData.activityScore}
              </div>
              <div className="metric-label">活跃度评分</div>
              <Progress
                percent={developerData.activityScore}
                strokeColor={getScoreColor(developerData.activityScore)}
                showInfo={false}
                className="score-progress"
              />
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="developer-detail-card">
            <div className="metric-card">
              <div className="metric-value" style={{ color: getScoreColor(developerData.collaborationScore) }}>
                {developerData.collaborationScore}
              </div>
              <div className="metric-label">协作能力</div>
              <Progress
                percent={developerData.collaborationScore}
                strokeColor={getScoreColor(developerData.collaborationScore)}
                showInfo={false}
                className="score-progress"
              />
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card className="developer-detail-card">
            <div className="metric-card">
              <div style={{ display: 'flex', justifyContent: 'space-around', alignItems: 'center' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 14, color: '#666' }}>代码贡献</div>
                  <div style={{ fontSize: 18, fontWeight: 'bold', color: '#1890ff' }}>
                    #{developerData.codeContributionRank}
                  </div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 14, color: '#666' }}>代码质量</div>
                  <div style={{ fontSize: 18, fontWeight: 'bold', color: '#52c41a' }}>
                    #{developerData.codeQualityRank}
                  </div>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 14, color: '#666' }}>Bug解决</div>
                  <div style={{ fontSize: 18, fontWeight: 'bold', color: '#722ed1' }}>
                    #{developerData.bugResolutionRank}
                  </div>
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {/* 代码指标 */}
        <Col span={12}>
          <Card title="代码指标" className="developer-detail-card">
            <Row gutter={16}>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#1890ff' }}>
                    {developerData.codeMetrics?.commitCount || 0}
                  </div>
                  <div className="metric-label">提交次数</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#52c41a' }}>
                    +{(developerData.codeMetrics?.linesAdded || 0).toLocaleString()}
                  </div>
                  <div className="metric-label">新增代码行</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#ff4d4f' }}>
                    -{(developerData.codeMetrics?.linesDeleted || 0).toLocaleString()}
                  </div>
                  <div className="metric-label">删除代码行</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#722ed1' }}>
                    {developerData.codeMetrics?.filesChanged || 0}
                  </div>
                  <div className="metric-label">修改文件数</div>
                </div>
              </Col>
            </Row>
            <div style={{ marginTop: 16 }}>
              <div style={{ marginBottom: 8 }}>代码质量评分</div>
              <Progress
                percent={developerData.codeMetrics?.qualityScore || 0}
                strokeColor={getScoreColor(developerData.codeMetrics?.qualityScore || 0)}
              />
            </div>
          </Card>
        </Col>

        {/* Bug处理指标 */}
        <Col span={12}>
          <Card title="Bug处理指标" className="developer-detail-card">
            <Row gutter={16}>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#1890ff' }}>
                    {developerData.bugMetrics?.bugsCreated || 0}
                  </div>
                  <div className="metric-label">创建Bug数</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#52c41a' }}>
                    {developerData.bugMetrics?.bugsResolved || 0}
                  </div>
                  <div className="metric-label">解决Bug数</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#faad14' }}>
                    {(developerData.bugMetrics?.averageResolutionTime || 0).toFixed(1)}h
                  </div>
                  <div className="metric-label">平均解决时间</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metric-card">
                  <div className="metric-value" style={{ color: '#ff4d4f' }}>
                    {developerData.bugMetrics?.bugsReopened || 0}
                  </div>
                  <div className="metric-label">重新打开Bug</div>
                </div>
              </Col>
            </Row>
            <div style={{ marginTop: 16 }}>
              <div style={{ marginBottom: 8 }}>Bug处理效率</div>
              <Progress
                percent={developerData.bugMetrics?.efficiencyScore || 0}
                strokeColor={getScoreColor(developerData.bugMetrics?.efficiencyScore || 0)}
              />
            </div>
          </Card>
        </Col>
      </Row>

      {/* 图表 */}
      <Row gutter={16}>
        <Col span={12}>
          <Card title="提交趋势" className="developer-detail-card">
            <div className="chart-container">
              <ReactECharts option={getCommitTrendOption()} style={{ height: '100%' }} />
            </div>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="代码分布" className="developer-detail-card">
            <div className="chart-container">
              <ReactECharts option={getFileTypeOption()} style={{ height: '100%' }} />
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DeveloperDetail;