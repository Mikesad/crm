import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 路由配置
 * - hidden: 是否在侧边栏隐藏
 * - alwaysShow: 总是显示根路由
 * - meta.title / icon / roles / permissions 控制菜单与权限
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
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
        meta: { title: '首页', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/lead',
    component: () => import('@/layout/index.vue'),
    meta: { title: '线索管理', icon: 'Aim' },
    children: [
      {
        path: 'list',
        name: 'LeadList',
        component: () => import('@/views/lead/list.vue'),
        meta: { title: '线索列表', permissions: ['crm:lead:list'] }
      }
    ]
  },
  {
    path: '/customer',
    component: () => import('@/layout/index.vue'),
    meta: { title: '客户管理', icon: 'User' },
    children: [
      {
        path: 'list',
        name: 'CustomerList',
        component: () => import('@/views/customer/list.vue'),
        meta: { title: '客户列表', permissions: ['crm:customer:list'] }
      },
      {
        path: 'public',
        name: 'CustomerPublic',
        component: () => import('@/views/customer/public.vue'),
        meta: { title: '公海池', permissions: ['crm:customer:public'] }
      }
    ]
  },
  {
    path: '/business',
    component: () => import('@/layout/index.vue'),
    meta: { title: '商机管理', icon: 'TrendCharts' },
    children: [
      {
        path: 'list',
        name: 'BusinessList',
        component: () => import('@/views/business/list.vue'),
        meta: { title: '商机列表', permissions: ['crm:business:list'] }
      },
      {
        path: 'funnel',
        name: 'BusinessFunnel',
        component: () => import('@/views/business/funnel.vue'),
        meta: { title: '销售漏斗', permissions: ['crm:business:funnel'] }
      }
    ]
  },
  {
    path: '/contract',
    component: () => import('@/layout/index.vue'),
    meta: { title: '合同与回款', icon: 'Document' },
    children: [
      {
        path: 'list',
        name: 'ContractList',
        component: () => import('@/views/contract/list.vue'),
        meta: { title: '合同列表', permissions: ['crm:contract:list'] }
      },
      {
        path: 'receivable',
        name: 'ReceivableList',
        component: () => import('@/views/contract/receivable.vue'),
        meta: { title: '回款管理', permissions: ['crm:contract:receivable'] }
      }
    ]
  },
  {
    path: '/system',
    component: () => import('@/layout/index.vue'),
    meta: { title: '系统管理', icon: 'Setting', roles: ['admin'] },
    children: [
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', permissions: ['sys:user:list'] }
      },
      {
        path: 'role',
        name: 'RoleList',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理', permissions: ['sys:role:list'] }
      },
      {
        path: 'menu',
        name: 'MenuList',
        component: () => import('@/views/system/menu.vue'),
        meta: { title: '菜单权限', permissions: ['sys:menu:list'] }
      },
      {
        path: 'dept',
        name: 'DeptList',
        component: () => import('@/views/system/dept.vue'),
        meta: { title: '部门管理', permissions: ['sys:dept:list'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
