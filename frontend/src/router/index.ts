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
      name: 'Home',
      component: () => import('../views/Ecommerce.vue'),
      meta: { title: 'Bosh sahifa', requiresAuth: true },
    },
    {
      path: '/clients',
      name: 'Clients',
      component: () => import('../views/crm/ClientsView.vue'),
      meta: { title: 'Mijozlar', requiresAuth: true },
    },
    {
      path: '/clients/:id',
      name: 'ClientDetail',
      component: () => import('../views/crm/ClientDetailView.vue'),
      meta: { title: 'Mijoz', requiresAuth: true },
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
      path: '/settings/warehouses',
      redirect: '/warehouses',
    },
    {
      path: '/warehouse-orders',
      name: 'WarehouseOrders',
      component: () => import('../views/crm/WarehouseOrdersView.vue'),
      meta: { title: 'Kirim', requiresAuth: true },
    },
    {
      path: '/warehouse-orders/create',
      name: 'WarehouseOrderCreate',
      component: () => import('../views/crm/WarehouseOrderCreateView.vue'),
      meta: { title: 'Yangi kirim', requiresAuth: true },
    },
    {
      path: '/warehouse-orders/:id/edit',
      name: 'WarehouseOrderEdit',
      component: () => import('../views/crm/WarehouseOrderCreateView.vue'),
      meta: { title: 'Kirimni tahrirlash', requiresAuth: true },
    },
    {
      path: '/stock',
      name: 'Stock',
      component: () => import('../views/crm/StockView.vue'),
      meta: { title: 'Qoldiq', requiresAuth: true },
    },
    {
      path: '/stock/transfers',
      name: 'StockTransfers',
      component: () => import('../views/crm/StockTransfersView.vue'),
      meta: { title: 'Transferlar', requiresAuth: true },
    },
    {
      path: '/stock/history',
      name: 'StockHistory',
      component: () => import('../views/crm/StockHistoryView.vue'),
      meta: { title: 'Tarix', requiresAuth: true },
    },
    {
      path: '/sales',
      name: 'Sales',
      component: () => import('../views/crm/SalesView.vue'),
      meta: { title: 'Savdolar', requiresAuth: true },
    },
    {
      path: '/sales/create',
      name: 'SaleOrderCreate',
      component: () => import('../views/crm/SaleOrderCreateView.vue'),
      meta: { title: 'Yangi savdo', requiresAuth: true },
    },
    {
      path: '/sales/:id',
      name: 'SaleOrderDetail',
      component: () => import('../views/crm/SaleOrderDetailView.vue'),
      meta: { title: 'Savdo', requiresAuth: true },
    },
    {
      path: '/inventory',
      name: 'Inventory',
      component: () => import('../views/crm/InventoryView.vue'),
      meta: { title: 'Inventarizatsiya', requiresAuth: true },
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
      path: '/suppliers/balances',
      name: 'SuppliersBalances',
      component: () => import('../views/crm/SuppliersBalanceView.vue'),
      meta: { title: 'Yetkazib beruvchilar balansi', requiresAuth: true },
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
      redirect: '/settings/users',
    },
    {
      path: '/settings',
      name: 'SettingsGeneral',
      component: () => import('../views/crm/settings/GeneralSettingsView.vue'),
      meta: { title: 'Umumiy sozlamalar', requiresAuth: true },
    },
    {
      path: '/settings/users',
      name: 'SettingsUsers',
      component: () => import('../views/crm/UsersView.vue'),
      meta: { title: 'Foydalanuvchilar', requiresAuth: true },
    },
    {
      path: '/settings/sessions',
      name: 'SettingsSessions',
      component: () => import('../views/crm/settings/SessionsSettingsView.vue'),
      meta: { title: 'Faol sessiyalar', requiresAuth: true },
    },
    {
      path: '/settings/roles',
      name: 'SettingsRoles',
      component: () => import('../views/crm/settings/RolesSettingsView.vue'),
      meta: { title: 'Rollar', requiresAuth: true },
    },
    {
      path: '/settings/company',
      name: 'SettingsCompany',
      component: () => import('../views/crm/settings/CompanySettingsView.vue'),
      meta: { title: 'Tashkilot rekvizitlari', requiresAuth: true },
    },
    {
      path: '/settings/telegram',
      name: 'SettingsTelegram',
      component: () => import('../views/crm/settings/TelegramSettingsView.vue'),
      meta: { title: 'Telegram bot', requiresAuth: true },
    },
    {
      path: '/settings/eskiz',
      name: 'SettingsEskiz',
      component: () => import('../views/crm/settings/EskizSettingsView.vue'),
      meta: { title: 'Eskiz.uz SMS', requiresAuth: true },
    },
    {
      path: '/settings/units',
      name: 'SettingsUnits',
      component: () => import('../views/crm/settings/UnitsSettingsView.vue'),
      meta: { title: 'O‘lchov birliklari', requiresAuth: true },
    },
    {
      path: '/settings/payment-types',
      name: 'SettingsPaymentTypes',
      component: () => import('../views/crm/settings/PaymentTypesSettingsView.vue'),
      meta: { title: 'To‘lov turlari', requiresAuth: true },
    },
    {
      path: '/settings/expense-categories',
      name: 'SettingsExpenseCategories',
      component: () => import('../views/crm/settings/ExpenseCategoriesSettingsView.vue'),
      meta: { title: 'Xarajat kategoriyalari', requiresAuth: true },
    },
    {
      path: '/settings/audit',
      name: 'SettingsAudit',
      component: () => import('../views/crm/settings/AuditSettingsView.vue'),
      meta: { title: 'Audit loglar', requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/Others/UserProfile.vue'),
      meta: { title: 'Profil', requiresAuth: true },
    },
    {
      path: '/profile/password',
      name: 'ChangePassword',
      component: () => import('../views/Others/ChangePasswordView.vue'),
      meta: { title: 'Parolni yangilash', requiresAuth: true },
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
