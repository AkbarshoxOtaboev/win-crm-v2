<template>
  <AdminLayout>
    <PageBreadcrumb
      :pageTitle="client?.fullName || t('common.client')"
      :items="breadcrumbItems"
    />

    <div class="mb-4">
      <router-link to="/clients" class="text-sm text-brand-500 hover:underline">
        {{ t('clientDetail.backToClients') }}
      </router-link>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="loading" class="empty-block">{{ t('common.loading') }}</div>

    <template v-else-if="client">
      <!-- Profile header -->
      <div class="card mb-4 p-5">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h3 class="text-xl font-bold uppercase text-gray-800 dark:text-white/90">
                {{ client.fullName }}
              </h3>
              <span v-if="client.clientGroupName" class="group-badge">{{ client.clientGroupName }}</span>
              <span v-else class="group-badge">{{ t('clientDetail.groupDefault') }}</span>
            </div>
            <p class="mt-1 text-sm text-gray-500">
              <span :class="client.status === 'DISABLED' ? 'text-warning-600' : 'text-success-600'">
                {{ client.status === 'DISABLED' ? t('common.inactive') : t('common.active') }}
              </span>
              <span class="mx-1.5 text-gray-300">·</span>
              {{ t('clientDetail.lastActivity', { date: lastActivityLabel }) }}
            </p>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <router-link :to="`/sales/create?clientId=${client.id}`" class="btn">
              <Plus class="h-4 w-4" />
              {{ t('clientDetail.newOrder') }}
            </router-link>
            <router-link :to="`/payments?clientId=${client.id}`" class="ghost-btn">
              <Wallet class="h-4 w-4" />
              {{ t('clientDetail.makePayment') }}
            </router-link>
            <button type="button" class="ghost-btn" :disabled="smsSending" @click="onSendSms">
              <MessageSquare class="h-4 w-4" />
              {{ t('clientDetail.sendDebtSms') }}
            </button>
            <a
              v-if="client.phone"
              class="ghost-btn"
              :href="`https://t.me/+${phoneDigits(client.phone)}`"
              target="_blank"
              rel="noopener"
            >
              <Send class="h-4 w-4" />
              {{ t('clientDetail.sendTelegram') }}
            </a>
            <a v-if="client.phone" class="ghost-btn" :href="`tel:${client.phone}`">
              <Phone class="h-4 w-4" />
              {{ t('clientDetail.call') }}
            </a>
          </div>
        </div>

        <div v-if="clientCurrencies.length > 1" class="cur-switch mt-5">
          <span class="text-sm text-gray-500">{{ t('clientDetail.viewCurrency') }}</span>
          <button
            v-for="c in clientCurrencies"
            :key="c"
            type="button"
            class="cur-pill"
            :class="{ active: viewCurrency === c }"
            @click="viewCurrency = c"
          >
            {{ t(`exchangeRates.currencies.${c}`) }}
          </button>
        </div>
        <div class="mt-6 grid grid-cols-1 gap-4 border-t border-gray-100 pt-5 sm:grid-cols-3 dark:border-gray-800">
          <div>
            <p class="text-sm text-gray-500">{{ t('clientDetail.totalSales') }}</p>
            <p class="mt-1 text-2xl font-bold text-brand-500">{{ moneySom(totalSales) }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-500">{{ t('clientDetail.totalPaid') }}</p>
            <p class="mt-1 text-2xl font-bold text-success-600">{{ moneySom(totalPaid) }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-500">{{ t('clientDetail.totalDebt') }}</p>
            <p class="mt-1 text-2xl font-bold text-error-600">{{ moneySom(totalDebt) }}</p>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs-bar mb-4">
        <button
          v-for="item in tabs"
          :key="item.key"
          type="button"
          class="tab-pill"
          :class="{ active: tab === item.key }"
          @click="tab = item.key"
        >
          {{ item.label }}
        </button>
      </div>

      <!-- Umumiy -->
      <div v-show="tab === 'overview'" class="space-y-4">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <article class="kpi-card kpi-red">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">{{ t('clientDetail.totalDebt') }}</p>
                <p class="kpi-value text-error-600">{{ moneySom(totalDebt) }}</p>
                <p class="kpi-sub">{{ debtAgeLabel }}</p>
              </div>
              <span class="kpi-icon bg-error-50 text-error-500"><Wallet class="h-5 w-5" /></span>
            </div>
          </article>
          <article class="kpi-card kpi-green">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">{{ t('clientDetail.margin') }}</p>
                <p class="kpi-value text-brand-500">{{ marginPct.toFixed(1) }}%</p>
                <p class="kpi-sub">{{ moneySom(totalProfit) }}</p>
              </div>
              <span class="kpi-icon bg-success-50 text-success-600"><Percent class="h-5 w-5" /></span>
            </div>
          </article>
          <article class="kpi-card kpi-blue">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">{{ t('clientDetail.orders') }}</p>
                <p class="kpi-value">{{ sales.length }}</p>
                <p class="kpi-sub">{{ lastActivityRelative }}</p>
              </div>
              <span class="kpi-icon bg-blue-light-50 text-blue-light-500"><Package class="h-5 w-5" /></span>
            </div>
          </article>
          <article class="kpi-card kpi-yellow">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">{{ t('clientDetail.avgCheck') }}</p>
                <p class="kpi-value">{{ moneySom(avgCheck) }}</p>
                <p class="kpi-sub">{{ moneySom(totalSales) }}</p>
              </div>
              <span class="kpi-icon bg-warning-50 text-warning-600"><Banknote class="h-5 w-5" /></span>
            </div>
          </article>
        </div>

        <!-- Qarz yoshi -->
        <div class="card p-5">
          <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div>
              <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.debtAge') }}</h4>
              <p class="mt-0.5 text-sm text-gray-500">{{ t('clientDetail.debtAgeSubtitle') }}</p>
            </div>
            <div class="text-right">
              <p class="text-sm text-gray-500">{{ t('clientDetail.totalDebt') }}</p>
              <p class="text-xl font-bold text-error-600">{{ moneySom(totalDebt) }}</p>
            </div>
          </div>

          <div class="debt-bar mb-2">
            <div
              v-for="seg in debtSegments"
              :key="seg.key"
              class="debt-bar-seg"
              :style="{ width: `${Math.max(seg.pct, seg.amount > 0 ? 2 : 0)}%`, background: seg.color }"
              :title="`${seg.label}: ${moneySom(seg.amount)}`"
            />
          </div>
          <div class="mb-5 flex flex-wrap gap-3">
            <span
              v-for="seg in debtSegments.filter((s) => s.amount > 0 || s.key === '0-30')"
              :key="`leg-${seg.key}`"
              class="inline-flex items-center gap-1.5 text-xs text-gray-500"
            >
              <i class="h-2 w-2 rounded-full" :style="{ background: seg.color }" />
              {{ seg.label }}
            </span>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <article
              v-for="seg in debtSegments"
              :key="`card-${seg.key}`"
              class="debt-seg-card"
              :style="{ '--accent': seg.color }"
            >
              <p class="text-xs text-gray-500">{{ seg.label }}</p>
              <p class="mt-2 text-lg font-bold text-gray-800 dark:text-white/90">{{ moneySom(seg.amount) }}</p>
              <div class="mt-3 flex items-center justify-between text-xs">
                <span class="text-gray-500">{{ t('clientDetail.segmentCount', { n: seg.count }) }}</span>
                <span class="font-medium" :style="{ color: seg.color }">{{ seg.pct }}%</span>
              </div>
              <div class="mt-2 h-1 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
                <div class="h-full rounded-full" :style="{ width: `${seg.pct}%`, background: seg.color }" />
              </div>
            </article>
          </div>
        </div>

        <!-- Foyda / Oyna -->
        <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
          <div class="card p-5">
            <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.profitTitle') }}</h4>
            <p class="mt-0.5 text-sm text-gray-500">{{ t('clientDetail.profitSubtitle') }}</p>
            <div class="mt-4 grid grid-cols-2 gap-3">
              <div class="metric-box">
                <p class="text-xs text-gray-500">{{ t('clientDetail.totalSales') }}</p>
                <p class="mt-1 font-semibold text-brand-500">{{ moneySom(itemsSales) }}</p>
              </div>
              <div class="metric-box">
                <p class="text-xs text-gray-500">{{ t('clientDetail.totalCost') }}</p>
                <p class="mt-1 font-semibold text-gray-800 dark:text-white/90">{{ moneySom(itemsCost) }}</p>
              </div>
              <div class="metric-box">
                <p class="text-xs text-gray-500">{{ t('clientDetail.totalProfit') }}</p>
                <p class="mt-1 font-semibold text-success-600">{{ moneySom(totalProfit) }}</p>
              </div>
              <div class="metric-box">
                <p class="text-xs text-gray-500">{{ t('clientDetail.margin') }}</p>
                <p class="mt-1 font-semibold text-gray-800 dark:text-white/90">{{ marginPct.toFixed(1) }}%</p>
              </div>
            </div>
            <div v-if="items.length === 0" class="mt-6 py-8 text-center text-sm text-gray-400">{{ t('common.notFound') }}</div>
          </div>

          <div class="card flex flex-col items-center justify-center p-8 text-center">
            <LayoutGrid class="mb-3 h-10 w-10 text-gray-300" />
            <h4 class="text-base font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.windowStats') }}</h4>
            <p class="mt-1 text-sm text-gray-500">
              {{
                windowItemsCount > 0
                  ? t('clientDetail.windowItems', { n: windowItemsCount })
                  : t('clientDetail.noWindowOrders')
              }}
            </p>
          </div>
        </div>

        <!-- Keyingi to'lov -->
        <div class="card p-5">
          <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div class="flex items-start gap-3">
              <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-warning-50 text-warning-600">
                <CalendarClock class="h-5 w-5" />
              </span>
              <div>
                <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.nextPaymentDate') }}</h4>
                <p class="mt-0.5 text-sm text-gray-500">{{ t('clientDetail.nextPaymentSubtitle') }}</p>
              </div>
            </div>
            <span class="status-pill" :class="nextPaymentDate ? 'status-set' : ''">
              {{ nextPaymentDate ? formatDate(nextPaymentDate) : t('clientDetail.notSet') }}
            </span>
          </div>
          <label class="lbl">{{ t('clientDetail.nextPaymentDate') }}</label>
          <div class="mt-1 flex flex-wrap gap-2">
            <input v-model="nextPaymentDate" type="date" class="field max-w-xs" @change="saveNextPayment" />
            <button type="button" class="ghost-btn" @click="clearNextPayment">{{ t('clientDetail.clear') }}</button>
          </div>
        </div>

        <!-- Sotuvchilar / Ombor -->
        <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
          <div class="card p-5">
            <div class="mb-4 flex items-start justify-between gap-2">
              <div>
                <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.bySellers') }}</h4>
                <p class="mt-0.5 text-sm text-gray-500">{{ t('clientDetail.sellersSubtitle') }}</p>
              </div>
              <span class="count-pill">{{ t('clientDetail.sellersCount', { n: sellers.length }) }}</span>
            </div>
            <div class="overflow-x-auto">
              <table class="min-w-full">
                <thead>
                  <tr class="border-b border-gray-100 dark:border-gray-800">
                    <th class="th">#</th>
                    <th class="th">{{ t('clientDetail.seller') }}</th>
                    <th class="th">{{ t('clientDetail.orders') }}</th>
                    <th class="th">{{ t('clientDetail.totalSales') }}</th>
                    <th class="th">{{ t('clientDetail.share') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="sellers.length === 0">
                    <td colspan="5" class="empty">{{ t('clientDetail.noSellers') }}</td>
                  </tr>
                  <tr
                    v-for="(s, i) in sellers"
                    :key="s.key"
                    class="border-b border-gray-100 dark:border-gray-800"
                  >
                    <td class="td">{{ i + 1 }}</td>
                    <td class="td">
                      <div class="flex items-center gap-2">
                        <span class="avatar">{{ initials(s.name) }}</span>
                        <span class="font-medium text-brand-500">{{ s.name }}</span>
                      </div>
                    </td>
                    <td class="td">{{ s.count }}</td>
                    <td class="td font-semibold">{{ moneySom(s.sum) }}</td>
                    <td class="td text-gray-400">{{ s.share }}%</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="card p-5">
            <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.byWarehouse') }}</h4>
            <p class="mt-0.5 text-sm text-gray-500">{{ t('clientDetail.warehouseSubtitle') }}</p>
            <div v-if="warehouseSeries.length === 0" class="py-12 text-center text-sm text-gray-400">
              {{ t('clientDetail.noData') }}
            </div>
            <template v-else>
              <VueApexCharts
                v-if="chartMounted"
                type="donut"
                height="260"
                :options="warehouseChartOptions"
                :series="warehouseSeries"
              />
              <div class="mt-2 space-y-2">
                <div
                  v-for="(w, i) in warehouses"
                  :key="w.name"
                  class="flex items-center justify-between text-sm"
                >
                  <span class="inline-flex items-center gap-2 text-gray-600 dark:text-gray-300">
                    <i
                      class="h-2.5 w-2.5 rounded-full"
                      :style="{ background: warehouseColors[i % warehouseColors.length] }"
                    />
                    {{ w.name }}
                  </span>
                  <span class="font-medium text-gray-700 dark:text-gray-200">{{ w.share }}%</span>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- Tahlil -->
      <div v-show="tab === 'analysis'" class="card p-5">
        <h4 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.tabs.analysis') }}</h4>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div class="metric-box">
            <p class="text-xs text-gray-500">{{ t('clientDetail.totalSales') }}</p>
            <p class="mt-1 text-lg font-bold text-brand-500">{{ moneySom(totalSales) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">{{ t('clientDetail.paid') }}</p>
            <p class="mt-1 text-lg font-bold text-success-600">{{ moneySom(totalPaid) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">{{ t('clientDetail.debt') }}</p>
            <p class="mt-1 text-lg font-bold text-error-600">{{ moneySom(totalDebt) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">{{ t('clientDetail.margin') }}</p>
            <p class="mt-1 text-lg font-bold">{{ marginPct.toFixed(1) }}%</p>
          </div>
        </div>
        <div class="mt-5">
          <h5 class="mb-3 font-medium text-gray-700 dark:text-gray-200">{{ t('clientDetail.debtAge') }}</h5>
          <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <div v-for="seg in debtSegments" :key="`a-${seg.key}`" class="metric-box">
              <p class="text-xs text-gray-500">{{ seg.label }}</p>
              <p class="mt-1 font-semibold" :style="{ color: seg.color }">{{ moneySom(seg.amount) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Buyurtmalar -->
      <div v-show="tab === 'orders'" class="card">
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">#</th>
                <th class="th">{{ t('common.date') }}</th>
                <th class="th">{{ t('common.warehouse') }}</th>
                <th class="th">{{ t('clientDetail.seller') }}</th>
                <th class="th">{{ t('common.total') }}</th>
                <th class="th">{{ t('clientDetail.paid') }}</th>
                <th class="th">{{ t('clientDetail.debt') }}</th>
                <th class="th">{{ t('common.status') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="sales.length === 0"><td colspan="8" class="empty">{{ t('clientDetail.noOrders') }}</td></tr>
              <tr
                v-for="o in sales"
                :key="o.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">
                  <router-link :to="`/sales/${o.id}`" class="text-brand-500 hover:underline">
                    #{{ o.id }}
                  </router-link>
                </td>
                <td class="td">{{ formatDate(o.orderDate) }}</td>
                <td class="td">{{ o.warehouseName || '—' }}</td>
                <td class="td">{{ o.userFullName || '—' }}</td>
                <td class="td">{{ moneySom(o.totalSum) }}</td>
                <td class="td">{{ moneySom(o.paidSum) }}</td>
                <td class="td text-error-600">{{ moneySom(o.debtSum) }}</td>
                <td class="td">{{ o.orderStatus ? enumLabel('saleStatus', o.orderStatus) : '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pozitsiyalar -->
      <div v-show="tab === 'items'" class="card">
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">#</th>
                <th class="th">{{ t('clientDetail.goods') }}</th>
                <th class="th">{{ t('common.warehouse') }}</th>
                <th class="th">{{ t('common.count') }}</th>
                <th class="th">{{ t('clientDetail.cost') }}</th>
                <th class="th">{{ t('clientDetail.sale') }}</th>
                <th class="th">{{ t('clientDetail.order') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="items.length === 0"><td colspan="7" class="empty">{{ t('clientDetail.noItems') }}</td></tr>
              <tr
                v-for="it in items"
                :key="it.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ it.id }}</td>
                <td class="td">{{ it.goodsName || '—' }}</td>
                <td class="td">{{ it.warehouseName || '—' }}</td>
                <td class="td">{{ it.count ?? '—' }}</td>
                <td class="td whitespace-nowrap">{{ moneySom(it.priceCost) }}</td>
                <td class="td whitespace-nowrap">{{ moneySom(it.priceSelling) }}</td>
                <td class="td">
                  <router-link
                    v-if="it.saleOrderId"
                    :to="`/sales/${it.saleOrderId}`"
                    class="text-brand-500"
                  >
                    #{{ it.saleOrderId }}
                  </router-link>
                  <span v-else>—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Akt / Hisob-kitob -->
      <div v-show="tab === 'act'" class="space-y-4">
        <div class="card p-5">
          <h4 class="mb-1 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.tabs.act') }}</h4>
          <p class="mb-4 text-sm text-gray-500">{{ t('clientDetail.actSubtitle') }}</p>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div class="metric-box">
              <p class="text-xs text-gray-500">{{ t('clientDetail.debit') }}</p>
              <p class="mt-1 text-lg font-bold text-brand-500">{{ moneySom(totalSales) }}</p>
            </div>
            <div class="metric-box">
              <p class="text-xs text-gray-500">{{ t('clientDetail.credit') }}</p>
              <p class="mt-1 text-lg font-bold text-success-600">{{ moneySom(totalPaid) }}</p>
            </div>
            <div class="metric-box">
              <p class="text-xs text-gray-500">{{ t('clientDetail.balanceRest') }}</p>
              <p class="mt-1 text-lg font-bold text-error-600">{{ moneySom(totalDebt) }}</p>
            </div>
            <div class="metric-box">
              <p class="text-xs text-gray-500">{{ t('clientDetail.allocation.unallocated') }}</p>
              <p class="mt-1 text-lg font-bold text-warning-600">{{ moneySom(unallocatedTotal) }}</p>
            </div>
          </div>
        </div>

        <div v-if="canAllocate" class="card p-5">
          <h4 class="mb-1 text-base font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.allocation.title') }}</h4>
          <p class="mb-4 text-sm text-gray-500">{{ t('clientDetail.allocation.hint') }}</p>

          <div class="grid gap-4 lg:grid-cols-2">
            <div class="alloc-box">
              <div class="alloc-head">
                <span>{{ t('clientDetail.allocation.payments') }}</span>
                <label v-if="unallocatedPayments.length" class="inline-flex items-center gap-1.5 text-xs font-normal">
                  <input
                    type="checkbox"
                    :checked="allPaymentsSelected"
                    @change="toggleAllPayments(($event.target as HTMLInputElement).checked)"
                  />
                  {{ t('clientDetail.allocation.selectAll') }}
                </label>
              </div>
              <p v-if="unallocatedPayments.length === 0" class="empty">{{ t('clientDetail.allocation.noUnallocated') }}</p>
              <table v-else class="min-w-full">
                <tbody>
                  <tr v-for="p in unallocatedPayments" :key="p.id" class="border-b border-gray-100 last:border-0 dark:border-gray-800">
                    <td class="td w-8"><input v-model="selectedPaymentIds" type="checkbox" :value="p.id" /></td>
                    <td class="td">
                      <div class="font-medium text-gray-800 dark:text-white/90">#{{ p.id }} · {{ p.paymentTypeName || '—' }}</div>
                      <div class="text-xs text-gray-500">{{ formatDate(p.paymentDate) }}<template v-if="p.comment"> · {{ p.comment }}</template></div>
                    </td>
                    <td class="td whitespace-nowrap text-end font-semibold text-success-600">
                      {{ moneySom(applied(p)) }}
                      <div v-if="(p.currency || 'UZS') !== viewCurrency" class="text-xs font-normal text-gray-400">{{ cashText(p) }}</div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="alloc-box">
              <div class="alloc-head">
                <span>{{ t('clientDetail.allocation.orders') }}</span>
              </div>
              <p v-if="debtOrders.length === 0" class="empty">{{ t('clientDetail.allocation.noDebtOrders') }}</p>
              <table v-else class="min-w-full">
                <tbody>
                  <tr v-for="o in debtOrders" :key="o.id" class="border-b border-gray-100 last:border-0 dark:border-gray-800">
                    <td class="td w-8">
                      <input
                        type="checkbox"
                        :checked="selectedOrderIds.includes(o.id)"
                        @change="toggleOrder(o, ($event.target as HTMLInputElement).checked)"
                      />
                    </td>
                    <td class="td">
                      <router-link :to="`/sales/${o.id}`" class="font-medium text-brand-500">#{{ o.id }}</router-link>
                      <div class="text-xs text-gray-500">
                        {{ formatDate(o.orderDate) }} · {{ t('clientDetail.allocation.debtLeft') }}: {{ moneySom(o.debtSum) }}
                      </div>
                    </td>
                    <td class="td w-36">
                      <input
                        v-if="selectedOrderIds.includes(o.id)"
                        v-model.number="allocationAmounts[o.id]"
                        type="number"
                        min="0.01"
                        step="0.01"
                        :max="Number(o.debtSum || 0)"
                        class="field h-9 text-end"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="mt-4 flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
            <span class="text-gray-500">{{ t('clientDetail.allocation.selected') }}: <b class="text-gray-800 dark:text-white/90">{{ moneySom(selectedPool) }}</b></span>
            <span class="text-gray-500">{{ t('clientDetail.allocation.allocating') }}: <b class="text-brand-500">{{ moneySom(allocatingTotal) }}</b></span>
            <span class="text-gray-500">{{ t('clientDetail.allocation.left') }}: <b :class="allocationLeft < 0 ? 'text-error-600' : 'text-gray-800 dark:text-white/90'">{{ moneySom(allocationLeft) }}</b></span>
            <button
              type="button"
              class="btn ms-auto"
              :disabled="allocating || !!allocationError || allocatingTotal <= 0"
              @click="onAllocate"
            >
              {{ allocating ? t('common.saving') : t('clientDetail.allocation.submit') }}
            </button>
          </div>
          <p v-if="allocationError && allocatingTotal > 0" class="mt-2 text-sm text-error-600">{{ allocationError }}</p>
          <p v-if="allocationMessage" class="mt-2 text-sm text-success-600">{{ allocationMessage }}</p>
        </div>

        <div class="card">
          <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
            <h4 class="text-base font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.statement.title') }}</h4>
          </div>
          <div class="overflow-x-auto">
            <table class="min-w-full">
              <thead>
                <tr class="border-b border-gray-100 dark:border-gray-800">
                  <th class="th">{{ t('common.date') }}</th>
                  <th class="th">{{ t('clientDetail.statement.document') }}</th>
                  <th class="th text-end">{{ t('clientDetail.debit') }}</th>
                  <th class="th text-end">{{ t('clientDetail.credit') }}</th>
                  <th class="th text-end">{{ t('clientDetail.statement.balance') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="statementRows.length === 0"><td colspan="5" class="empty">{{ t('clientDetail.statement.empty') }}</td></tr>
                <tr v-for="row in statementRows" :key="row.key" class="border-b border-gray-100 dark:border-gray-800">
                  <td class="td whitespace-nowrap">{{ formatDate(row.date) }}</td>
                  <td class="td">
                    <router-link v-if="row.orderId" :to="`/sales/${row.orderId}`" class="text-brand-500">{{ row.title }}</router-link>
                    <span v-else>{{ row.title }}</span>
                    <span v-if="row.badge" class="ms-1.5 inline-flex rounded-md bg-warning-50 px-1.5 py-0.5 text-xs text-warning-700 dark:bg-warning-500/10 dark:text-warning-400">{{ row.badge }}</span>
                    <div v-if="row.cashNote" class="text-xs text-gray-400">{{ t('clientDetail.statement.paidIn', { amount: row.cashNote }) }}</div>
                  </td>
                  <td class="td text-end">{{ row.debit ? stmtMoney(row.debit) : '' }}</td>
                  <td class="td text-end text-success-600">{{ row.credit ? stmtMoney(row.credit) : '' }}</td>
                  <td class="td text-end font-medium" :class="row.balance > 0 ? 'text-error-600' : 'text-gray-800 dark:text-white/90'">{{ stmtMoney(row.balance) }}</td>
                </tr>
              </tbody>
              <tfoot v-if="statementRows.length">
                <tr>
                  <td class="td font-semibold" colspan="2">{{ t('clientDetail.statement.total') }}</td>
                  <td class="td text-end font-semibold">{{ stmtMoney(statementTotals.debit) }}</td>
                  <td class="td text-end font-semibold text-success-600">{{ stmtMoney(statementTotals.credit) }}</td>
                  <td class="td text-end font-semibold" :class="statementTotals.debit - statementTotals.credit > 0 ? 'text-error-600' : ''">{{ stmtMoney(statementTotals.debit - statementTotals.credit) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      </div>

      <!-- To'lovlar -->
      <div v-show="tab === 'payments'" class="card">
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">#</th>
                <th class="th">{{ t('common.sum') }}</th>
                <th class="th">{{ t('common.type') }}</th>
                <th class="th">{{ t('clientDetail.order') }}</th>
                <th class="th">{{ t('common.date') }}</th>
                <th v-if="canAllocate" class="th text-end">{{ t('common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="payments.length === 0"><td :colspan="canAllocate ? 6 : 5" class="empty">{{ t('clientDetail.noPayments') }}</td></tr>
              <tr
                v-for="p in payments"
                :key="p.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ p.id }}</td>
                <td class="td whitespace-nowrap font-semibold text-success-600">
                  {{ cashText(p) }}
                  <div v-if="(p.currency || 'UZS') !== viewCurrency" class="text-xs font-normal text-gray-400">→ {{ moneySom(applied(p)) }}</div>
                </td>
                <td class="td">{{ p.paymentTypeName || '—' }}</td>
                <td class="td">
                  <router-link
                    v-if="p.saleOrderId"
                    :to="`/sales/${p.saleOrderId}`"
                    class="text-brand-500"
                  >
                    #{{ p.saleOrderId }}
                  </router-link>
                  <span v-else class="text-xs text-warning-600">{{ t('clientDetail.allocation.unallocatedShort') }}</span>
                </td>
                <td class="td">{{ formatDate(p.paymentDate) }}</td>
                <td v-if="canAllocate" class="td text-end">
                  <button
                    v-if="p.saleOrderId"
                    type="button"
                    class="text-xs font-medium text-warning-600 hover:underline"
                    @click="onUnallocate(p)"
                  >
                    {{ t('clientDetail.allocation.unallocate') }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Faollik -->
      <div v-show="tab === 'activity'" class="card p-5">
        <h4 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.tabs.activity') }}</h4>
        <ul class="space-y-3">
          <li v-for="ev in activityEvents" :key="ev.key" class="flex gap-3 text-sm">
            <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full" :style="{ background: ev.color }" />
            <div>
              <p class="font-medium text-gray-800 dark:text-white/90">{{ ev.title }}</p>
              <p class="text-gray-500">{{ ev.meta }}</p>
            </div>
          </li>
          <li v-if="activityEvents.length === 0" class="text-sm text-gray-400">{{ t('clientDetail.noActivity') }}</li>
        </ul>
      </div>

      <!-- Eslatmalar -->
      <div v-show="tab === 'notes'" class="card">
        <div class="border-b border-gray-100 p-4 dark:border-gray-800">
          <form class="grid gap-2 md:grid-cols-4" @submit.prevent="onNoteSave">
            <select v-model="noteForm.type" class="field">
              <option v-for="nt in noteTypes" :key="nt" :value="nt">{{ enumLabel('clientDetail.noteTypes', nt) }}</option>
            </select>
            <input v-model="noteForm.content" required class="field md:col-span-2" :placeholder="t('clientDetail.content')" />
            <button type="submit" class="btn justify-center">{{ t('clientDetail.add') }}</button>
          </form>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">{{ t('common.type') }}</th>
                <th class="th">{{ t('clientDetail.content') }}</th>
                <th class="th">{{ t('clientDetail.status') }}</th>
                <th class="th text-right">{{ t('common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="notes.length === 0"><td colspan="4" class="empty">{{ t('clientDetail.noNotes') }}</td></tr>
              <tr
                v-for="n in notes"
                :key="n.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ n.type ? enumLabel('clientDetail.noteTypes', n.type) : '' }}</td>
                <td class="td">{{ n.content }}</td>
                <td class="td">
                  <select
                    :value="n.reminderStatus"
                    class="field"
                    @change="onReminder(n.id, ($event.target as HTMLSelectElement).value)"
                  >
                    <option v-for="rs in reminderStatuses" :key="rs" :value="rs">
                      {{ enumLabel('clientDetail.reminderStatuses', rs) }}
                    </option>
                  </select>
                </td>
                <td class="td text-right">
                  <RowActions :edit="false" @delete="onNoteDelete(n.id)" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Mijoz ma'lumotlari -->
      <div v-show="tab === 'info'" class="card p-5">
        <div class="mb-4 flex items-center justify-between">
          <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.tabs.info') }}</h4>
          <button type="button" class="ghost-btn" @click="openEdit">{{ t('common.edit') }}</button>
        </div>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div><p class="lbl">{{ t('common.fullName') }}</p><p class="info-val">{{ client.fullName }}</p></div>
          <div><p class="lbl">{{ t('common.phone') }}</p><p class="info-val">{{ client.phone || '—' }}</p></div>
          <div><p class="lbl">{{ t('clientDetail.additionalPhone') }}</p><p class="info-val">{{ client.additionalPhone || '—' }}</p></div>
          <div><p class="lbl">{{ t('common.address') }}</p><p class="info-val">{{ client.address || '—' }}</p></div>
          <div><p class="lbl">{{ t('clientDetail.group') }}</p><p class="info-val">{{ client.clientGroupName || '—' }}</p></div>
          <div><p class="lbl">{{ t('common.inn') }}</p><p class="info-val">{{ client.inn || '—' }}</p></div>
          <div><p class="lbl">{{ t('clientDetail.bank') }}</p><p class="info-val">{{ client.bankName || '—' }}</p></div>
          <div><p class="lbl">{{ t('clientDetail.mfo') }}</p><p class="info-val">{{ client.mfo || '—' }}</p></div>
          <div><p class="lbl">{{ t('clientDetail.accountNumber') }}</p><p class="info-val">{{ client.accountNumber || '—' }}</p></div>
          <div class="sm:col-span-2 lg:col-span-3">
            <p class="lbl">{{ t('common.note') }}</p>
            <p class="info-val">{{ client.description || '—' }}</p>
          </div>
        </div>

        <div class="mt-6 border-t border-gray-100 pt-4 dark:border-gray-800">
          <h5 class="mb-3 font-medium text-gray-700">
            {{ t('clientDetail.balanceSettings') }}<template v-if="clientCurrencies.length > 1"> · {{ t(`exchangeRates.currencies.${viewCurrency}`) }}</template>
          </h5>
          <div class="flex flex-wrap items-end gap-2">
            <button type="button" class="btn" @click="onRecalc">{{ t('clientDetail.recalc') }}</button>
            <form class="flex flex-wrap gap-2" @submit.prevent="onAdjust">
              <input v-model.number="adjPurchase" type="number" class="field w-32" :placeholder="t('clientDetail.salePlaceholder')" />
              <input v-model.number="adjPaid" type="number" class="field w-32" :placeholder="t('clientDetail.paymentPlaceholder')" />
              <button type="submit" class="ghost-btn">{{ t('clientDetail.adjust') }}</button>
            </form>
          </div>
        </div>
      </div>

      <!-- SMS -->
      <div v-show="tab === 'sms'" class="card">
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">#</th>
                <th class="th">{{ t('clientDetail.status') }}</th>
                <th class="th">{{ t('clientDetail.message') }}</th>
                <th class="th">{{ t('common.date') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="sms.length === 0"><td colspan="4" class="empty">{{ t('clientDetail.noSms') }}</td></tr>
              <tr
                v-for="h in sms"
                :key="h.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ h.id }}</td>
                <td class="td">{{ h.status }}</td>
                <td class="td">{{ h.message }}</td>
                <td class="td">{{ formatDate(h.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Edit modal -->
    <div v-if="editOpen" class="overlay">
      <div class="modal">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('clientDetail.editTitle') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onEditSave">
          <div>
            <label class="lbl">{{ t('common.fullName') }} *</label>
            <input v-model="editForm.fullName" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('common.phone') }} *</label>
              <input v-model="editForm.phone" required class="field" />
            </div>
            <div>
              <label class="lbl">{{ t('clientDetail.additionalPhone') }}</label>
              <input v-model="editForm.additionalPhone" class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">{{ t('common.address') }} *</label>
            <input v-model="editForm.address" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('common.inn') }}</label>
              <input v-model="editForm.inn" class="field" />
            </div>
            <div>
              <label class="lbl">{{ t('clientDetail.bank') }}</label>
              <input v-model="editForm.bankName" class="field" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('clientDetail.mfo') }}</label>
              <input v-model="editForm.mfo" class="field" />
            </div>
            <div>
              <label class="lbl">{{ t('clientDetail.accountNumber') }}</label>
              <input v-model="editForm.accountNumber" class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">{{ t('common.note') }}</label>
            <textarea v-model="editForm.description" rows="3" class="field !h-auto py-2" />
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <button type="button" class="ghost-btn" @click="editOpen = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('common.save') }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  Banknote,
  CalendarClock,
  LayoutGrid,
  MessageSquare,
  Package,
  Percent,
  Phone,
  Plus,
  Send,
  Wallet,
} from 'lucide-vue-next'
import VueApexCharts from 'vue3-apexcharts'
import type { ApexOptions } from 'apexcharts'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import { fetchClient, updateClient, type Client, type ClientPayload } from '@/api/clients'
import {
  adjustClientBalance,
  balanceIn,
  fetchClientBalance,
  recalculateClientBalance,
  type ClientBalance,
} from '@/api/clientBalances'
import {
  createClientNote,
  deleteClientNote,
  fetchClientNotes,
  updateReminderStatus,
  type ClientNote,
} from '@/api/clientNotes'
import { fetchSaleOrdersByClient, type SaleOrder } from '@/api/sales'
import { allocatePayments, fetchPaymentsByClient, unallocatePayment, type Payment } from '@/api/payments'
import { useAuthStore } from '@/stores/auth'
import { fetchDebtHistoryByClient, sendDebtSmsToClient, type DebtNotificationHistory } from '@/api/debt'
import { fetchSaleOrderItemsByClient, type SaleOrderItem } from '@/api/saleOrderItems'
import { formatApiError } from '@/api/http'
import { formatDate, money, today } from '@/utils/format'
import { BASE_CURRENCY, CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'
import { phoneDigits } from '@/utils/phone'

type TabKey =
  | 'overview'
  | 'analysis'
  | 'orders'
  | 'items'
  | 'act'
  | 'payments'
  | 'activity'
  | 'notes'
  | 'info'
  | 'sms'

const route = useRoute()
const { t, te } = useI18n()
const auth = useAuthStore()

const tabKeys: TabKey[] = [
  'overview',
  'analysis',
  'orders',
  'items',
  'act',
  'payments',
  'activity',
  'notes',
  'info',
  'sms',
]
const tabs = computed(() =>
  tabKeys.map((key) => ({ key, label: t(`clientDetail.tabs.${key}`) })),
)
const noteTypes = ['CALL', 'MEETING', 'SMS', 'PAYMENT_PROMISE', 'OTHER']
const reminderStatuses = ['NONE', 'PENDING', 'DONE', 'BROKEN']

function enumLabel(prefix: string, value: string) {
  const key = `${prefix}.${value}`
  return te(key) ? t(key) : value
}

const tab = ref<TabKey>('overview')
const loading = ref(true)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const client = ref<Client | null>(null)
const balances = ref<ClientBalance[]>([])
const notes = ref<ClientNote[]>([])
const allSales = ref<SaleOrder[]>([])
const allPayments = ref<Payment[]>([])
const sms = ref<DebtNotificationHistory[]>([])
const allItems = ref<SaleOrderItem[]>([])

/** Sahifa tanlangan valyutada ko'rsatiladi: turli valyutadagi summalar hech qachon qo'shilmaydi. */
const viewCurrency = ref<CurrencyCode>(BASE_CURRENCY)
const clientCurrencies = computed<CurrencyCode[]>(() => {
  const set = new Set<CurrencyCode>([BASE_CURRENCY])
  balances.value.forEach((b) => set.add(b.currency || BASE_CURRENCY))
  allSales.value.forEach((o) => set.add(o.currency || BASE_CURRENCY))
  allPayments.value.forEach((p) => set.add(p.debtCurrency || BASE_CURRENCY))
  return CURRENCIES.filter((c) => set.has(c))
})
const balance = computed(() => balanceIn(balances.value, viewCurrency.value) || null)
const sales = computed(() => allSales.value.filter((o) => (o.currency || BASE_CURRENCY) === viewCurrency.value))
const payments = computed(() =>
  allPayments.value.filter((p) => (p.debtCurrency || BASE_CURRENCY) === viewCurrency.value),
)
const salesById = computed(() => new Map(sales.value.map((o) => [o.id, o])))
/** Pozitsiya tannarxi so'mda saqlanadi - xorijiy buyurtmada buyurtma kursi bo'yicha o'giriladi. */
const items = computed(() =>
  allItems.value
    .filter((it) => it.saleOrderId != null && salesById.value.has(it.saleOrderId))
    .map((it) => {
      const order = salesById.value.get(it.saleOrderId as number)
      const rate = Number(order?.exchangeRate || 0)
      if (viewCurrency.value === BASE_CURRENCY || !(rate > 0) || it.priceCost == null) return it
      return { ...it, priceCost: Math.round((Number(it.priceCost) / rate) * 100) / 100 }
    }),
)

function applied(p: Payment) {
  return Number(p.appliedAmount ?? p.paymentAmount ?? 0)
}

function cashText(p: Payment) {
  const cur = p.currency || BASE_CURRENCY
  return cur === BASE_CURRENCY ? `${money(p.paymentAmount)} ${t('clientDetail.currency')}` : moneyIn(p.paymentAmount, cur)
}
const smsSending = ref(false)
const chartMounted = ref(false)
const editOpen = ref(false)
const saving = ref(false)
const adjPurchase = ref(0)
const adjPaid = ref(0)
const nextPaymentDate = ref('')
const noteForm = reactive({ type: 'CALL', content: '' })
const editForm = reactive<ClientPayload>({
  fullName: '',
  phone: '',
  address: '',
  inn: '',
  additionalPhone: '',
  bankName: '',
  mfo: '',
  accountNumber: '',
  description: '',
})

const warehouseColors = ['#465fff', '#12b76a', '#f79009', '#f04438', '#9b8afb', '#15b79e']

function clientId() {
  return Number(route.params.id)
}

function moneySom(v?: number | null) {
  return viewCurrency.value === BASE_CURRENCY ? `${money(v)} ${t('clientDetail.currency')}` : moneyIn(v, viewCurrency.value)
}

/** Akt sverka jadvali: so'mda faqat raqam (eski ko'rinish), xorijiy valyutada belgisi bilan. */
function stmtMoney(v?: number | null) {
  return viewCurrency.value === BASE_CURRENCY ? money(v) : moneyIn(v, viewCurrency.value)
}

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() || '')
    .join('')
}

function daysAgo(dateStr?: string | null) {
  if (!dateStr) return null
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return null
  return Math.floor((Date.now() - d.getTime()) / 86400000)
}

function paymentKey() {
  return `client-next-payment:${clientId()}`
}

const breadcrumbItems = computed(() => [
  { label: t('nav.home'), to: '/' },
  { label: t('nav.clients'), to: '/clients' },
  { label: client.value?.fullName || t('common.client') },
])

const totalSales = computed(() => Number(balance.value?.totalPurchase ?? 0))
const totalPaid = computed(() => Number(balance.value?.totalPaid ?? 0))
const totalDebt = computed(() => Number(balance.value?.totalDebt ?? 0))

const canAllocate = computed(() => auth.can('PAYMENT_CREATE') || auth.can('PAYMENT_EDIT'))
const selectedPaymentIds = ref<number[]>([])
const selectedOrderIds = ref<number[]>([])
const allocationAmounts = reactive<Record<number, number>>({})
const allocating = ref(false)
const allocationMessage = ref('')

function byDateAsc<T extends { id: number }>(date: (x: T) => string | undefined) {
  return (a: T, b: T) => String(date(a) || '').localeCompare(String(date(b) || '')) || a.id - b.id
}

const unallocatedPayments = computed(() =>
  payments.value.filter((p) => !p.saleOrderId).sort(byDateAsc((p) => p.paymentDate)),
)
const unallocatedTotal = computed(() =>
  unallocatedPayments.value.reduce((s, p) => s + applied(p), 0),
)
const debtOrders = computed(() =>
  sales.value
    .filter((o) => o.orderStatus !== 'CANCELLED' && Number(o.debtSum || 0) > 0)
    .sort(byDateAsc((o) => o.orderDate)),
)
const allPaymentsSelected = computed(
  () => unallocatedPayments.value.length > 0 && selectedPaymentIds.value.length === unallocatedPayments.value.length,
)
const selectedPool = computed(() =>
  unallocatedPayments.value
    .filter((p) => selectedPaymentIds.value.includes(p.id))
    .reduce((s, p) => s + applied(p), 0),
)
const allocatingTotal = computed(() =>
  selectedOrderIds.value.reduce((s, id) => s + Number(allocationAmounts[id] || 0), 0),
)
const allocationLeft = computed(() => Math.round((selectedPool.value - allocatingTotal.value) * 100) / 100)

const allocationError = computed(() => {
  if (!selectedPaymentIds.value.length) return t('clientDetail.allocation.selectPayments')
  if (allocationLeft.value < 0) return t('clientDetail.allocation.exceedsPool')
  for (const id of selectedOrderIds.value) {
    const order = debtOrders.value.find((o) => o.id === id)
    const amount = Number(allocationAmounts[id] || 0)
    if (amount <= 0) return t('clientDetail.allocation.amountRequired', { id })
    if (order && amount > Number(order.debtSum || 0) + 0.005) return t('clientDetail.allocation.exceedsDebt', { id })
  }
  return ''
})

function toggleAllPayments(checked: boolean) {
  selectedPaymentIds.value = checked ? unallocatedPayments.value.map((p) => p.id) : []
}

function toggleOrder(order: SaleOrder, checked: boolean) {
  allocationMessage.value = ''
  if (!checked) {
    selectedOrderIds.value = selectedOrderIds.value.filter((id) => id !== order.id)
    delete allocationAmounts[order.id]
    return
  }
  selectedOrderIds.value = [...selectedOrderIds.value, order.id]
  const suggested = Math.min(Number(order.debtSum || 0), Math.max(0, allocationLeft.value))
  allocationAmounts[order.id] = Math.round(suggested * 100) / 100
}

function resetAllocation() {
  selectedPaymentIds.value = []
  selectedOrderIds.value = []
  for (const key of Object.keys(allocationAmounts)) delete allocationAmounts[Number(key)]
}

async function onAllocate() {
  if (allocationError.value || allocatingTotal.value <= 0) return
  allocating.value = true
  allocationMessage.value = ''
  error.value = null
  try {
    await allocatePayments({
      clientId: clientId(),
      paymentIds: [...selectedPaymentIds.value],
      allocations: selectedOrderIds.value.map((id) => ({ saleOrderId: id, amount: Number(allocationAmounts[id]) })),
    })
    const total = allocatingTotal.value
    resetAllocation()
    await load()
    allocationMessage.value = t('clientDetail.allocation.success', { amount: moneySom(total) })
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    allocating.value = false
  }
}

async function onUnallocate(p: Payment) {
  if (!confirm(t('clientDetail.allocation.unallocateConfirm', { id: p.id, order: p.saleOrderId }))) return
  error.value = null
  try {
    await unallocatePayment(p.id)
    resetAllocation()
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

type StatementRow = {
  key: string
  date: string
  title: string
  badge?: string
  /** Boshqa valyutada to'langan bo'lsa, kassaga tushgan summa. */
  cashNote?: string
  orderId?: number
  debit: number
  credit: number
  balance: number
}

const statementRows = computed<StatementRow[]>(() => {
  const entries: Omit<StatementRow, 'balance'>[] = []
  for (const o of sales.value) {
    if (o.orderStatus === 'CANCELLED') continue
    entries.push({
      key: `o${o.id}`,
      date: o.orderDate || '',
      title: t('clientDetail.statement.order', { id: o.id }),
      orderId: o.id,
      debit: Number(o.totalSum || 0),
      credit: 0,
    })
  }
  for (const p of payments.value) {
    entries.push({
      key: `p${p.id}`,
      date: p.paymentDate || '',
      title: p.saleOrderId
        ? t('clientDetail.statement.paymentFor', { id: p.id, type: p.paymentTypeName || '—', order: p.saleOrderId })
        : t('clientDetail.statement.payment', { id: p.id, type: p.paymentTypeName || '—' }),
      badge: p.saleOrderId ? undefined : t('clientDetail.allocation.unallocatedShort'),
      cashNote: (p.currency || BASE_CURRENCY) !== viewCurrency.value ? cashText(p) : undefined,
      debit: 0,
      credit: applied(p),
    })
  }
  entries.sort((a, b) => a.date.localeCompare(b.date) || b.debit - a.debit)
  let balanceRun = 0
  return entries.map((e) => {
    balanceRun += e.debit - e.credit
    return { ...e, balance: Math.round(balanceRun * 100) / 100 }
  })
})

const statementTotals = computed(() =>
  statementRows.value.reduce(
    (acc, r) => ({ debit: acc.debit + r.debit, credit: acc.credit + r.credit }),
    { debit: 0, credit: 0 },
  ),
)

const itemsCost = computed(() =>
  items.value.reduce((s, it) => s + Number(it.priceCost || 0) * Number(it.count || 1), 0),
)
const itemsSales = computed(() =>
  items.value.reduce((s, it) => s + Number(it.priceSelling || 0) * Number(it.count || 1), 0),
)
const totalProfit = computed(() => itemsSales.value - itemsCost.value)
const marginPct = computed(() =>
  itemsSales.value > 0 ? (totalProfit.value / itemsSales.value) * 100 : 0,
)
const avgCheck = computed(() =>
  sales.value.length ? totalSales.value / sales.value.length : 0,
)
const windowItemsCount = computed(
  () => items.value.filter((it) => it.width != null && it.height != null).length,
)

const lastActivityDate = computed(() => {
  const dates = [
    ...sales.value.map((o) => o.orderDate),
    ...payments.value.map((p) => p.paymentDate),
    client.value?.updatedAt,
  ].filter(Boolean) as string[]
  if (!dates.length) return null
  return dates.sort().at(-1) || null
})

const lastActivityLabel = computed(() => {
  const d = lastActivityDate.value
  if (!d) return '—'
  const raw = formatDate(d).slice(0, 10)
  const m = raw.match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[3]}-${m[2]}-${m[1]}` : raw
})

const lastActivityRelative = computed(() => {
  const days = daysAgo(lastActivityDate.value)
  if (days == null) return t('clientDetail.noActivity')
  if (days === 0) return t('clientDetail.activeToday')
  if (days === 1) return t('clientDetail.activeDaysAgo', { n: 1 })
  return t('clientDetail.activeDaysAgo', { n: days })
})

const debtAgeLabel = computed(() => {
  const debtOrders = sales.value.filter((o) => Number(o.debtSum || 0) > 0)
  if (!debtOrders.length) return t('clientDetail.noDebt')
  const ages = debtOrders.map((o) => daysAgo(o.orderDate)).filter((d): d is number => d != null)
  if (!ages.length) return t('clientDetail.debtAge')
  return t('clientDetail.debtAgeDays', { n: Math.max(...ages) })
})

const debtSegments = computed(() => {
  const buckets = [
    { key: '0-30', label: t('clientDetail.daysRange', { range: '0-30' }), color: '#12b76a', min: 0, max: 30 },
    { key: '31-60', label: t('clientDetail.daysRange', { range: '31-60' }), color: '#f79009', min: 31, max: 60 },
    { key: '61-90', label: t('clientDetail.daysRange', { range: '61-90' }), color: '#fb6514', min: 61, max: 90 },
    { key: '90+', label: t('clientDetail.daysRange', { range: '90+' }), color: '#f04438', min: 91, max: Infinity },
  ]
  const result = buckets.map((b) => ({ ...b, amount: 0, count: 0, pct: 0 }))
  for (const o of sales.value) {
    const debt = Number(o.debtSum || 0)
    if (debt <= 0) continue
    const age = daysAgo(o.orderDate) ?? 0
    const bucket = result.find((b) => age >= b.min && age <= b.max) || result[0]
    bucket.amount += debt
    bucket.count += 1
  }
  const total = result.reduce((s, b) => s + b.amount, 0)
  for (const b of result) {
    b.pct = total > 0 ? Math.round((b.amount / total) * 100) : 0
  }
  return result
})

const sellers = computed(() => {
  const map = new Map<string, { key: string; name: string; count: number; sum: number }>()
  for (const o of sales.value) {
    const key = String(o.userId ?? o.userFullName ?? 'unknown')
    const name = o.userFullName || t('clientDetail.userFallback', { id: o.userId || '?' })
    const cur = map.get(key) || { key, name, count: 0, sum: 0 }
    cur.count += 1
    cur.sum += Number(o.totalSum || 0)
    map.set(key, cur)
  }
  const list = [...map.values()].sort((a, b) => b.sum - a.sum)
  const total = list.reduce((s, x) => s + x.sum, 0)
  return list.map((x) => ({
    ...x,
    share: total > 0 ? Math.round((x.sum / total) * 100) : 0,
  }))
})

const warehouses = computed(() => {
  const map = new Map<string, { name: string; sum: number }>()
  for (const o of sales.value) {
    const name = o.warehouseName || t('clientDetail.unknown')
    const cur = map.get(name) || { name, sum: 0 }
    cur.sum += Number(o.totalSum || 0)
    map.set(name, cur)
  }
  const list = [...map.values()].sort((a, b) => b.sum - a.sum)
  const total = list.reduce((s, x) => s + x.sum, 0)
  return list.map((x) => ({
    ...x,
    share: total > 0 ? Math.round((x.sum / total) * 100) : 0,
  }))
})

const warehouseSeries = computed(() => warehouses.value.map((w) => w.sum))

const warehouseChartOptions = computed<ApexOptions>(() => ({
  chart: { type: 'donut', fontFamily: 'Outfit, sans-serif' },
  labels: warehouses.value.map((w) => w.name),
  colors: warehouseColors,
  legend: { show: false },
  dataLabels: { enabled: false },
  plotOptions: {
    pie: {
      donut: {
        size: '70%',
      },
    },
  },
  tooltip: {
    y: {
      formatter: (val: number) => moneySom(val),
    },
  },
}))

const activityEvents = computed(() => {
  const events: { key: string; title: string; meta: string; color: string; at: string }[] = []
  for (const o of sales.value) {
    events.push({
      key: `o-${o.id}`,
      title: t('clientDetail.orderN', { id: o.id }),
      meta: `${moneySom(o.totalSum)} · ${formatDate(o.orderDate)}`,
      color: '#465fff',
      at: o.orderDate || '',
    })
  }
  for (const p of payments.value) {
    events.push({
      key: `p-${p.id}`,
      title: t('clientDetail.paymentN', { id: p.id }),
      meta: `${cashText(p)} · ${formatDate(p.paymentDate)}`,
      color: '#12b76a',
      at: p.paymentDate || '',
    })
  }
  for (const n of notes.value) {
    events.push({
      key: `n-${n.id}`,
      title: t('clientDetail.noteN', { type: n.type ? enumLabel('clientDetail.noteTypes', n.type) : '' }),
      meta: n.content || '',
      color: '#f79009',
      at: n.interactionDate || n.reminderDate || '',
    })
  }
  return events.sort((a, b) => (b.at || '').localeCompare(a.at || '')).slice(0, 30)
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const id = clientId()
    const [c, b, n, s, p, h] = await Promise.all([
      fetchClient(id),
      fetchClientBalance(id, { fromDate: '2000-01-01', toDate: today() }),
      fetchClientNotes(id),
      fetchSaleOrdersByClient(id, 0, 200),
      fetchPaymentsByClient(id, 0, 200),
      fetchDebtHistoryByClient(id, 0, 100).catch(() => ({ data: { content: [] as DebtNotificationHistory[] } })),
    ])
    client.value = c.data
    balances.value = b.data || []
    notes.value = n.data || []
    allSales.value = s.data?.content || []
    allPayments.value = p.data?.content || []
    sms.value = h.data?.content || []
    if (!clientCurrencies.value.includes(viewCurrency.value)) viewCurrency.value = BASE_CURRENCY
    syncAdjustForm()
    nextPaymentDate.value = localStorage.getItem(paymentKey()) || ''

    try {
      const it = await fetchSaleOrderItemsByClient(id, 0, 500)
      allItems.value = it.data?.content || []
    } catch {
      allItems.value = []
    }
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
    await nextTick()
    chartMounted.value = true
  }
}

function saveNextPayment() {
  if (nextPaymentDate.value) localStorage.setItem(paymentKey(), nextPaymentDate.value)
  else localStorage.removeItem(paymentKey())
}

function clearNextPayment() {
  nextPaymentDate.value = ''
  localStorage.removeItem(paymentKey())
}

async function onSendSms() {
  if (!confirm(t('clientDetail.sendSmsConfirm'))) return
  smsSending.value = true
  try {
    await sendDebtSmsToClient(clientId())
    const h = await fetchDebtHistoryByClient(clientId(), 0, 100)
    sms.value = h.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    smsSending.value = false
  }
}

function syncAdjustForm() {
  adjPurchase.value = Number(balance.value?.totalPurchase || 0)
  adjPaid.value = Number(balance.value?.totalPaid || 0)
}

watch(viewCurrency, () => {
  resetAllocation()
  syncAdjustForm()
})

async function onRecalc() {
  try {
    const res = await recalculateClientBalance(clientId())
    balances.value = res.data || []
    syncAdjustForm()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onAdjust() {
  try {
    const res = await adjustClientBalance(clientId(), {
      totalPurchase: adjPurchase.value,
      totalPaid: adjPaid.value,
      currency: viewCurrency.value,
    })
    const updated = res.data
    if (updated) {
      const cur = updated.currency || BASE_CURRENCY
      balances.value = [...balances.value.filter((b) => (b.currency || BASE_CURRENCY) !== cur), updated]
    }
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onNoteSave() {
  try {
    await createClientNote({
      clientId: clientId(),
      type: noteForm.type,
      content: noteForm.content,
    })
    noteForm.content = ''
    notes.value = (await fetchClientNotes(clientId())).data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onReminder(noteId: number, status: string) {
  try {
    await updateReminderStatus(noteId, status)
    notes.value = (await fetchClientNotes(clientId())).data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onNoteDelete(noteId: number) {
  if (!confirm(t('clientDetail.deleteNoteConfirm'))) return
  try {
    await deleteClientNote(noteId)
    notes.value = (await fetchClientNotes(clientId())).data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function openEdit() {
  if (!client.value) return
  Object.assign(editForm, {
    fullName: client.value.fullName || '',
    phone: client.value.phone || '',
    address: client.value.address || '',
    inn: client.value.inn || '',
    additionalPhone: client.value.additionalPhone || '',
    bankName: client.value.bankName || '',
    mfo: client.value.mfo || '',
    accountNumber: client.value.accountNumber || '',
    description: client.value.description || '',
    clientGroupId: client.value.clientGroupId ?? null,
  })
  formError.value = null
  editOpen.value = true
}

async function onEditSave() {
  saving.value = true
  formError.value = null
  try {
    const res = await updateClient(clientId(), { ...editForm })
    client.value = res.data
    editOpen.value = false
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    chartMounted.value = false
    load()
  },
)

onMounted(() => {
  const requestedTab = route.query.tab
  if (typeof requestedTab === 'string' && tabKeys.includes(requestedTab as TabKey)) {
    tab.value = requestedTab as TabKey
  }
  load()
})
</script>

<style scoped>
.card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}
.dark .card {
  border-color: #1f2937;
  background: rgb(255 255 255 / 0.03);
}
.group-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  background: #ecfdf5;
  padding: 0.15rem 0.6rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #059669;
}
.btn {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.4rem;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
}
.ghost-btn {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.4rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: #fff;
  padding: 0 0.875rem;
  font-size: 0.875rem;
  color: #374151;
}
.dark .ghost-btn {
  border-color: #4b5563;
  background: transparent;
  color: #e5e7eb;
}
.ghost-btn:disabled {
  opacity: 0.5;
}
.tabs-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  border-radius: 0.85rem;
  background: #f3f4f6;
  padding: 0.35rem;
}
.dark .tabs-bar {
  background: #111827;
}
.tab-pill {
  border-radius: 0.6rem;
  padding: 0.5rem 0.85rem;
  font-size: 0.8125rem;
  color: #6b7280;
  white-space: nowrap;
}
.tab-pill.active {
  background: #fff;
  color: #1f2937;
  font-weight: 600;
  box-shadow: 0 1px 2px rgb(0 0 0 / 0.06);
}
.dark .tab-pill.active {
  background: #1f2937;
  color: #fff;
}
.cur-switch {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}
.cur-pill {
  border-radius: 9999px;
  border: 1px solid #e5e7eb;
  padding: 0.3rem 0.85rem;
  font-size: 0.8125rem;
  color: #4b5563;
}
.cur-pill.active {
  border-color: #465fff;
  background: #465fff;
  color: #fff;
  font-weight: 600;
}
.dark .cur-pill {
  border-color: #374151;
  color: #d1d5db;
}
.dark .cur-pill.active {
  border-color: #465fff;
  background: #465fff;
  color: #fff;
}
.kpi-card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 1.1rem 1.25rem;
  border-bottom-width: 3px;
}
.dark .kpi-card {
  border-color: #1f2937;
  background: rgb(255 255 255 / 0.03);
}
.kpi-red {
  border-bottom-color: #f04438;
}
.kpi-green {
  border-bottom-color: #12b76a;
}
.kpi-blue {
  border-bottom-color: #465fff;
}
.kpi-yellow {
  border-bottom-color: #f79009;
}
.kpi-label {
  font-size: 0.875rem;
  color: #6b7280;
}
.kpi-value {
  margin-top: 0.35rem;
  font-size: 1.35rem;
  font-weight: 700;
  color: #1f2937;
}
.dark .kpi-value {
  color: rgb(255 255 255 / 0.9);
}
.kpi-sub {
  margin-top: 0.25rem;
  font-size: 0.75rem;
  color: #9ca3af;
}
.kpi-icon {
  display: inline-flex;
  height: 2.5rem;
  width: 2.5rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
}
.debt-bar {
  display: flex;
  height: 0.7rem;
  overflow: hidden;
  border-radius: 9999px;
  background: #f3f4f6;
}
.debt-bar-seg {
  height: 100%;
  min-width: 0;
}
.debt-seg-card {
  border-radius: 0.85rem;
  border: 1px solid #e5e7eb;
  border-top-width: 3px;
  border-top-color: var(--accent);
  padding: 0.9rem 1rem;
}
.dark .debt-seg-card {
  border-color: #1f2937;
}
.metric-box {
  border-radius: 0.75rem;
  background: #f9fafb;
  padding: 0.85rem 1rem;
}
.dark .metric-box {
  background: rgb(255 255 255 / 0.04);
}
.alloc-box {
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  max-height: 22rem;
  overflow-y: auto;
}
.alloc-head {
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 0.6rem 1rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #374151;
}
.alloc-box .td {
  padding: 0.55rem 1rem;
}
.th.text-end,
.td.text-end {
  text-align: end;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.dark .alloc-box {
  border-color: #1f2937;
}
.dark .alloc-head {
  border-color: #1f2937;
  background: #111827;
  color: #d1d5db;
}
.status-pill {
  display: inline-flex;
  border-radius: 9999px;
  background: #f3f4f6;
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
}
.status-pill.status-set {
  background: #ecfdf5;
  color: #059669;
}
.count-pill {
  display: inline-flex;
  border-radius: 9999px;
  background: #eef2ff;
  padding: 0.25rem 0.7rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #465fff;
  white-space: nowrap;
}
.avatar {
  display: inline-flex;
  height: 1.75rem;
  width: 1.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: #eef2ff;
  font-size: 0.65rem;
  font-weight: 700;
  color: #465fff;
}
.th {
  padding: 0.75rem 1.25rem;
  text-align: left;
  font-size: 0.75rem;
  color: #6b7280;
}
.td {
  padding: 0.75rem 1.25rem;
  font-size: 0.875rem;
  color: #4b5563;
}
.dark .td {
  color: #d1d5db;
}
.empty {
  padding: 2rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
.empty-block {
  padding: 3rem;
  text-align: center;
  color: #6b7280;
}
.lbl {
  display: block;
  font-size: 0.75rem;
  color: #6b7280;
}
.info-val {
  margin-top: 0.2rem;
  font-size: 0.925rem;
  font-weight: 500;
  color: #1f2937;
}
.dark .info-val {
  color: rgb(255 255 255 / 0.9);
}
.field {
  height: 2.5rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: #fff;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  color: #374151;
}
.dark .field {
  border-color: #374151;
  background: #111827;
  color: #e5e7eb;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
}
.overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(0 0 0 / 0.4);
  padding: 1rem;
}
.modal {
  width: 100%;
  max-width: 32rem;
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 1.25rem;
}
.dark .modal {
  border-color: #1f2937;
  background: #111827;
}
</style>
