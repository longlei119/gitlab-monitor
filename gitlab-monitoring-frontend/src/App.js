import React, { useState, useEffect } from 'react';
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Select, message } from 'antd';
import {
  DashboardOutlined,
  TeamOutlined,
  UserOutlined,
  BarChartOutlined,
  BugOutlined,
  SettingOutlined
} from '@ant-design/icons';
import Dashboard from './pages/Dashboard';
import TeamOverview from './pages/TeamOverview';
import DeveloperDetail from './pages/DeveloperDetail';
import CodeQuality from './pages/CodeQuality';
import BugAnalysis from './pages/BugAnalysis';
import Settings from './pages/Settings';
import { getProjects } from './services/api';

const { Header, Sider, Content } = Layout;
const { Option } = Select;

function App() {
  const [collapsed, setCollapsed] = useState(false);
  const [projects, setProjects] = useState([]);
  const [selectedProject, setSelectedProject] = useState('1');
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      const data = await getProjects();
      setProjects(data);
      if (data.length > 0 && !selectedProject) {
        setSelectedProject(data[0].id);
      }
    } catch (error) {
      message.error('加载项目列表失败');
    }
  };

  const menuItems = [
    {
      key: '/',
      icon: <DashboardOutlined />,
      label: '仪表板',
    },
    {
      key: '/team',
      icon: <TeamOutlined />,
      label: '团队概览',
    },
    {
      key: '/developer',
      icon: <UserOutlined />,
      label: '开发者详情',
    },
    {
      key: '/quality',
      icon: <BarChartOutlined />,
      label: '代码质量',
    },
    {
      key: '/bugs',
      icon: <BugOutlined />,
      label: 'Bug分析',
    },
    {
      key: '/settings',
      icon: <SettingOutlined />,
      label: '设置',
    },
  ];

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  const handleProjectChange = (projectId) => {
    setSelectedProject(projectId);
  };

  return (
    <Layout>
      <Sider 
        collapsible 
        collapsed={collapsed} 
        onCollapse={setCollapsed}
        theme="light"
      >
        <div className="logo">
          {collapsed ? 'GM' : 'GitLab监控'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <h2 style={{ margin: 0, color: '#001529' }}>GitLab监控系统</h2>
            <div>
              <span style={{ marginRight: 8 }}>项目:</span>
              <Select
                value={selectedProject}
                onChange={handleProjectChange}
                style={{ width: 200 }}
                placeholder="选择项目"
              >
                {projects.map(project => (
                  <Option key={project.id} value={project.id}>
                    {project.name}
                  </Option>
                ))}
              </Select>
            </div>
          </div>
        </Header>
        
        <Content>
          <Routes>
            <Route path="/" element={<Dashboard projectId={selectedProject} />} />
            <Route path="/team" element={<TeamOverview projectId={selectedProject} />} />
            <Route path="/developer" element={<DeveloperDetail projectId={selectedProject} />} />
            <Route path="/developer/:userId" element={<DeveloperDetail projectId={selectedProject} />} />
            <Route path="/quality" element={<CodeQuality projectId={selectedProject} />} />
            <Route path="/bugs" element={<BugAnalysis projectId={selectedProject} />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}

export default App;