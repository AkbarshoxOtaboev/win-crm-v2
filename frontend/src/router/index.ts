import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    description?: string
    api?: string
    requiresAuth?: boolean
    guest?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { left: 0, top: 0 }
  },
  routes: [
    {
      path: '/signin',
      name: 'Signin',
      component: () => import('../views/Auth/Signin.vue'),
      meta: { title: 'Kirish', guest: true },
    },
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('../views/Ecommerce.vue'),
      meta: { title: 'Dashboard', requiresAuth: true },
    },
    {
      path: '/clients',
      name: 'Clients',
      component: () => import('../views/crm/ClientsView.vue'),
      meta: {
        title: 'Mijozlar',
        requiresAuth: true,
      },
    },
    {
      path: '/goods',
      name: 'Goods',
      component: () => import('../views/crm/GoodsView.vue'),
      meta: { title: 'Mahsulotlar', requiresAuth: true },
    },
    {
      path: '/warehouses',
      name: 'Warehouses',
      component: () => import('../views/crm/WarehousesView.vue'),
      meta: { title: 'Omborlar', requiresAuth: true },
    },
    {
      path: '/stock',
      name: 'Stock',
      component: () => import('../views/crm/StockView.vue'),
      meta: { title: 'Qoldiq', requiresAuth: true },
    },
    {
      path: '/sales',
      name: 'Sales',
      component: () => import('../views/crm/SalesView.vue'),
      meta: { title: 'Savdolar', requiresAuth: true },
    },
    {
      path: '/payments',
      name: 'Payments',
      component: () => import('../views/crm/PaymentsView.vue'),
      meta: { title: 'To‘lovlar', requiresAuth: true },
    },
    {
      path: '/suppliers',
      name: 'Suppliers',
      component: () => import('../views/crm/SuppliersView.vue'),
      meta: { title: 'Yetkazib beruvchilar', requiresAuth: true },
    },
    {
      path: '/expenses',
      name: 'Expenses',
      component: () => import('../views/crm/ExpensesView.vue'),
      meta: { title: 'Xarajatlar', requiresAuth: true },
    },
    {
      path: '/salary',
      name: 'Salary',
      component: () => import('../views/crm/SalaryView.vue'),
      meta: { title: 'Maosh', requiresAuth: true },
    },
    {
      path: '/users',
      name: 'Users',
      component: () => import('../views/crm/ModulePlaceholder.vue'),
      meta: {
        title: 'Foydalanuvchilar',
        description: 'Users, rollar va ruxsatlar.',
        api: '/api/users',
        requiresAuth: true,
      },
    },
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('../views/crm/ModulePlaceholder.vue'),
      meta: {
        title: 'Sozlamalar',
        description: 'Kompaniya, Telegram bot, Eskiz SMS.',
        api: '/api/telegram/bot-settings',
        requiresAuth: true,
      },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/Others/UserProfile.vue'),
      meta: { title: 'Profil', requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('../views/Errors/FourZeroFour.vue'),
      meta: { title: '404' },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const title = to.meta.title || 'WinCRM'
  document.title = `${title} | WinCRM`

  const auth = useAuthStore()
  const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)
  const guestOnly = to.matched.some((r) => r.meta.guest)

  if (requiresAuth && !auth.isAuthenticated) {
    next({ path: '/signin', query: { redirect: to.fullPath } })
    return
  }

  if (guestOnly && auth.isAuthenticated) {
    next({ path: '/' })
    return
  }

  next()
})

export default router
