<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.transportOrders')" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <button
        v-for="f in filters"
        :key="f.id"
        type="button"
        class="tab"
        :class="{ active: filter === f.id }"
        @click="setFilter(f.id)"
      >
        {{ f.label }}
        <span v-if="f.id !== 'all' && counts[f.id]" class="tab-count">{{ counts[f.id] }}</span>
      </button>
      <button type="button" class="icon-btn ms-auto" :disabled="loading" :title="t('common.refresh')" @click="load">
        <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
      </button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('transport.fields.order') }}</th>
              <th class="th">{{ t('transport.orders.colClientAddress') }}</th>
              <th class="th">{{ t('common.sum') }}</th>
              <th class="th">{{ t('common.status') }}</th>
              <th class="th">{{ t('transport.orders.colDriverWorkers') }}</th>
              <th class="th">{{ t('transport.orders.colTimes') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading && !visible.length"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="!visible.length"><td colspan="7" class="empty">{{ t('transport.orders.empty') }}</td></tr>
            <tr v-for="d in visible" :key="d.id" class="border-b border-gray-100 align-top dark:border-gray-800">
              <td class="td whitespace-nowrap">
                <button type="button" class="link" @click="detail = d">#{{ d.saleOrderId }}</button>
                <div class="text-xs text-gray-500">{{ formatDate(d.orderDate) }}</div>
              </td>
              <td class="td min-w-48">
                <div class="font-medium text-gray-800 dark:text-white/90">{{ d.clientFullName || '—' }}</div>
                <div v-if="d.clientPhone" class="text-xs text-gray-500">{{ d.clientPhone }}</div>
                <div v-if="d.address" class="text-xs text-gray-500">{{ d.address }}</div>
              </td>
              <td class="td whitespace-nowrap">{{ money(d.orderTotalSum) }}</td>
              <td class="td"><DeliveryStatusBadge :status="d.deliveryStatus" /></td>
              <td class="td min-w-44">
                <template v-if="d.driverFullName">
                  <div class="font-medium text-gray-800 dark:text-white/90">{{ d.driverFullName }}</div>
                  <div class="text-xs text-gray-500">{{ [d.carModel, d.carNumber].filter(Boolean).join(' · ') || '—' }}</div>
                  <div v-if="d.workers.length" class="text-xs text-gray-500">
                    {{ t('transport.orders.workersList', { names: d.workers.map((w) => w.fullName).join(', ') }) }}
                  </div>
                </template>
                <span v-else class="text-gray-400">—</span>
              </td>
              <td class="td whitespace-nowrap text-xs">
                <div>{{ t('transport.orders.sentAt', { date: formatDate(d.sentAt) }) }}</div>
                <div v-if="d.acceptedAt">{{ t('transport.orders.acceptedAt', { date: formatDate(d.acceptedAt) }) }}</div>
                <div v-if="d.departedAt">{{ t('transport.orders.departedAt', { date: formatDate(d.departedAt) }) }}</div>
                <div v-if="d.arrivedAt">{{ t('transport.orders.arrivedAt', { date: formatDate(d.arrivedAt) }) }}</div>
                <div v-if="d.confirmedAt">{{ t('transport.orders.confirmedAt', { date: formatDate(d.confirmedAt) }) }}</div>
              </td>
              <td class="td text-right">
                <div class="inline-flex flex-wrap justify-end gap-1.5">
                  <button
                    v-if="d.deliveryStatus === 'PENDING'"
                    type="button"
                    class="btn-sm"
                    :disabled="writeBlocked"
                    @click="openCrew(d, 'accept')"
                  >
                    {{ t('transport.orders.accept') }}
                  </button>
                  <button
                    v-if="d.deliveryStatus === 'ACCEPTED'"
                    type="button"
                    class="btn-sm"
                    :disabled="busyId === d.id || writeBlocked"
                    @click="onDepart(d)"
                  >
                    {{ t('transport.orders.depart') }}
                  </button>
                  <button
                    v-if="d.deliveryStatus === 'IN_TRANSIT'"
                    type="button"
                    class="btn-sm btn-success"
                    :disabled="busyId === d.id || writeBlocked"
                    @click="onArrive(d)"
                  >
                    {{ t('transport.orders.arrive') }}
                  </button>
                  <button
                    v-if="['ACCEPTED', 'IN_TRANSIT', 'ARRIVED'].includes(d.deliveryStatus)"
                    type="button"
                    class="btn-sm-ghost"
                    :disabled="writeBlocked"
                    @click="openCrew(d, 'crew')"
                  >
                    {{ t('transport.orders.crew') }}
                  </button>
                  <button type="button" class="btn-sm-ghost" @click="detail = d">{{ t('common.view') }}</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="crewTarget" class="overlay">
      <div class="modal">
        <h3 class="title mb-1">
          {{ crewMode === 'accept' ? t('transport.orders.acceptTitle') : t('transport.orders.crewTitle') }} #{{ crewTarget.saleOrderId }}
        </h3>
        <p class="sub mb-4">{{ crewTarget.clientFullName || '—' }} · {{ money(crewTarget.orderTotalSum) }}</p>
        <div v-if="crewError" class="err mb-3">{{ crewError }}</div>
        <form class="space-y-3" @submit.prevent="onCrewSave">
          <label class="lbl">
            {{ t('transport.orders.driverLabel') }}
            <select v-model.number="crew.driverId" required class="field">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option v-for="dr in drivers" :key="dr.id" :value="dr.id">
                {{ dr.fullName }}{{ dr.carNumber ? ` · ${dr.carNumber}` : '' }}
              </option>
            </select>
          </label>
          <div class="lbl">
            {{ t('transport.fields.workers') }}
            <div class="worker-list">
              <label v-for="w in workers" :key="w.id" class="worker-item">
                <input v-model="crew.workerIds" type="checkbox" :value="w.id" />
                <span>{{ w.fullName }}</span>
              </label>
              <p v-if="!workers.length" class="text-xs text-gray-500">{{ t('transport.orders.noActiveWorkers') }}</p>
            </div>
            <span class="hint">
              {{ t('transport.orders.salaryHint', { percent }) }}
            </span>
          </div>
          <label class="lbl">{{ t('common.address') }}<input v-model="crew.address" class="field" /></label>
          <label class="lbl">{{ t('common.note') }}<input v-model="crew.note" class="field" /></label>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="crewTarget = null">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="crewSaving || !crew.driverId">
              {{ crewSaving ? '...' : crewMode === 'accept' ? t('transport.orders.accept') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="detail" class="overlay" @click.self="detail = null">
      <div class="modal modal-lg">
        <div class="mb-4 flex items-start justify-between gap-3">
          <div>
            <h3 class="title">{{ t('transport.orders.orderTitle', { id: detail.saleOrderId }) }}</h3>
            <p class="sub"><DeliveryStatusBadge :status="detail.deliveryStatus" /></p>
          </div>
          <button type="button" class="ghost" @click="detail = null">{{ t('common.close') }}</button>
        </div>
        <div class="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <div><span class="k">{{ t('common.client') }}</span>{{ detail.clientFullName || '—' }}</div>
          <div><span class="k">{{ t('common.phone') }}</span>{{ detail.clientPhone || '—' }}</div>
          <div class="sm:col-span-2"><span class="k">{{ t('common.address') }}</span>{{ detail.address || '—' }}</div>
          <div><span class="k">{{ t('common.sum') }}</span>{{ money(detail.orderTotalSum) }}</div>
          <div><span class="k">{{ t('transport.orders.seller') }}</span>{{ detail.sellerFullName || '—' }}</div>
          <div><span class="k">{{ t('transport.orders.plannedDelivery') }}</span>{{ formatDate(detail.plannedDeliveryDate) }}</div>
          <div><span class="k">{{ t('common.note') }}</span>{{ detail.note || detail.orderComment || '—' }}</div>
        </div>

        <h4 class="section">{{ t('transport.orders.products') }}</h4>
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('transport.orders.product') }}</th>
              <th class="th">{{ t('transport.orders.quantity') }}</th>
              <th class="th">{{ t('transport.orders.size') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!detail.items.length"><td colspan="3" class="empty">{{ t('transport.orders.noItems') }}</td></tr>
            <tr v-for="it in detail.items" :key="it.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ it.goodsName || '—' }}</td>
              <td class="td">{{ qty(it.count) }}{{ it.goodsType === 'WINDOW' ? ` ${t('transport.orders.sqm')}` : '' }}</td>
              <td class="td">{{ it.width && it.height ? `${qty(it.width)}×${qty(it.height)}` : '—' }}</td>
            </tr>
          </tbody>
        </table>

        <h4 class="section">{{ t('transport.orders.timeline') }}</h4>
        <ul class="timeline">
          <li><span class="k">{{ t('transport.orders.tlSent') }}</span>{{ formatDate(detail.sentAt) }}</li>
          <li v-if="detail.acceptedAt">
            <span class="k">{{ t('transport.orders.tlAccepted') }}</span>{{ formatDate(detail.acceptedAt) }}
            <template v-if="detail.acceptedByName"> · {{ detail.acceptedByName }}</template>
          </li>
          <li v-if="detail.departedAt"><span class="k">{{ t('transport.orders.tlDeparted') }}</span>{{ formatDate(detail.departedAt) }}</li>
          <li v-if="detail.arrivedAt"><span class="k">{{ t('transport.orders.tlArrived') }}</span>{{ formatDate(detail.arrivedAt) }}</li>
          <li v-if="detail.confirmedAt">
            <span class="k">{{ t('transport.orders.tlConfirmed') }}</span>{{ formatDate(detail.confirmedAt) }}
            <template v-if="detail.confirmedByName"> · {{ detail.confirmedByName }}</template>
          </li>
          <li v-if="detail.cancelledAt"><span class="k">{{ t('transport.orders.tlCancelled') }}</span>{{ formatDate(detail.cancelledAt) }}</li>
        </ul>
        <p v-if="detail.deliveryStatus === 'CONFIRMED'" class="sub mt-3">
          {{ t('transport.orders.salaryCalculated') }} <strong>{{ money(detail.salaryTotal) }}</strong> ({{ detail.salaryPercent ?? 0 }}%)
        </p>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import DeliveryStatusBadge from '@/components/crm/DeliveryStatusBadge.vue'
import {
  acceptDelivery,
  arriveDelivery,
  departDelivery,
  fetchActiveDrivers,
  fetchActiveWorkers,
  fetchDeliveries,
  fetchTransportSetting,
  updateDeliveryCrew,
  type Delivery,
  type DeliveryStatus,
  type TransportDriver,
  type TransportWorker,
} from '@/api/transport'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate, money } from '@/utils/format'

type FilterId = 'active' | DeliveryStatus | 'all'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()

const ACTIVE: DeliveryStatus[] = ['PENDING', 'ACCEPTED', 'IN_TRANSIT', 'ARRIVED']
const filters = computed<{ id: FilterId; label: string }[]>(() => [
  { id: 'active', label: t('common.active') },
  ...(['PENDING', 'ACCEPTED', 'IN_TRANSIT', 'ARRIVED', 'CONFIRMED', 'CANCELLED'] as DeliveryStatus[]).map((s) => ({
    id: s as FilterId,
    label: t(`deliveryStatus.${s}`),
  })),
  { id: 'all', label: t('transport.fields.all') },
])

const deliveries = ref<Delivery[]>([])
const drivers = ref<TransportDriver[]>([])
const workers = ref<TransportWorker[]>([])
const percent = ref(0)
const filter = ref<FilterId>('active')
const loading = ref(false)
const error = ref<string | null>(null)
const busyId = ref<number | null>(null)
const detail = ref<Delivery | null>(null)

const crewTarget = ref<Delivery | null>(null)
const crewMode = ref<'accept' | 'crew'>('accept')
const crew = reactive({ driverId: 0, workerIds: [] as number[], address: '', note: '' })
const crewError = ref<string | null>(null)
const crewSaving = ref(false)

const counts = computed(() => {
  const map: Record<string, number> = {}
  for (const d of deliveries.value) map[d.deliveryStatus] = (map[d.deliveryStatus] || 0) + 1
  map.active = ACTIVE.reduce((sum, s) => sum + (map[s] || 0), 0)
  return map
})

const visible = computed(() => {
  if (filter.value === 'all') return deliveries.value
  if (filter.value === 'active') return deliveries.value.filter((d) => ACTIVE.includes(d.deliveryStatus))
  return deliveries.value.filter((d) => d.deliveryStatus === filter.value)
})

function setFilter(id: FilterId) {
  filter.value = id
}

function qty(v?: number | null) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

async function load() {
  loading.value = true
  error.value = null
  try {
    deliveries.value = (await fetchDeliveries()).data || []
    if (detail.value) detail.value = deliveries.value.find((d) => d.id === detail.value?.id) || null
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

async function openCrew(d: Delivery, mode: 'accept' | 'crew') {
  crewTarget.value = d
  crewMode.value = mode
  crewError.value = null
  crew.driverId = d.driverId || 0
  crew.workerIds = d.workers.map((w) => w.id)
  crew.address = d.address || ''
  crew.note = d.note || ''
  try {
    const [dr, wk, st] = await Promise.all([fetchActiveDrivers(), fetchActiveWorkers(), fetchTransportSetting()])
    drivers.value = dr.data || []
    workers.value = wk.data || []
    percent.value = Number(st.data?.workerSalaryPercent ?? 0)
    if (!crew.driverId && drivers.value.length === 1) crew.driverId = drivers.value[0].id
  } catch (e) {
    crewError.value = formatApiError(e, t('transport.orders.listsLoadError'))
  }
}

async function onCrewSave() {
  if (!crewTarget.value || !crew.driverId) return
  crewSaving.value = true
  crewError.value = null
  const payload = {
    driverId: crew.driverId,
    workerIds: crew.workerIds,
    address: crew.address.trim() || undefined,
    note: crew.note,
  }
  try {
    if (crewMode.value === 'accept') await acceptDelivery(crewTarget.value.id, payload)
    else await updateDeliveryCrew(crewTarget.value.id, payload)
    crewTarget.value = null
    await load()
  } catch (e) {
    crewError.value = formatApiError(e, t('common.saveError'))
  } finally {
    crewSaving.value = false
  }
}

async function runAction(d: Delivery, action: (id: number) => Promise<unknown>, question: string) {
  if (!confirm(question)) return
  busyId.value = d.id
  error.value = null
  try {
    await action(d.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    busyId.value = null
  }
}

function onDepart(d: Delivery) {
  return runAction(d, departDelivery, t('transport.orders.departConfirm', { id: d.saleOrderId }))
}

function onArrive(d: Delivery) {
  return runAction(d, arriveDelivery, t('transport.orders.arriveConfirm', { id: d.saleOrderId }))
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.lbl { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.hint { font-weight: 400; color: #6b7280; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-sm { display: inline-flex; height: 2rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 0.75rem; font-size: 0.75rem; font-weight: 500; color: #fff; white-space: nowrap; }
.btn-sm:disabled, .btn-sm-ghost:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-success { background: #059669; }
.btn-sm-ghost { display: inline-flex; height: 2rem; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.75rem; white-space: nowrap; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.icon-btn { display: inline-flex; height: 2.25rem; width: 2.25rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #e5e7eb; color: #4b5563; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.tab { display: inline-flex; align-items: center; gap: 0.375rem; height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.875rem; font-size: 0.875rem; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
.tab-count { min-width: 1.25rem; border-radius: 9999px; background: rgb(0 0 0 / 8%); padding: 0 0.375rem; font-size: 0.75rem; }
.tab.active .tab-count { background: rgb(255 255 255 / 25%); }
.link { color: #465fff; font-weight: 500; }
.link:hover { text-decoration: underline; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 30rem; max-height: 90vh; overflow-y: auto; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.modal-lg { max-width: 44rem; }
.worker-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr)); gap: 0.375rem; max-height: 12rem; overflow-y: auto; border-radius: 0.5rem; border: 1px solid #e5e7eb; padding: 0.5rem; }
.worker-item { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; font-weight: 400; color: #374151; }
.k { display: block; font-size: 0.75rem; color: #6b7280; }
.section { margin: 1.25rem 0 0.5rem; font-size: 0.875rem; font-weight: 600; color: #1f2937; }
.timeline { display: grid; gap: 0.5rem; font-size: 0.875rem; color: #374151; }
.dark .worker-list { border-color: #374151; }
.dark .worker-item, .dark .timeline { color: #d1d5db; }
.dark .section { color: rgba(255, 255, 255, 0.92); }
.dark .btn-sm-ghost { border-color: #374151; color: #e5e7eb; }
.dark .tab-count { background: rgb(255 255 255 / 10%); }
</style>
