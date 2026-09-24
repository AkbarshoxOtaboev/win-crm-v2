<template>
  <AdminLayout>
    <PageBreadcrumb
      :pageTitle="client?.fullName || 'Mijoz'"
      :items="breadcrumbItems"
    />

    <div class="mb-4">
      <router-link to="/clients" class="text-sm text-brand-500 hover:underline">
        ← Mijozlar ro'yxati
      </router-link>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="loading" class="empty-block">Yuklanmoqda...</div>

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
              <span v-else class="group-badge">Oddiy</span>
            </div>
            <p class="mt-1 text-sm text-gray-500">
              <span :class="client.status === 'DISABLED' ? 'text-warning-600' : 'text-success-600'">
                {{ client.status === 'DISABLED' ? 'Nofaol' : 'Faol' }}
              </span>
              <span class="mx-1.5 text-gray-300">·</span>
              Oxirgi faollik: {{ lastActivityLabel }}
            </p>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <router-link :to="`/sales/create?clientId=${client.id}`" class="btn">
              <Plus class="h-4 w-4" />
              Yangi buyurtma
            </router-link>
            <router-link :to="`/payments?clientId=${client.id}`" class="ghost-btn">
              <Wallet class="h-4 w-4" />
              To'lov qilish
            </router-link>
            <button type="button" class="ghost-btn" :disabled="smsSending" @click="onSendSms">
              <MessageSquare class="h-4 w-4" />
              Qarz haqida SMS yuborish
            </button>
            <a
              v-if="client.phone"
              class="ghost-btn"
              :href="`https://t.me/+${phoneDigits(client.phone)}`"
              target="_blank"
              rel="noopener"
            >
              <Send class="h-4 w-4" />
              Telegram yuborish
            </a>
            <a v-if="client.phone" class="ghost-btn" :href="`tel:${client.phone}`">
              <Phone class="h-4 w-4" />
              Qo'ng'iroq
            </a>
          </div>
        </div>

        <div class="mt-6 grid grid-cols-1 gap-4 border-t border-gray-100 pt-5 sm:grid-cols-3 dark:border-gray-800">
          <div>
            <p class="text-sm text-gray-500">Jami sotuv</p>
            <p class="mt-1 text-2xl font-bold text-brand-500">{{ moneySom(totalSales) }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-500">Jami to'langan</p>
            <p class="mt-1 text-2xl font-bold text-success-600">{{ moneySom(totalPaid) }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-500">Jami qarz</p>
            <p class="mt-1 text-2xl font-bold text-error-600">{{ moneySom(totalDebt) }}</p>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs-bar mb-4">
        <button
          v-for="t in tabs"
          :key="t.key"
          type="button"
          class="tab-pill"
          :class="{ active: tab === t.key }"
          @click="tab = t.key"
        >
          {{ t.label }}
        </button>
      </div>

      <!-- Umumiy -->
      <div v-show="tab === 'overview'" class="space-y-4">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <article class="kpi-card kpi-red">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">Jami qarz</p>
                <p class="kpi-value text-error-600">{{ moneySom(totalDebt) }}</p>
                <p class="kpi-sub">{{ debtAgeLabel }}</p>
              </div>
              <span class="kpi-icon bg-error-50 text-error-500"><Wallet class="h-5 w-5" /></span>
            </div>
          </article>
          <article class="kpi-card kpi-green">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">Marja</p>
                <p class="kpi-value text-brand-500">{{ marginPct.toFixed(1) }}%</p>
                <p class="kpi-sub">{{ moneySom(totalProfit) }}</p>
              </div>
              <span class="kpi-icon bg-success-50 text-success-600"><Percent class="h-5 w-5" /></span>
            </div>
          </article>
          <article class="kpi-card kpi-blue">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">Buyurtmalar</p>
                <p class="kpi-value">{{ sales.length }}</p>
                <p class="kpi-sub">{{ lastActivityRelative }}</p>
              </div>
              <span class="kpi-icon bg-blue-light-50 text-blue-light-500"><Package class="h-5 w-5" /></span>
            </div>
          </article>
          <article class="kpi-card kpi-yellow">
            <div class="flex items-start justify-between">
              <div>
                <p class="kpi-label">O'rtacha chek</p>
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
              <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">Qarz yoshi</h4>
              <p class="mt-0.5 text-sm text-gray-500">Kechikkan qarz segmentlari bo'yicha taqsimot</p>
            </div>
            <div class="text-right">
              <p class="text-sm text-gray-500">Jami qarz</p>
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
                <span class="text-gray-500">{{ seg.count }} ta</span>
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
            <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">Foyda va marja</h4>
            <p class="mt-0.5 text-sm text-gray-500">Tannarx va sotuv narxi bo'yicha foyda tahlili</p>
            <div class="mt-4 grid grid-cols-2 gap-3">
              <div class="metric-box">
                <p class="text-xs text-gray-500">Jami sotuv</p>
                <p class="mt-1 font-semibold text-brand-500">{{ moneySom(itemsSales) }}</p>
              </div>
              <div class="metric-box">
                <p class="text-xs text-gray-500">Jami tannarx</p>
                <p class="mt-1 font-semibold text-gray-800 dark:text-white/90">{{ moneySom(itemsCost) }}</p>
              </div>
              <div class="metric-box">
                <p class="text-xs text-gray-500">Jami foyda</p>
                <p class="mt-1 font-semibold text-success-600">{{ moneySom(totalProfit) }}</p>
              </div>
              <div class="metric-box">
                <p class="text-xs text-gray-500">Marja</p>
                <p class="mt-1 font-semibold text-gray-800 dark:text-white/90">{{ marginPct.toFixed(1) }}%</p>
              </div>
            </div>
            <div v-if="items.length === 0" class="mt-6 py-8 text-center text-sm text-gray-400">Topilmadi</div>
          </div>

          <div class="card flex flex-col items-center justify-center p-8 text-center">
            <LayoutGrid class="mb-3 h-10 w-10 text-gray-300" />
            <h4 class="text-base font-semibold text-gray-800 dark:text-white/90">Oyna statistikasi</h4>
            <p class="mt-1 text-sm text-gray-500">
              {{
                windowItemsCount > 0
                  ? `${windowItemsCount} ta oyna pozitsiyasi`
                  : "Bu mijozda hali oyna buyurtmalari yo'q"
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
                <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">Keyingi to'lov sanasi</h4>
                <p class="mt-0.5 text-sm text-gray-500">Keyingi to'lov sanasini belgilang va kuzatib boring</p>
              </div>
            </div>
            <span class="status-pill" :class="nextPaymentDate ? 'status-set' : ''">
              {{ nextPaymentDate ? formatDate(nextPaymentDate) : 'Belgilanmagan' }}
            </span>
          </div>
          <label class="lbl">Keyingi to'lov sanasi</label>
          <div class="mt-1 flex flex-wrap gap-2">
            <input v-model="nextPaymentDate" type="date" class="field max-w-xs" @change="saveNextPayment" />
            <button type="button" class="ghost-btn" @click="clearNextPayment">Tozalash</button>
          </div>
        </div>

        <!-- Sotuvchilar / Ombor -->
        <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
          <div class="card p-5">
            <div class="mb-4 flex items-start justify-between gap-2">
              <div>
                <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">Sotuvchilar bo'yicha</h4>
                <p class="mt-0.5 text-sm text-gray-500">Mijoz bilan ishlagan sotuvchilar va ularning ulushi</p>
              </div>
              <span class="count-pill">{{ sellers.length }} ta sotuvchi</span>
            </div>
            <div class="overflow-x-auto">
              <table class="min-w-full">
                <thead>
                  <tr class="border-b border-gray-100 dark:border-gray-800">
                    <th class="th">#</th>
                    <th class="th">Sotuvchi</th>
                    <th class="th">Buyurtmalar</th>
                    <th class="th">Jami sotuv</th>
                    <th class="th">Ulush</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="sellers.length === 0">
                    <td colspan="5" class="empty">Sotuvchi yo'q</td>
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
            <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">Ombor bo'yicha taqsimot</h4>
            <p class="mt-0.5 text-sm text-gray-500">Qaysi ombordan ko'proq sotib olingan</p>
            <div v-if="warehouseSeries.length === 0" class="py-12 text-center text-sm text-gray-400">
              Ma'lumot yo'q
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
        <h4 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">Tahlil</h4>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div class="metric-box">
            <p class="text-xs text-gray-500">Jami sotuv</p>
            <p class="mt-1 text-lg font-bold text-brand-500">{{ moneySom(totalSales) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">To'langan</p>
            <p class="mt-1 text-lg font-bold text-success-600">{{ moneySom(totalPaid) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">Qarz</p>
            <p class="mt-1 text-lg font-bold text-error-600">{{ moneySom(totalDebt) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">Marja</p>
            <p class="mt-1 text-lg font-bold">{{ marginPct.toFixed(1) }}%</p>
          </div>
        </div>
        <div class="mt-5">
          <h5 class="mb-3 font-medium text-gray-700 dark:text-gray-200">Qarz yoshi</h5>
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
                <th class="th">Sana</th>
                <th class="th">Ombor</th>
                <th class="th">Sotuvchi</th>
                <th class="th">Jami</th>
                <th class="th">To'langan</th>
                <th class="th">Qarz</th>
                <th class="th">Holat</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="sales.length === 0"><td colspan="8" class="empty">Buyurtma yo'q</td></tr>
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
                <td class="td">{{ o.orderStatus || '—' }}</td>
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
                <th class="th">Tovar</th>
                <th class="th">Ombor</th>
                <th class="th">Soni</th>
                <th class="th">Tannarx</th>
                <th class="th">Sotuv</th>
                <th class="th">Buyurtma</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="items.length === 0"><td colspan="7" class="empty">Pozitsiya yo'q</td></tr>
              <tr
                v-for="it in items"
                :key="it.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ it.id }}</td>
                <td class="td">{{ it.goodsName || '—' }}</td>
                <td class="td">{{ it.warehouseName || '—' }}</td>
                <td class="td">{{ it.count ?? '—' }}</td>
                <td class="td">{{ moneySom(it.priceCost) }}</td>
                <td class="td">{{ moneySom(it.priceSelling) }}</td>
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
      <div v-show="tab === 'act'" class="card p-5">
        <h4 class="mb-1 text-lg font-semibold text-gray-800 dark:text-white/90">Akt sverka (Hisob-kitob)</h4>
        <p class="mb-4 text-sm text-gray-500">Sotuv va to'lovlar bo'yicha qisqa hisobot</p>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div class="metric-box">
            <p class="text-xs text-gray-500">Debet (sotuv)</p>
            <p class="mt-1 text-lg font-bold text-brand-500">{{ moneySom(totalSales) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">Kredit (to'lov)</p>
            <p class="mt-1 text-lg font-bold text-success-600">{{ moneySom(totalPaid) }}</p>
          </div>
          <div class="metric-box">
            <p class="text-xs text-gray-500">Qoldiq (qarz)</p>
            <p class="mt-1 text-lg font-bold text-error-600">{{ moneySom(totalDebt) }}</p>
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
                <th class="th">Summa</th>
                <th class="th">Tur</th>
                <th class="th">Buyurtma</th>
                <th class="th">Sana</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="payments.length === 0"><td colspan="5" class="empty">To'lov yo'q</td></tr>
              <tr
                v-for="p in payments"
                :key="p.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ p.id }}</td>
                <td class="td font-semibold text-success-600">{{ moneySom(p.paymentAmount) }}</td>
                <td class="td">{{ p.paymentTypeName || '—' }}</td>
                <td class="td">
                  <router-link
                    v-if="p.saleOrderId"
                    :to="`/sales/${p.saleOrderId}`"
                    class="text-brand-500"
                  >
                    #{{ p.saleOrderId }}
                  </router-link>
                  <span v-else>—</span>
                </td>
                <td class="td">{{ formatDate(p.paymentDate) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Faollik -->
      <div v-show="tab === 'activity'" class="card p-5">
        <h4 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">Faollik</h4>
        <ul class="space-y-3">
          <li v-for="ev in activityEvents" :key="ev.key" class="flex gap-3 text-sm">
            <span class="mt-1.5 h-2 w-2 shrink-0 rounded-full" :style="{ background: ev.color }" />
            <div>
              <p class="font-medium text-gray-800 dark:text-white/90">{{ ev.title }}</p>
              <p class="text-gray-500">{{ ev.meta }}</p>
            </div>
          </li>
          <li v-if="activityEvents.length === 0" class="text-sm text-gray-400">Faollik yo'q</li>
        </ul>
      </div>

      <!-- Eslatmalar -->
      <div v-show="tab === 'notes'" class="card">
        <div class="border-b border-gray-100 p-4 dark:border-gray-800">
          <form class="grid gap-2 md:grid-cols-4" @submit.prevent="onNoteSave">
            <select v-model="noteForm.type" class="field">
              <option>CALL</option>
              <option>MEETING</option>
              <option>SMS</option>
              <option>PAYMENT_PROMISE</option>
              <option>OTHER</option>
            </select>
            <input v-model="noteForm.content" required class="field md:col-span-2" placeholder="Mazmun" />
            <button type="submit" class="btn justify-center">Qo'shish</button>
          </form>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">Tur</th>
                <th class="th">Mazmun</th>
                <th class="th">Status</th>
                <th class="th text-right">Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="notes.length === 0"><td colspan="4" class="empty">Izoh yo'q</td></tr>
              <tr
                v-for="n in notes"
                :key="n.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ n.type }}</td>
                <td class="td">{{ n.content }}</td>
                <td class="td">
                  <select
                    :value="n.reminderStatus"
                    class="field"
                    @change="onReminder(n.id, ($event.target as HTMLSelectElement).value)"
                  >
                    <option>NONE</option>
                    <option>PENDING</option>
                    <option>DONE</option>
                    <option>BROKEN</option>
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
          <h4 class="text-lg font-semibold text-gray-800 dark:text-white/90">Mijoz ma'lumotlari</h4>
          <button type="button" class="ghost-btn" @click="openEdit">Tahrirlash</button>
        </div>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div><p class="lbl">F.I.Sh</p><p class="info-val">{{ client.fullName }}</p></div>
          <div><p class="lbl">Telefon</p><p class="info-val">{{ client.phone || '—' }}</p></div>
          <div><p class="lbl">Qo'shimcha tel</p><p class="info-val">{{ client.additionalPhone || '—' }}</p></div>
          <div><p class="lbl">Manzil</p><p class="info-val">{{ client.address || '—' }}</p></div>
          <div><p class="lbl">Guruh</p><p class="info-val">{{ client.clientGroupName || '—' }}</p></div>
          <div><p class="lbl">INN</p><p class="info-val">{{ client.inn || '—' }}</p></div>
          <div><p class="lbl">Bank</p><p class="info-val">{{ client.bankName || '—' }}</p></div>
          <div><p class="lbl">MFO</p><p class="info-val">{{ client.mfo || '—' }}</p></div>
          <div><p class="lbl">Hisob raqam</p><p class="info-val">{{ client.accountNumber || '—' }}</p></div>
          <div class="sm:col-span-2 lg:col-span-3">
            <p class="lbl">Izoh</p>
            <p class="info-val">{{ client.description || '—' }}</p>
          </div>
        </div>

        <div class="mt-6 border-t border-gray-100 pt-4 dark:border-gray-800">
          <h5 class="mb-3 font-medium text-gray-700">Balansni sozlash</h5>
          <div class="flex flex-wrap items-end gap-2">
            <button type="button" class="btn" @click="onRecalc">Qayta hisoblash</button>
            <form class="flex flex-wrap gap-2" @submit.prevent="onAdjust">
              <input v-model.number="adjPurchase" type="number" class="field w-32" placeholder="Sotuv" />
              <input v-model.number="adjPaid" type="number" class="field w-32" placeholder="To'lov" />
              <button type="submit" class="ghost-btn">Adjust</button>
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
                <th class="th">Status</th>
                <th class="th">Xabar</th>
                <th class="th">Sana</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="sms.length === 0"><td colspan="4" class="empty">SMS yo'q</td></tr>
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
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">Mijozni tahrirlash</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onEditSave">
          <div>
            <label class="lbl">F.I.Sh *</label>
            <input v-model="editForm.fullName" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">Telefon *</label>
              <input v-model="editForm.phone" required class="field" />
            </div>
            <div>
              <label class="lbl">Qo'shimcha tel</label>
              <input v-model="editForm.additionalPhone" class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">Manzil *</label>
            <input v-model="editForm.address" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">INN</label>
              <input v-model="editForm.inn" class="field" />
            </div>
            <div>
              <label class="lbl">Bank</label>
              <input v-model="editForm.bankName" class="field" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">MFO</label>
              <input v-model="editForm.mfo" class="field" />
            </div>
            <div>
              <label class="lbl">Hisob raqam</label>
              <input v-model="editForm.accountNumber" class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">Izoh</label>
            <textarea v-model="editForm.description" rows="3" class="field !h-auto py-2" />
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <button type="button" class="ghost-btn" @click="editOpen = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Saqlash' }}</button>
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
import { fetchPaymentsByClient, type Payment } from '@/api/payments'
import { fetchDebtHistoryByClient, sendDebtSmsToClient, type DebtNotificationHistory } from '@/api/debt'
import { fetchSaleOrderItemsByClient, type SaleOrderItem } from '@/api/saleOrderItems'
import { formatApiError } from '@/api/http'
import { formatDate, money, today } from '@/utils/format'
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
const { t } = useI18n()

const tabs: { key: TabKey; label: string }[] = [
  { key: 'overview', label: 'Umumiy' },
  { key: 'analysis', label: 'Tahlil' },
  { key: 'orders', label: 'Buyurtmalar' },
  { key: 'items', label: 'Pozitsiyalar' },
  { key: 'act', label: 'Akt sverka (Hisob-kitob)' },
  { key: 'payments', label: "To'lovlar" },
  { key: 'activity', label: 'Faollik' },
  { key: 'notes', label: 'Eslatmalar' },
  { key: 'info', label: "Mijoz ma'lumotlari" },
  { key: 'sms', label: 'SMS tarixi' },
]

const tab = ref<TabKey>('overview')
const loading = ref(true)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const client = ref<Client | null>(null)
const balance = ref<ClientBalance | null>(null)
const notes = ref<ClientNote[]>([])
const sales = ref<SaleOrder[]>([])
const payments = ref<Payment[]>([])
const sms = ref<DebtNotificationHistory[]>([])
const items = ref<SaleOrderItem[]>([])
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
  return `${money(v)} so'm`
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
  { label: client.value?.fullName || 'Mijoz' },
])

const totalSales = computed(() => Number(balance.value?.totalPurchase ?? 0))
const totalPaid = computed(() => Number(balance.value?.totalPaid ?? 0))
const totalDebt = computed(() => Number(balance.value?.totalDebt ?? 0))

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
  if (days == null) return 'Faollik yo\'q'
  if (days === 0) return 'Bugun faol'
  if (days === 1) return '1 kun oldin faol'
  return `${days} kun oldin faol`
})

const debtAgeLabel = computed(() => {
  const debtOrders = sales.value.filter((o) => Number(o.debtSum || 0) > 0)
  if (!debtOrders.length) return 'Qarz yo\'q'
  const ages = debtOrders.map((o) => daysAgo(o.orderDate)).filter((d): d is number => d != null)
  if (!ages.length) return 'Qarz yoshi'
  return `Qarz yoshi: ${Math.max(...ages)} kun`
})

const debtSegments = computed(() => {
  const buckets = [
    { key: '0-30', label: '0-30 kun', color: '#12b76a', min: 0, max: 30 },
    { key: '31-60', label: '31-60 kun', color: '#f79009', min: 31, max: 60 },
    { key: '61-90', label: '61-90 kun', color: '#fb6514', min: 61, max: 90 },
    { key: '90+', label: '90+ kun', color: '#f04438', min: 91, max: Infinity },
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
    const name = o.userFullName || `User #${o.userId || '?'}`
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
    const name = o.warehouseName || 'Noma\'lum'
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
      title: `Buyurtma #${o.id}`,
      meta: `${moneySom(o.totalSum)} · ${formatDate(o.orderDate)}`,
      color: '#465fff',
      at: o.orderDate || '',
    })
  }
  for (const p of payments.value) {
    events.push({
      key: `p-${p.id}`,
      title: `To'lov #${p.id}`,
      meta: `${moneySom(p.paymentAmount)} · ${formatDate(p.paymentDate)}`,
      color: '#12b76a',
      at: p.paymentDate || '',
    })
  }
  for (const n of notes.value) {
    events.push({
      key: `n-${n.id}`,
      title: `Eslatma: ${n.type}`,
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
      fetchDebtHistoryByClient(id, 0, 100),
    ])
    client.value = c.data
    balance.value = b.data
    notes.value = n.data || []
    sales.value = s.data?.content || []
    payments.value = p.data?.content || []
    sms.value = h.data?.content || []
    adjPurchase.value = Number(balance.value?.totalPurchase || 0)
    adjPaid.value = Number(balance.value?.totalPaid || 0)
    nextPaymentDate.value = localStorage.getItem(paymentKey()) || ''

    try {
      const it = await fetchSaleOrderItemsByClient(id, 0, 500)
      items.value = it.data?.content || []
    } catch {
      items.value = []
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
  if (!confirm('Qarz haqida SMS yuborilsinmi?')) return
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

async function onRecalc() {
  try {
    const res = await recalculateClientBalance(clientId())
    balance.value = res.data
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onAdjust() {
  try {
    const res = await adjustClientBalance(clientId(), {
      totalPurchase: adjPurchase.value,
      totalPaid: adjPaid.value,
    })
    balance.value = res.data
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
  if (!confirm("Izoh o'chirilsinmi?")) return
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

onMounted(load)
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
