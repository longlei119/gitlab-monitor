import React, { useState } from 'react';
import { Row, Col, Card, Form, Input, Switch, Button, message, Divider, InputNumber } from 'antd';
import { SettingOutlined, SaveOutlined } from '@ant-design/icons';

const Settings = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values) => {
    setLoading(true);
    try {
      // 这里应该调用API保存设置
      console.log('保存设置:', values);
      message.success('设置保存成功');
    } catch (error) {
      message.error('保存设置失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <h2>
            <SettingOutlined style={{ marginRight: 8 }} />
            系统设置
          </h2>
        </Col>
      </Row>

      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{
          gitlabUrl: 'https://gitlab.example.com/api/v4',
          accessToken: '',
          connectTimeout: 5000,
          readTimeout: 10000,
          maxRetries: 3,
          notificationsEnabled: true,
          emailEnabled: false,
          webhookEnabled: false,
          webhookUrl: '',
          qualityWarningThreshold: 60,
          qualityCriticalThreshold: 40,
          bugWarningThreshold: 72,
          bugCriticalThreshold: 168,
          reportsPath: './reports'
        }}
      >
        <Row gutter={16}>
          {/* GitLab API配置 */}
          <Col span={12}>
            <Card title="GitLab API配置" className="dashboard-card">
              <Form.Item
                label="GitLab API地址"
                name="gitlabUrl"
                rules={[{ required: true, message: '请输入GitLab API地址' }]}
              >
                <Input placeholder="https://gitlab.example.com/api/v4" />
              </Form.Item>

              <Form.Item
                label="访问令牌"
                name="accessToken"
                rules={[{ required: true, message: '请输入访问令牌' }]}
              >
                <Input.Password placeholder="输入GitLab访问令牌" />
              </Form.Item>

              <Row gutter={16}>
                <Col span={8}>
                  <Form.Item
                    label="连接超时(ms)"
                    name="connectTimeout"
                  >
                    <InputNumber min={1000} max={30000} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label="读取超时(ms)"
                    name="readTimeout"
                  >
                    <InputNumber min={1000} max={60000} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    label="最大重试次数"
                    name="maxRetries"
                  >
                    <InputNumber min={1} max={10} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
            </Card>
          </Col>

          {/* 通知配置 */}
          <Col span={12}>
            <Card title="通知配置" className="dashboard-card">
              <Form.Item
                label="启用通知"
                name="notificationsEnabled"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>

              <Divider />

              <Form.Item
                label="邮件通知"
                name="emailEnabled"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>

              <Form.Item
                label="Webhook通知"
                name="webhookEnabled"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>

              <Form.Item
                label="Webhook URL"
                name="webhookUrl"
              >
                <Input placeholder="https://your-webhook-url.com" />
              </Form.Item>
            </Card>
          </Col>
        </Row>

        <Row gutter={16} style={{ marginTop: 16 }}>
          {/* 监控阈值配置 */}
          <Col span={12}>
            <Card title="监控阈值配置" className="dashboard-card">
              <h4>代码质量阈值</h4>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="警告阈值"
                    name="qualityWarningThreshold"
                  >
                    <InputNumber min={0} max={100} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="严重警告阈值"
                    name="qualityCriticalThreshold"
                  >
                    <InputNumber min={0} max={100} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>

              <Divider />

              <h4>Bug解决时间阈值(小时)</h4>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="警告阈值"
                    name="bugWarningThreshold"
                  >
                    <InputNumber min={1} max={1000} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="严重警告阈值"
                    name="bugCriticalThreshold"
                  >
                    <InputNumber min={1} max={1000} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
            </Card>
          </Col>

          {/* 报告配置 */}
          <Col span={12}>
            <Card title="报告配置" className="dashboard-card">
              <Form.Item
                label="报告存储路径"
                name="reportsPath"
                rules={[{ required: true, message: '请输入报告存储路径' }]}
              >
                <Input placeholder="./reports" />
              </Form.Item>

              <div style={{ marginTop: 24 }}>
                <h4>定时任务说明</h4>
                <ul style={{ color: '#666', fontSize: 14 }}>
                  <li>日报：每天凌晨2点生成</li>
                  <li>周报：每周一凌晨3点生成</li>
                  <li>月报：每月1号凌晨4点生成</li>
                </ul>
              </div>

              <div style={{ marginTop: 24 }}>
                <h4>评分算法说明</h4>
                <ul style={{ color: '#666', fontSize: 14 }}>
                  <li>代码质量：提交消息(40%) + 提交大小(30%) + 提交频率(30%)</li>
                  <li>Bug效率：基于解决时间，24小时内满分</li>
                  <li>综合评分：代码质量(40%) + Bug处理(40%) + 贡献量(20%)</li>
                </ul>
              </div>
            </Card>
          </Col>
        </Row>

        <Row gutter={16} style={{ marginTop: 24 }}>
          <Col span={24}>
            <Card className="dashboard-card">
              <div style={{ textAlign: 'center' }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  size="large"
                  icon={<SaveOutlined />}
                >
                  保存设置
                </Button>
                <Button
                  style={{ marginLeft: 16 }}
                  size="large"
                  onClick={() => form.resetFields()}
                >
                  重置
                </Button>
              </div>
            </Card>
          </Col>
        </Row>
      </Form>
    </div>
  );
};

export default Settings;