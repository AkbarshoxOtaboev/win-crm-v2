import { watch } from 'vue'
import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import i18n from '@/i18n'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    description?: string
    api?: string
    requiresAuth?: boolean
    guest?: boolean
    /** Faqat SUPER_ADMIN yoki ADMIN */
    settingsAdmin?: boolean
    /** Faqat DIRECTOR (xodimlar) */
    employeesOnly?: boolean
    /** SUPER_ADMIN / ADMIN / DIRECTOR yoki TRANSPORT_MANAGER */
    transport?: boolean
  }
}

const TRANSPORT_HOME = '/transport/dashboard'

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
      path: '/workshops',
      name: 'Workshops',
      component: () => import('../views/crm/WorkshopsView.vue'),
      meta: { title: 'Sexlar', requiresAuth: true },
    },
    {
      path: '/settings/workshops',
      redirect: '/workshops',
    },
    {
      path: '/production',
      name: 'ProductionOrders',
      component: () => import('../views/crm/ProductionOrdersView.vue'),
      meta: { title: 'Ishlab chiqarish', requiresAuth: true },
    },
    {
      path: '/production/dashboard',
      name: 'ProductionDashboard',
      component: () => import('../views/crm/ProductionDashboardView.vue'),
      meta: { title: 'Dashboard', requiresAuth: true },
    },
    {
      path: '/production/board',
      name: 'ProductionBoard',
      component: () => import('../views/crm/ProductionBoardView.vue'),
      meta: { title: 'Ishlab chiqarish', requiresAuth: true },
    },
    {
      path: '/transport',
      redirect: TRANSPORT_HOME,
    },
    {
      path: '/transport/dashboard',
      name: 'TransportDashboard',
      component: () => import('../views/crm/transport/TransportDashboardView.vue'),
      meta: { title: 'Transport dashboard', requiresAuth: true, transport: true },
    },
    {
      path: '/transport/orders',
      name: 'TransportOrders',
      component: () => import('../views/crm/transport/TransportOrdersView.vue'),
      meta: { title: 'Transport buyurtmalari', requiresAuth: true, transport: true },
    },
    {
      path: '/transport/drivers',
      name: 'TransportDrivers',
      component: () => import('../views/crm/transport/TransportDriversView.vue'),
      meta: { title: 'Yetkazib beruvchilar', requiresAuth: true, transport: true },
    },
    {
      path: '/transport/workers',
      name: 'TransportWorkers',
      component: () => import('../views/crm/transport/TransportWorkersView.vue'),
      meta: { title: 'Ishchilar', requiresAuth: true, transport: true },
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
      meta: { title: 'Sotuv buyurtmalari', requiresAuth: true },
    },
    {
      path: '/sales/dashboard',
      name: 'SalesDashboard',
      component: () => import('../views/crm/SalesDashboardView.vue'),
      meta: { title: 'Sotuv dashboard', requiresAuth: true },
    },
    {
      path: '/sales/report',
      name: 'SalesReport',
      component: () => import('../views/crm/SalesReportView.vue'),
      meta: { title: 'Sotuv hisoboti', requiresAuth: true },
    },
    {
      path: '/currency',
      name: 'Currency',
      component: () => import('../views/crm/CurrencyView.vue'),
      meta: { title: 'Valyuta', requiresAuth: true },
    },
    {
      path: '/sales/fx-difference',
      name: 'FxDifference',
      component: () => import('../views/crm/FxDifferenceView.vue'),
      meta: { title: 'Kurs farqi', requiresAuth: true },
    },
    {
      path: '/sales/debts',
      name: 'SellerDebts',
      component: () => import('../views/crm/SellerDebtsView.vue'),
      meta: { title: 'Qarzdorlar', requiresAuth: true },
    },
    {
      path: '/sales/wastes',
      name: 'SalesWastes',
      component: () => import('../views/crm/SalesWastesView.vue'),
      meta: { title: 'Ortiqcha material', requiresAuth: true },
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
      path: '/payments/dashboard',
      name: 'PaymentsDashboard',
      component: () => import('../views/crm/PaymentsDashboardView.vue'),
      meta: { title: 'Tushum dashboard', requiresAuth: true },
    },
    {
      path: '/services',
      name: 'Services',
      component: () => import('../views/crm/ServicesView.vue'),
      meta: { title: 'Xizmatlar', requiresAuth: true },
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
      path: '/kpi',
      name: 'Kpi',
      component: () => import('../views/crm/KpiView.vue'),
      meta: { title: 'KPI', requiresAuth: true },
    },
    {
      path: '/cash-handovers',
      name: 'CashHandover',
      component: () => import('../views/crm/CashHandoverView.vue'),
      meta: { title: 'Kassa topshirish', requiresAuth: true },
    },
    {
      path: '/users',
      redirect: () => {
        const auth = useAuthStore()
        if (auth.canManageEmployees) return '/employees'
        return '/settings/users'
      },
    },
    {
      path: '/employees',
      name: 'Employees',
      component: () => import('../views/crm/UsersView.vue'),
      meta: { title: 'Xodimlar', requiresAuth: true, employeesOnly: true },
    },
    {
      path: '/settings',
      name: 'SettingsGeneral',
      component: () => import('../views/crm/settings/GeneralSettingsView.vue'),
      meta: { title: 'Umumiy sozlamalar', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/filials',
      name: 'SettingsFilials',
      component: () => import('../views/crm/settings/FilialsView.vue'),
      meta: { title: 'Filiallar', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/users',
      name: 'SettingsUsers',
      component: () => import('../views/crm/UsersView.vue'),
      meta: { title: 'Foydalanuvchilar', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/sessions',
      name: 'SettingsSessions',
      component: () => import('../views/crm/settings/SessionsSettingsView.vue'),
      meta: { title: 'Faol sessiyalar', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/roles',
      name: 'SettingsRoles',
      component: () => import('../views/crm/settings/RolesSettingsView.vue'),
      meta: { title: 'Rollar', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/company',
      name: 'SettingsCompany',
      component: () => import('../views/crm/settings/CompanySettingsView.vue'),
      meta: { title: 'Tashkilot rekvizitlari', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/telegram',
      name: 'SettingsTelegram',
      component: () => import('../views/crm/settings/TelegramSettingsView.vue'),
      meta: { title: 'Telegram bot', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/eskiz',
      name: 'SettingsEskiz',
      component: () => import('../views/crm/settings/EskizSettingsView.vue'),
      meta: { title: 'Eskiz.uz SMS', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/units',
      name: 'SettingsUnits',
      component: () => import('../views/crm/settings/UnitsSettingsView.vue'),
      meta: { title: 'O‘lchov birliklari', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/payment-types',
      name: 'SettingsPaymentTypes',
      component: () => import('../views/crm/settings/PaymentTypesSettingsView.vue'),
      meta: { title: 'To‘lov turlari', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/expense-categories',
      name: 'SettingsExpenseCategories',
      component: () => import('../views/crm/settings/ExpenseCategoriesSettingsView.vue'),
      meta: { title: 'Xarajat kategoriyalari', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/audit',
      name: 'SettingsAudit',
      component: () => import('../views/crm/settings/AuditSettingsView.vue'),
      meta: { title: 'Audit loglar', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/discount-rules',
      name: 'SettingsDiscountRules',
      component: () => import('../views/crm/settings/DiscountRulesSettingsView.vue'),
      meta: { title: 'Chegirma qoidalari', requiresAuth: true, settingsAdmin: true },
    },
    {
      path: '/settings/exchange-rates',
      redirect: '/currency',
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/Others/UserProfile.vue'),
      meta: { title: 'Profil', requiresAuth: true },
    },
    {
      path: '/profile/password',
      redirect: { path: '/profile', hash: '#password' },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('../views/Errors/FourZeroFour.vue'),
      meta: { title: '404' },
    },
  ],
})

function applyDocumentTitle(route: RouteLocationNormalized) {
  const key = `routes.${String(route.name ?? '')}`
  const title = route.name && i18n.global.te(key) ? i18n.global.t(key) : route.meta.title || 'WinCRM'
  document.title = `${title} | WinCRM`
}

watch(i18n.global.locale, () => applyDocumentTitle(router.currentRoute.value))

router.beforeEach((to, _from, next) => {
  applyDocumentTitle(to)

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

  if (auth.isAuthenticated && auth.isProductionManagerOnly) {
    const allowed =
      to.path === '/production/dashboard' ||
      to.path.startsWith('/production/dashboard/') ||
      to.path === '/production/board' ||
      to.path.startsWith('/production/board/') ||
      to.path === '/workshops' ||
      to.path.startsWith('/workshops/') ||
      to.path === '/profile' ||
      to.path.startsWith('/profile/')
    if (to.path === '/' || to.path === '' || to.path === '/production') {
      next({ path: '/production/dashboard' })
      return
    }
    if (!allowed) {
      next({ path: '/production/dashboard' })
      return
    }
  }

  if (auth.isAuthenticated && auth.isTransportManagerOnly) {
    const allowed =
      to.path.startsWith('/transport/') || to.path === '/profile' || to.path.startsWith('/profile/')
    if (!allowed) {
      next({ path: TRANSPORT_HOME })
      return
    }
  }

  if (auth.isAuthenticated && auth.isCashierOnly) {
    const allowed =
      to.path === '/payments' ||
      to.path === '/payments/dashboard' ||
      to.path === '/cash-handovers' ||
      to.path === '/clients' ||
      to.path.startsWith('/clients/') ||
      to.path === '/profile' ||
      to.path.startsWith('/profile/')
    if (!allowed) {
      next({ path: '/payments' })
      return
    }
  }

  if (auth.isAuthenticated && to.matched.some((r) => r.meta.transport) && !auth.canAccessTransport) {
    next({ path: '/' })
    return
  }

  if (auth.isAuthenticated && to.matched.some((r) => r.meta.settingsAdmin) && !auth.canAccessSettings) {
    next({ path: auth.canManageEmployees ? '/employees' : '/' })
    return
  }

  if (auth.isAuthenticated && to.matched.some((r) => r.meta.employeesOnly) && !auth.canManageEmployees) {
    next({ path: auth.canAccessSettings ? '/settings/users' : '/' })
    return
  }

  next()
})

export default router
