import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

// 无需登录即可访问的白名单
const WHITE_LIST = ['/login']

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
      },
      {
        // 必须放在 list 之后
        path: ':id',
        name: 'LeadDetail',
        component: () => import('@/views/lead/detail.vue'),
        meta: { title: '线索详情', permissions: ['crm:lead:list'], hidden: true }
      }
    ]
  },
  {
    path: '/record',
    component: () => import('@/layout/index.vue'),
    meta: { title: '跟进中心', icon: 'BellFilled' },
    children: [
      {
        path: 'center',
        name: 'RecordCenter',
        component: () => import('@/views/record/center.vue'),
        meta: { title: '跟进中心', permissions: ['crm:record:center'] }
      }
    ]
  },
  {
    path: '/report',
    component: () => import('@/layout/index.vue'),
    meta: { title: '报表中心', icon: 'DataLine' },
    children: [
      {
        path: '',
        name: 'ReportCenter',
        component: () => import('@/views/report/ReportCenter.vue'),
        meta: { title: '报表中心', permissions: ['crm:report:view'] }
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
        name: 'CustomerPublicPool',
        component: () => import('@/views/customer/public-pool.vue'),
        meta: { title: '公海池', permissions: ['crm:customer:public_pool'] }
      },
      {
        // 必须放在 list/public 之后
        path: ':id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/detail.vue'),
        meta: { title: '客户详情', permissions: ['crm:customer:list'], hidden: true }
      }
    ]
  },
  {
    path: '/contact',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: 'list',
        name: 'ContactList',
        component: () => import('@/views/contact/list.vue'),
        meta: { title: '联系人', permissions: ['crm:contact:list'] }
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
      },
      {
        // 必须放在 list/funnel 之后,否则 :id 会捕获 /business/list
        path: ':id',
        name: 'BusinessDetail',
        component: () => import('@/views/business/detail.vue'),
        meta: { title: '商机详情', permissions: ['crm:business:list'], hidden: true }
      }
    ]
  },
  {
    path: '/product',
    component: () => import('@/layout/index.vue'),
    meta: { title: '产品管理', icon: 'Goods' },
    children: [
      {
        // v0.5:产品分类作为产品库页内的第 2 个 Tab,不再是独立路由
        path: 'list',
        name: 'ProductList',
        component: () => import('@/views/product/list.vue'),
        meta: { title: '产品库', permissions: ['crm:product:list'] }
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
        path: 'submit',
        name: 'ContractSubmit',
        component: () => import('@/views/contract/submit.vue'),
        meta: { title: '新建合同', permissions: ['crm:contract:edit'] }
      },
      {
        path: 'approval',
        name: 'ApprovalList',
        component: () => import('@/views/approval/list.vue'),
        meta: { title: '审批中心', permissions: ['crm:contract:approve'] }
      },
      {
        path: 'receivable',
        name: 'ReceivableList',
        component: () => import('@/views/receivable/list.vue'),
        meta: { title: '回款管理', permissions: ['crm:receivable:list'] }
      },
      {
        path: ':id',
        name: 'ContractDetail',
        component: () => import('@/views/contract/detail.vue'),
        meta: { title: '合同详情', permissions: ['crm:contract:list'], hidden: true }
      }
    ]
  },
  {
    path: '/system',
    component: () => import('@/layout/index.vue'),
    meta: { title: '系统设置', icon: 'Setting', roles: ['admin', 'sales_director'] },
    children: [
      {
        // v0.3:角色管理含 2 个 tab(用户管理 / 权限管理)
        path: 'role',
        name: 'RoleHome',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色', permissions: ['sys:user:list'] }
      },
      {
        // 必须放在 role 之后,否则 :id 会捕获 /system/role
        path: 'role/:id',
        name: 'RoleDetail',
        component: () => import('@/views/system/role/detail.vue'),
        meta: { title: '角色详情', permissions: ['sys:role:list'], hidden: true }
      },
      {
        // 阶段七 commit:部门管理(left tree + right detail)
        path: 'dept',
        name: 'DeptHome',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门', permissions: ['sys:dept:list'] }
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

/**
 * 全局路由守卫：登录态校验
 *
 * - 未登录访问任何应用路由 → 重定向到 /login，附 redirect query 让登录后跳回原页面
 * - 已登录访问 /login → 直接跳 /dashboard，避免重复登录
 * - 404 路由（/:pathMatch(.*)*）允许直接访问
 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const hasToken = !!userStore.token

  // 已登录访问登录页：直接跳主页
  if (to.path === '/login') {
    return hasToken ? next({ path: '/dashboard' }) : next()
  }

  // 白名单（除 /login 外的公开页面，如 404）直接放行
  if (WHITE_LIST.includes(to.path)) {
    return next()
  }

  // 需要登录的路由：没 token 跳登录页，附 redirect
  if (!hasToken) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  next()
})

export default router
