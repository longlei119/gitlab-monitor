import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

// 创建axios实例
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    console.error('API请求错误:', error);
    return Promise.reject(error);
  }
);

// 项目相关API
export const getProjects = () => api.get('/projects');
export const getProject = (projectId) => api.get(`/projects/${projectId}`);

// 仪表板相关API
export const getDashboardOverview = (projectId, days = 30) => 
  api.get(`/dashboard/overview?projectId=${projectId}&days=${days}`);

export const getLeaderboard = (projectId, days = 30, sortBy = 'overall') => 
  api.get(`/dashboard/leaderboard?projectId=${projectId}&days=${days}&sortBy=${sortBy}`);

export const getActivityTimeline = (projectId, days = 7) => 
  api.get(`/dashboard/timeline?projectId=${projectId}&days=${days}`);

export const getQualityDistribution = (projectId, days = 30) => 
  api.get(`/dashboard/quality-distribution?projectId=${projectId}&days=${days}`);

export const getBugStats = (projectId, days = 30) => 
  api.get(`/dashboard/bug-stats?projectId=${projectId}&days=${days}`);

export const getDeveloperDetails = (userId, projectId, days = 30) => 
  api.get(`/dashboard/developer/${userId}?projectId=${projectId}&days=${days}`);

// GitLab监控原始API
export const getRealtimeTeamMonitoring = (projectId, startDate, endDate) => 
  api.get(`/gitlab/monitoring/projects/${projectId}/team/realtime?startDate=${startDate}&endDate=${endDate}`);

export const getMonitorDeveloper = (userId, projectId, startDate, endDate) => 
  api.get(`/gitlab/monitoring/projects/${projectId}/developers/${userId}?startDate=${startDate}&endDate=${endDate}`);

export const getDailyReport = (projectId, date) => 
  api.get(`/gitlab/monitoring/projects/${projectId}/reports/daily/${date}`);

export const getWeeklyReport = (projectId, week) => 
  api.get(`/gitlab/monitoring/projects/${projectId}/reports/weekly/${week}`);

export const getMonthlyReport = (projectId, month) => 
  api.get(`/gitlab/monitoring/projects/${projectId}/reports/monthly/${month}`);

export const getProjectSummary = (projectId, period, date) => 
  api.get(`/gitlab/monitoring/projects/${projectId}/summary/${period}/${date}`);

export const getAvailableReportDates = (projectId, period) => 
  api.get(`/gitlab/monitoring/projects/${projectId}/reports/${period}/dates`);

export const generateReport = (projectId, reportType, startDate, endDate) => 
  api.post(`/gitlab/monitoring/projects/${projectId}/reports/${reportType}/generate?startDate=${startDate}&endDate=${endDate}`);

export default api;