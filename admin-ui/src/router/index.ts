import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { hideMenu: true },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Monitor' },
      },
      {
        path: 'system/user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', perms: 'system:user:list' },
      },
      {
        path: 'system/role',
        name: 'Role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar', perms: 'system:role:list' },
      },
      {
        path: 'system/menu',
        name: 'Menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu', perms: 'system:menu:list' },
      },
      {
        path: 'system/dept',
        name: 'Dept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', perms: 'system:dept:list' },
      },
      {
        path: 'system/dict',
        name: 'Dict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', icon: 'Notebook', perms: 'system:dict:list' },
      },
      {
        path: 'system/config',
        name: 'Config',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '参数设置', icon: 'Coin', perms: 'system:config:list' },
      },
      {
        path: 'monitor/online',
        name: 'Online',
        component: () => import('@/views/monitor/online/index.vue'),
        meta: { title: '在线用户', icon: 'Link', perms: 'monitor:online:list' },
      },
      {
        path: 'monitor/operlog',
        name: 'OperLog',
        component: () => import('@/views/monitor/operlog/index.vue'),
        meta: { title: '操作日志', icon: 'Document', perms: 'monitor:operlog:list' },
      },
      {
        path: 'monitor/loginlog',
        name: 'LoginLog',
        component: () => import('@/views/monitor/loginlog/index.vue'),
        meta: { title: '登录日志', icon: 'Lock', perms: 'monitor:loginlog:list' },
      },
      {
        path: 'monitor/jvm',
        name: 'MonitorJvm',
        component: () => import('@/views/monitor/jvm/index.vue'),
        meta: { title: 'JVM 监控', icon: 'Cpu', perms: 'monitor:jvm:list' },
      },
      {
        path: 'monitor/cache',
        name: 'MonitorCache',
        component: () => import('@/views/monitor/cache/index.vue'),
        meta: { title: 'Redis 缓存', icon: 'Coin', perms: 'monitor:cache:list' },
      },
      {
        path: 'monitor/api',
        name: 'MonitorApi',
        component: () => import('@/views/monitor/api/index.vue'),
        meta: { title: 'API 监控', icon: 'DataLine', perms: 'monitor:api:list' },
      },
      {
        path: 'monitor/database',
        name: 'MonitorDatabase',
        component: () => import('@/views/monitor/database/index.vue'),
        meta: { title: '数据库监控', icon: 'Coin', perms: 'monitor:database:list' },
      },
      {
        path: 'monitor/websocket',
        name: 'MonitorWebSocket',
        component: () => import('@/views/monitor/websocket/index.vue'),
        meta: { title: 'WebSocket 监控', icon: 'Link', perms: 'monitor:websocket:list' },
      },
      {
        path: 'monitor/nacos',
        name: 'MonitorNacos',
        component: () => import('@/views/monitor/nacos/index.vue'),
        meta: { title: 'Nacos 监控', icon: 'Connection', perms: 'monitor:nacos:list' },
      },

      {
        path: 'risk/config',
        name: 'RiskConfig',
        component: () => import('@/views/risk/config/index.vue'),
        meta: { title: '风控规则配置', icon: 'Setting', perms: 'risk:config:list' },
      },
      {
        path: 'risk/dashboard',
        name: 'RiskDashboard',
        component: () => import('@/views/risk/dashboard/index.vue'),
        meta: { title: '风控监控大屏', icon: 'Monitor', perms: 'risk:monitor:list' },
      },
      {
        path: 'risk/liquidation',
        name: 'RiskLiquidation',
        component: () => import('@/views/risk/liquidation/index.vue'),
        meta: { title: '强平记录', icon: 'Warning', perms: 'risk:liquidation:list' },
      },
      {
        path: 'risk/alert',
        name: 'RiskAlert',
        component: () => import('@/views/risk/alert/index.vue'),
        meta: { title: '异常报警', icon: 'AlarmClock', perms: 'risk:alert:list' },
      },
      {
        path: 'risk/report',
        name: 'RiskReport',
        component: () => import('@/views/risk/report/index.vue'),
        meta: { title: '风控报表', icon: 'DataLine', perms: 'risk:report:list' },
      },

      {
        path: 'finance/dashboard',
        name: 'FinanceDashboard',
        component: () => import('@/views/finance/dashboard/index.vue'),
        meta: { title: '财务看板', icon: 'Coin', perms: 'finance:dashboard:list' },
      },
      {
        path: 'finance/trade',
        name: 'FinanceTrade',
        component: () => import('@/views/finance/trade/index.vue'),
        meta: { title: '交易流水', icon: 'List', perms: 'finance:trade:list' },
      },
      {
        path: 'finance/report',
        name: 'FinanceReport',
        component: () => import('@/views/finance/report/index.vue'),
        meta: { title: '日报月报', icon: 'Document', perms: 'finance:report:list' },
      },
      {
        path: 'finance/reconciliation',
        name: 'FinanceReconciliation',
        component: () => import('@/views/finance/reconciliation/index.vue'),
        meta: { title: '对账管理', icon: 'Link', perms: 'finance:reconciliation:list' },
      },
      {
        path: 'finance/regulatory',
        name: 'FinanceRegulatory',
        component: () => import('@/views/finance/regulatory/index.vue'),
        meta: { title: '监管报表', icon: 'DataBoard', perms: 'finance:regulatory:list' },
      },
      {
        path: 'ops/service',
        name: 'OpsService',
        component: () => import('@/views/ops/service/index.vue'),
        meta: { title: '服务状态监控', icon: 'Monitor', perms: 'ops:service:list' },
      },
      {
        path: 'ops/release',
        name: 'OpsRelease',
        component: () => import('@/views/ops/release/index.vue'),
        meta: { title: '发布管理', icon: 'Upload', perms: 'ops:release:list' },
      },
      {
        path: 'ops/config',
        name: 'OpsConfig',
        component: () => import('@/views/ops/config/index.vue'),
        meta: { title: '配置管理', icon: 'Setting', perms: 'ops:config:list' },
      },
      {
        path: 'ops/log',
        name: 'OpsLog',
        component: () => import('@/views/ops/log/index.vue'),
        meta: { title: '日志查询', icon: 'Search', perms: 'ops:log:search' },
      },
      {
        path: 'ops/alert',
        name: 'OpsAlert',
        component: () => import('@/views/ops/alert/index.vue'),
        meta: { title: '告警管理', icon: 'Warning', perms: 'ops:alert:list' },
      },
      {
        path: 'ops/audit',
        name: 'OpsAudit',
        component: () => import('@/views/ops/audit/index.vue'),
        meta: { title: '审计日志', icon: 'Document', perms: 'ops:audit:list' },
      },
      {
        path: 'crm/customer',
        name: 'CrmCustomer',
        component: () => import('@/views/crm/customer/index.vue'),
        meta: { title: '客户信息管理', icon: 'User', perms: 'crm:customer:list' },
      },
      {
        path: 'crm/level',
        name: 'CrmLevel',
        component: () => import('@/views/crm/level/index.vue'),
        meta: { title: '客户等级管理', icon: 'Star', perms: 'crm:level:list' },
      },
      {
        path: 'crm/tag',
        name: 'CrmTag',
        component: () => import('@/views/crm/tag/index.vue'),
        meta: { title: '客户标签管理', icon: 'Collection', perms: 'crm:tag:list' },
      },
      {
        path: 'crm/communication',
        name: 'CrmCommunication',
        component: () => import('@/views/crm/communication/index.vue'),
        meta: { title: '客户沟通记录', icon: 'ChatDotSquare', perms: 'crm:communication:list' },
      },
      {
        path: 'crm/feedback',
        name: 'CrmFeedback',
        component: () => import('@/views/crm/feedback/index.vue'),
        meta: { title: '客户反馈处理', icon: 'Warning', perms: 'crm:feedback:list' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User', hideMenu: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫 — 未登录跳转登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin-token')
  if (to.path === '/login') {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router