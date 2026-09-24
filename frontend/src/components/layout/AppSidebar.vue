<template>
  <aside
    :class="[
      'fixed flex flex-col mt-0 top-0 px-5 start-0 bg-white dark:bg-gray-900 dark:border-gray-800 text-gray-900 h-screen transition-all duration-300 ease-in-out z-99999 border-e border-gray-200',
      {
        'xl:w-[290px]': isExpanded || isMobileOpen || isHovered,
        'xl:w-[90px]': !isExpanded && !isHovered,
        'translate-x-0 w-[290px]': isMobileOpen,
        'max-xl:-translate-x-full max-xl:rtl:translate-x-full': !isMobileOpen,
        'xl:translate-x-0': true,
      },
    ]"
    @mouseenter="!isExpanded && (isHovered = true)"
    @mouseleave="isHovered = false"
  >
    <div
      :class="[
        'pt-8 pb-7 flex',
        !isExpanded && !isHovered ? 'xl:justify-center' : 'justify-start',
      ]"
    >
      <router-link to="/">
        <img
          v-if="isExpanded || isHovered || isMobileOpen"
          class="dark:hidden"
          src="/images/logo/logo.svg"
          alt="WinCRM"
          width="150"
          height="40"
        />
        <img
          v-if="isExpanded || isHovered || isMobileOpen"
          class="hidden dark:block"
          src="/images/logo/logo-dark.svg"
          alt="WinCRM"
          width="150"
          height="40"
        />
        <img
          v-else
          src="/images/logo/logo-icon.svg"
          alt="WinCRM"
          width="32"
          height="32"
        />
      </router-link>
    </div>
    <div
      class="flex flex-col overflow-y-auto duration-300 ease-linear no-scrollbar"
    >
      <nav class="mb-6">
        <div class="flex flex-col gap-4">
          <div v-for="(menuGroup, groupIndex) in menuGroups" :key="groupIndex">
            <h2
              :class="[
                'mb-4 text-xs uppercase flex leading-5 text-gray-400',
                !isExpanded && !isHovered
                  ? 'xl:justify-center'
                  : 'justify-start',
              ]"
            >
              <template v-if="isExpanded || isHovered || isMobileOpen">
                {{ menuGroup.title }}
              </template>
              <HorizontalDots v-else />
            </h2>
            <ul class="flex flex-col gap-1">
              <li v-for="(item, index) in menuGroup.items" :key="item.name">
                <button
                  v-if="item.subItems"
                  @click="toggleSubmenu(groupIndex, index)"
                  :class="[
                    'menu-item group w-full',
                    {
                      'menu-item-active': isSubmenuOpen(groupIndex, index),
                      'menu-item-inactive': !isSubmenuOpen(groupIndex, index),
                    },
                    !isExpanded && !isHovered
                      ? 'xl:justify-center'
                      : 'xl:justify-start',
                  ]"
                >
                  <span
                    :class="[
                      isSubmenuOpen(groupIndex, index)
                        ? 'menu-item-icon-active'
                        : 'menu-item-icon-inactive',
                    ]"
                  >
                    <component :is="item.icon" />
                  </span>
                  <span
                    v-if="isExpanded || isHovered || isMobileOpen"
                    class="menu-item-text truncate"
                    >{{ item.name }}</span
                  >
                  <ChevronDownIcon
                    v-if="isExpanded || isHovered || isMobileOpen"
                    :class="[
                      'ms-auto w-5 h-5 transition-transform duration-200',
                      {
                        'rotate-180 text-brand-500': isSubmenuOpen(
                          groupIndex,
                          index
                        ),
                      },
                    ]"
                  />
                </button>
                <router-link
                  v-else-if="item.path"
                  :to="item.path"
                  :class="[
                    'menu-item group',
                    {
                      'menu-item-active': isActive(item.path),
                      'menu-item-inactive': !isActive(item.path),
                    },
                  ]"
                >
                  <span
                    :class="[
                      isActive(item.path)
                        ? 'menu-item-icon-active'
                        : 'menu-item-icon-inactive',
                    ]"
                  >
                    <component :is="item.icon" />
                  </span>
                  <span
                    v-if="isExpanded || isHovered || isMobileOpen"
                    class="menu-item-text"
                    >{{ item.name }}</span
                  >
                </router-link>
                <transition
                  @enter="startTransition"
                  @after-enter="endTransition"
                  @before-leave="startTransition"
                  @after-leave="endTransition"
                >
                  <div
                    v-show="
                      isSubmenuOpen(groupIndex, index) &&
                      (isExpanded || isHovered || isMobileOpen)
                    "
                  >
                    <ul class="mt-2 space-y-1 ms-4">
                      <li v-for="subItem in item.subItems" :key="subItem.name">
                        <router-link
                          :to="subItem.path"
                          :class="[
                            'menu-dropdown-item',
                            {
                              'menu-dropdown-item-active': isActive(
                                subItem.path,
                                subItem.exact,
                                subItem.match
                              ),
                              'menu-dropdown-item-inactive': !isActive(
                                subItem.path,
                                subItem.exact,
                                subItem.match
                              ),
                            },
                          ]"
                        >
                          <component
                            :is="subItem.icon"
                            v-if="subItem.icon"
                            class="h-4 w-4 shrink-0"
                          />
                          <span class="truncate">{{ subItem.name }}</span>
                          <span class="flex items-center gap-1 ms-auto">
                            <span
                              v-if="subItem.new"
                              :class="[
                                'menu-dropdown-badge',
                                {
                                  'menu-dropdown-badge-active': isActive(
                                    subItem.path,
                                    subItem.exact
                                  ),
                                  'menu-dropdown-badge-inactive': !isActive(
                                    subItem.path,
                                    subItem.exact
                                  ),
                                },
                              ]"
                            >
                              new
                            </span>
                            <span
                              v-if="subItem.pro"
                              :class="[
                                'menu-dropdown-badge',
                                {
                                  'menu-dropdown-badge-active': isActive(
                                    subItem.path,
                                    subItem.exact
                                  ),
                                  'menu-dropdown-badge-inactive': !isActive(
                                    subItem.path,
                                    subItem.exact
                                  ),
                                },
                              ]"
                            >
                              pro
                            </span>
                          </span>
                        </router-link>
                      </li>
                    </ul>
                  </div>
                </transition>
              </li>
            </ul>
          </div>
        </div>
      </nav>
      <SidebarWidget v-if="isExpanded || isHovered || isMobileOpen" />
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, watch, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeftRight,
  BarChart3,
  Box,
  Building2,
  ClipboardList,
  FileText,
  Files,
  Folder,
  Factory,
  History,
  LayoutGrid,
  List,
  Package,
  Plug,
  Send,
  Settings,
  Shield,
  ShieldCheck,
  UserCircle,
} from 'lucide-vue-next'

import { useSidebar } from '@/composables/useSidebar'
import {
  BoxIcon,
  ChevronDownIcon,
  DocsIcon,
  HomeIcon,
  HorizontalDots,
  PieChartIcon,
  SettingsIcon,
  TaskIcon,
  UserCircleIcon,
  UserGroupIcon,
} from '@/icons'
import SidebarWidget from './SidebarWidget.vue'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const route = useRoute()
const auth = useAuthStore()

const { isExpanded, isMobileOpen, isHovered, openSubmenu } = useSidebar()

const isSalesOrdersPath = (path: string) =>
  path === '/sales' || path.startsWith('/sales/create') || /^\/sales\/\d+(?:\/|$)/.test(path)

interface SubItem {
  name: string
  path: string
  icon?: Component
  exact?: boolean
  match?: (path: string) => boolean
  pro?: boolean
  new?: boolean
}

interface MenuItem {
  icon?: Component
  name: string
  path?: string
  subItems?: SubItem[]
  new?: boolean
  pro?: boolean
}

interface MenuGroup {
  title: string
  items: MenuItem[]
}

const menuGroups = computed<MenuGroup[]>(() => {
  const productionSubItems: SubItem[] = [
    { name: t('nav.productionDashboard'), path: '/production/dashboard', icon: LayoutGrid, exact: true },
    { name: t('nav.productionBoard'), path: '/production/board', icon: LayoutGrid, exact: true },
    { name: t('nav.workshops'), path: '/workshops', icon: Factory, exact: true },
  ]

  if (!auth.isProductionManagerOnly) {
    productionSubItems.splice(2, 0, {
      name: t('nav.productionOrders'),
      path: '/production',
      icon: ClipboardList,
      exact: true,
    })
  }

  const productionItem: MenuItem = {
    icon: Factory,
    name: t('nav.productionMenu'),
    subItems: productionSubItems,
  }

  if (auth.isProductionManagerOnly) {
    return [
      {
        title: t('nav.main'),
        items: [productionItem],
      },
    ]
  }

  return [
  {
    title: t('nav.main'),
    items: [
      { icon: HomeIcon, name: t('nav.home'), path: '/' },
      {
        icon: PieChartIcon,
        name: t('nav.sales'),
        subItems: [
          { name: t('nav.salesDashboard'), path: '/sales/dashboard', icon: LayoutGrid, exact: true },
          {
            name: t('nav.salesOrders'),
            path: '/sales',
            icon: ClipboardList,
            match: isSalesOrdersPath,
          },
          { name: t('nav.salesReport'), path: '/sales/report', icon: List, exact: true },
          { name: t('nav.salesWaste'), path: '/sales/wastes', icon: FileText, exact: true },
        ],
      },
      {
        icon: BoxIcon,
        name: t('nav.suppliers'),
        subItems: [
          { name: t('nav.suppliersList'), path: '/suppliers', icon: ClipboardList, exact: true },
          { name: t('nav.suppliersBalance'), path: '/suppliers/balances', icon: PieChartIcon },
        ],
      },
      {
        icon: Package,
        name: t('nav.warehouseMenu'),
        subItems: [
          { name: t('nav.goods'), path: '/goods', icon: BoxIcon },
          { name: t('nav.inbound'), path: '/warehouse-orders', icon: ClipboardList },
          { name: t('nav.stock'), path: '/stock', icon: Box, exact: true },
          { name: t('nav.stockHistory'), path: '/stock/history', icon: History },
          { name: t('nav.inventory'), path: '/inventory', icon: Files },
          { name: t('nav.transfers'), path: '/stock/transfers', icon: ArrowLeftRight },
          { name: t('nav.warehouses'), path: '/warehouses', icon: Package },
        ],
      },
      productionItem,
      { icon: PieChartIcon, name: t('nav.payments'), path: '/payments' },
      { icon: UserGroupIcon, name: t('nav.clients'), path: '/clients' },
      ...(auth.canManageEmployees
        ? [{ icon: UserCircleIcon, name: t('nav.employees'), path: '/employees' }]
        : []),
    ],
  },
  {
    title: t('nav.finance'),
    items: [
      { icon: TaskIcon, name: t('nav.expenses'), path: '/expenses' },
      { icon: DocsIcon, name: t('nav.salary'), path: '/salary' },
    ],
  },
  {
    title: t('nav.system'),
    items: [
      ...(auth.canAccessSettings
        ? [
            {
              icon: SettingsIcon,
              name: t('nav.settings'),
              subItems: [
                { name: t('nav.generalSettings'), path: '/settings', icon: Settings, exact: true },
                { name: t('nav.filials'), path: '/settings/filials', icon: Building2 },
                { name: t('nav.users'), path: '/settings/users', icon: UserCircle },
                { name: t('nav.sessions'), path: '/settings/sessions', icon: Plug },
                { name: t('nav.roles'), path: '/settings/roles', icon: Shield },
                { name: t('nav.company'), path: '/settings/company', icon: Files },
                { name: t('nav.telegram'), path: '/settings/telegram', icon: Plug },
                { name: t('nav.eskiz'), path: '/settings/eskiz', icon: Send },
                { name: t('nav.units'), path: '/settings/units', icon: List },
                { name: t('nav.paymentTypes'), path: '/settings/payment-types', icon: BarChart3 },
                { name: t('nav.expenseCategories'), path: '/settings/expense-categories', icon: Folder },
                { name: t('nav.audit'), path: '/settings/audit', icon: ShieldCheck },
              ],
            },
          ]
        : []),
      { icon: UserCircleIcon, name: t('nav.profile'), path: '/profile' },
    ],
  },
]
})

const isActive = (path?: string, exact = false, match?: (path: string) => boolean) => {
  if (match) return match(route.path)
  if (!path) return false
  if (route.path === path) return true
  if (!exact && path !== '/' && route.path.startsWith(`${path}/`)) return true
  return false
}

const setActiveMenuFromRoute = () => {
  menuGroups.value.forEach((group, groupIndex) => {
    group.items.forEach((item, itemIndex) => {
      if (
        item.subItems?.some((subItem) => isActive(subItem.path, subItem.exact, subItem.match))
      ) {
        openSubmenu.value = `${groupIndex}-${itemIndex}`
      }
    })
  })
}

watch(
  () => route.path,
  () => {
    setActiveMenuFromRoute()
  },
  { immediate: true },
)

const toggleSubmenu = (groupIndex: number, itemIndex: number) => {
  const key = `${groupIndex}-${itemIndex}`
  openSubmenu.value = openSubmenu.value === key ? null : key
}

const isSubmenuOpen = (groupIndex: number, itemIndex: number) => {
  const key = `${groupIndex}-${itemIndex}`
  return openSubmenu.value === key
}

const startTransition = (el: Element) => {
  const htmlEl = el as HTMLElement
  htmlEl.style.height = 'auto'
  const height = htmlEl.scrollHeight
  htmlEl.style.height = '0px'
  void htmlEl.offsetHeight // force reflow
  htmlEl.style.height = height + 'px'
}

const endTransition = (el: Element) => {
  const htmlEl = el as HTMLElement
  htmlEl.style.height = ''
}
</script>
