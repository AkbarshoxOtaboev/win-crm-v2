<template>
  <Teleport to="body">
    <div v-if="open" class="wo-view-overlay">
      <div class="modal">
        <div class="modal-head no-print">
          <div>
            <h3 class="title">{{ t('warehouseOrders.orderNo', { id: order?.id }) }}</h3>
            <p class="sub">{{ t('warehouseOrders.orderSubtitle') }}</p>
          </div>
          <button type="button" class="ghost" @click="close">{{ t('common.close') }}</button>
        </div>

        <div v-if="loading" class="empty">{{ t('common.loading') }}</div>
        <div v-else-if="order" ref="printAreaRef" class="wo-print-area">
          <div class="print-only-title">
            <h3 class="title">{{ t('warehouseOrders.orderNo', { id: order.id }) }}</h3>
            <p class="sub">{{ t('warehouseOrders.orderSubtitle') }}</p>
          </div>

          <div class="info-grid">
            <div><span class="lbl">{{ t('warehouseOrders.supplier') }}</span>{{ order.supplierName || '—' }}</div>
            <div><span class="lbl">{{ t('common.phone') }}</span>{{ supplierPhone || '—' }}</div>
            <div><span class="lbl">{{ t('common.warehouse') }}</span>{{ order.warehouseName || '—' }}</div>
            <div><span class="lbl">{{ t('common.date') }}</span>{{ formatDate(order.arrivalDate) }}</div>
            <div><span class="lbl">{{ t('common.status') }}</span>{{ statusLabel }}</div>
            <div>
              <span class="lbl">{{ t('warehouseOrders.grandTotal') }}</span>{{ amt(orderTotal) }}
              <span v-if="isForeign && order.totalSumBase != null" class="base-eq">
                {{ t('warehouseOrders.baseEquivalent', { value: moneyIn(order.totalSumBase, 'UZS', t('common.currency')) }) }}
              </span>
            </div>
            <div v-if="isForeign">
              <span class="lbl">{{ t('warehouseOrders.currency') }}</span>
              {{ t(`exchangeRates.currencies.${order.currency}`) }} ·
              {{ t('warehouseOrders.rateLabel', { value: formatRate(order.exchangeRate) }) }}
            </div>
            <div v-if="Number(order.serviceFee) > 0">
              <span class="lbl">{{ t('warehouseOrders.serviceFee') }}</span>{{ amt(order.serviceFee) }}
            </div>
            <div v-if="order.comment"><span class="lbl">{{ t('common.comment') }}</span>{{ order.comment }}</div>
          </div>

          <h4 class="section-title">{{ t('warehouseOrders.items') }}</h4>
          <div class="overflow-x-auto">
            <table class="min-w-full">
              <thead>
                <tr class="border-b border-gray-100">
                  <th class="th">#</th>
                  <th class="th">{{ t('common.product') }}</th>
                  <th class="th">{{ t('warehouseOrders.widthCm') }}</th>
                  <th class="th">{{ t('warehouseOrders.heightCm') }}</th>
                  <th class="th">{{ t('common.count') }}</th>
                  <th class="th">{{ t('warehouseOrders.kvm') }}</th>
                  <th class="th">{{ t('warehouseOrders.arrivalPrice') }}</th>
                  <th class="th">{{ t('common.sum') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="displayItems.length === 0">
                  <td colspan="8" class="empty">{{ t('warehouseOrders.noItems') }}</td>
                </tr>
                <tr v-for="(row, idx) in displayItems" :key="row.id" class="border-b border-gray-100">
                  <td class="td">{{ idx + 1 }}</td>
                  <td class="td">{{ row.goodsName }}</td>
                  <td class="td">{{ row.width != null ? formatNum(row.width) : '—' }}</td>
                  <td class="td">{{ row.height != null ? formatNum(row.height) : '—' }}</td>
                  <td class="td">{{ row.pieces != null ? formatNum(row.pieces) : formatNum(row.count) }}</td>
                  <td class="td">{{ row.isWindow ? formatNum(row.count) : '—' }}</td>
                  <td class="td">{{ amt(row.priceCost) }}</td>
                  <td class="td">{{ amt(row.sum) }}</td>
                </tr>
              </tbody>
              <tfoot v-if="displayItems.length || Number(order.serviceFee) > 0">
                <tr v-if="displayItems.length" class="border-t border-gray-200 bg-gray-50">
                  <td class="td font-medium" colspan="7">{{ t('warehouseOrders.itemsTotal') }}</td>
                  <td class="td font-medium">{{ amt(itemsTotalSum) }}</td>
                </tr>
                <tr v-if="Number(order.serviceFee) > 0" class="border-t border-gray-100 bg-gray-50">
                  <td class="td font-medium" colspan="7">{{ t('warehouseOrders.serviceFee') }}</td>
                  <td class="td font-medium">{{ amt(order.serviceFee) }}</td>
                </tr>
                <tr class="border-t border-gray-200 bg-gray-50">
                  <td class="td font-semibold" colspan="7">{{ t('common.total') }}</td>
                  <td class="td font-semibold">{{ amt(orderTotal) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div class="notify-block no-print">
            <h4 class="section-title">{{ t('warehouseOrders.messageToSupplier') }}</h4>
            <p v-if="notifyError" class="err mb-2">{{ notifyError }}</p>
            <p v-if="notifySuccess" class="ok mb-2">{{ notifySuccess }}</p>
            <textarea
              v-model="messageText"
              rows="5"
              class="textarea"
              :placeholder="t('warehouseOrders.messagePlaceholder')"
            />
            <div class="action-row">
              <button type="button" class="btn sms-btn" :disabled="sending || !messageText.trim()" @click="onSendSms">
                <MailIcon class="btn-icon" />
                {{ sending === 'sms' ? '...' : t('warehouseOrders.sendSms') }}
              </button>
              <button
                type="button"
                class="btn tg-btn"
                :disabled="sending || !messageText.trim()"
                @click="onSendTelegram"
              >
                <SendIcon class="btn-icon" />
                {{ sending === 'telegram' ? '...' : t('warehouseOrders.telegramBot') }}
              </button>
              <button type="button" class="ghost" @click="onPrint">
                <DocsIcon class="btn-icon" />
                {{ t('common.print') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  fetchWarehouseOrder,
  fetchWarehouseOrderItems,
  sendWarehouseOrderSms,
  sendWarehouseOrderTelegram,
  type WarehouseOrder,
  type WarehouseOrderItem,
} from '@/api/warehouseOrders'
import type { Supplier } from '@/api/suppliers'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'
import { formatDate, money } from '@/utils/format'
import { formatRate, moneyIn } from '@/utils/currency'
import MailIcon from '@/icons/MailIcon.vue'
import SendIcon from '@/icons/SendIcon.vue'
import DocsIcon from '@/icons/DocsIcon.vue'

const props = defineProps<{
  open: boolean
  orderId: number | null
  supplier?: Supplier | null
}>()

const emit = defineEmits<{
  close: []
}>()

const { t } = useI18n()
const order = ref<WarehouseOrder | null>(null)
const items = ref<WarehouseOrderItem[]>([])
const goods = ref<Goods[]>([])
const loading = ref(false)
const messageText = ref('')
const sending = ref<'sms' | 'telegram' | false>(false)
const notifyError = ref<string | null>(null)
const notifySuccess = ref<string | null>(null)
const printAreaRef = ref<HTMLElement | null>(null)

const supplierPhone = computed(() => props.supplier?.phone || '')
const orderTotal = computed(() => Number(order.value?.totalSum || 0))
const isForeign = computed(() => !!order.value?.currency && order.value.currency !== 'UZS')

function amt(v?: number | null) {
  return isForeign.value ? moneyIn(v, order.value?.currency) : money(v)
}
const statusLabel = computed(() => {
  if (order.value?.orderStatus === 'TRANSFERRED') return t('warehouseOrders.statusTransferred')
  return order.value?.orderStatus || t('warehouseOrders.statusNew')
})

function isWindowItem(it: WarehouseOrderItem) {
  const g = goods.value.find((x) => x.id === it.goodsId)
  if (g) return (g.type || '').toUpperCase() === 'WINDOW'
  return it.weight != null && it.height != null && Number(it.weight) > 0 && Number(it.height) > 0
}

const displayItems = computed(() =>
  items.value.map((it) => {
    const windowItem = isWindowItem(it)
    const width = it.weight != null ? Number(it.weight) : null
    const height = it.height != null ? Number(it.height) : null
    const count = Number(it.count || 0)
    const priceCost = Number(it.priceCost || 0)
    let pieces: number | null = it.pieceCount != null ? Number(it.pieceCount) : null
    if (pieces == null && windowItem && width && height && width > 0 && height > 0) {
      pieces = (count * 10000) / (width * height)
    }
    return {
      id: it.id,
      goodsName: it.goodsName || String(it.goodsId || ''),
      width,
      height,
      pieces,
      count,
      priceCost,
      isWindow: windowItem,
      sum: count * priceCost,
    }
  }),
)

const itemsTotalSum = computed(() => displayItems.value.reduce((acc, r) => acc + r.sum, 0))

function formatNum(v?: number | null) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

function buildDefaultMessage(o: WarehouseOrder, rows: typeof displayItems.value) {
  const lines = [
    t('warehouseOrders.msgHeader', { id: o.id }),
    t('warehouseOrders.msgSupplier', { value: o.supplierName || '—' }),
    t('warehouseOrders.msgWarehouse', { value: o.warehouseName || '—' }),
    t('warehouseOrders.msgDate', { value: formatDate(o.arrivalDate) }),
    '',
    t('warehouseOrders.msgItems'),
  ]
  rows.forEach((row, idx) => {
    lines.push(
      `${idx + 1}. ${row.goodsName} — ${row.isWindow ? `${formatNum(row.count)} ${t('warehouseOrders.kvmUnit')}` : `${formatNum(row.count)} ${t('common.pcs')}`} — ${amt(row.sum)}`,
    )
  })
  if (Number(o.serviceFee) > 0) {
    lines.push(t('warehouseOrders.msgServiceFee', { value: amt(o.serviceFee) }))
  }
  lines.push(t('warehouseOrders.msgTotal', { value: amt(o.totalSum) }))
  return lines.join('\n')
}

async function load() {
  if (!props.orderId) return
  loading.value = true
  notifyError.value = null
  notifySuccess.value = null
  try {
    const [o, it, g] = await Promise.all([
      fetchWarehouseOrder(props.orderId),
      fetchWarehouseOrderItems(props.orderId),
      fetchGoods(),
    ])
    order.value = o.data || null
    items.value = it.data || []
    goods.value = g.data || []
    if (order.value) {
      messageText.value = buildDefaultMessage(order.value, displayItems.value)
    }
  } catch (e) {
    notifyError.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

function close() {
  emit('close')
}

async function onSendSms() {
  if (!props.orderId || !messageText.value.trim()) return
  sending.value = 'sms'
  notifyError.value = null
  notifySuccess.value = null
  try {
    await sendWarehouseOrderSms(props.orderId, messageText.value.trim())
    notifySuccess.value = t('warehouseOrders.smsSent')
  } catch (e) {
    notifyError.value = formatApiError(e)
  } finally {
    sending.value = false
  }
}

async function onSendTelegram() {
  if (!props.orderId || !messageText.value.trim()) return
  sending.value = 'telegram'
  notifyError.value = null
  notifySuccess.value = null
  try {
    await sendWarehouseOrderTelegram(props.orderId, messageText.value.trim())
    notifySuccess.value = t('warehouseOrders.telegramSent')
  } catch (e) {
    notifyError.value = formatApiError(e)
  } finally {
    sending.value = false
  }
}

function onPrint() {
  window.print()
}

watch(
  () => [props.open, props.orderId] as const,
  ([isOpen, id]) => {
    if (isOpen && id) {
      void load()
    } else {
      order.value = null
      items.value = []
      messageText.value = ''
      notifyError.value = null
      notifySuccess.value = null
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.wo-view-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow-y: auto;
  background: rgba(0, 0, 0, 0.45);
  padding: 1rem;
}
.modal {
  width: 100%;
  max-width: 56rem;
  border-radius: 1rem;
  background: #fff;
  padding: 1.25rem;
  margin: 1rem 0 2rem;
}
.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f3f4f6;
}
.print-only-title {
  display: none;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
}
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem 1rem;
  margin-bottom: 1.25rem;
  font-size: 0.875rem;
  color: #374151;
}
.lbl {
  display: block;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 0.15rem;
}
.base-eq {
  display: block;
  font-size: 0.75rem;
  color: #6b7280;
}
.section-title {
  margin: 0 0 0.75rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1f2937;
}
.th { padding: 0.65rem 0.75rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.65rem 0.75rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.notify-block { margin-top: 1.25rem; padding-top: 1.25rem; border-top: 1px solid #f3f4f6; }
.textarea {
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0.75rem;
  font-size: 0.875rem;
  resize: vertical;
}
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.75rem;
}
.btn {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.4rem;
  border-radius: 0.5rem;
  padding: 0 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
}
.sms-btn { background: #465fff; }
.tg-btn { background: #0088cc; }
.ghost {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.4rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  color: #374151;
  background: #fff;
}
.btn-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.5rem 0.75rem; font-size: 0.8125rem; color: #dc2626; }
.ok { border-radius: 0.5rem; border: 1px solid #bbf7d0; background: #f0fdf4; padding: 0.5rem 0.75rem; font-size: 0.8125rem; color: #15803d; }
</style>

<style>
@media print {
  body * {
    visibility: hidden !important;
  }
  .wo-print-area,
  .wo-print-area * {
    visibility: visible !important;
  }
  .wo-view-overlay {
    position: static !important;
    inset: auto !important;
    background: transparent !important;
    padding: 0 !important;
    overflow: visible !important;
    display: block !important;
  }
  .wo-view-overlay .modal {
    max-width: none !important;
    margin: 0 !important;
    padding: 0 !important;
    border-radius: 0 !important;
    box-shadow: none !important;
  }
  .wo-print-area {
    position: absolute !important;
    left: 0 !important;
    top: 0 !important;
    width: 100% !important;
    padding: 0 !important;
  }
  .wo-print-area .print-only-title {
    display: block !important;
  }
  .wo-print-area .no-print {
    display: none !important;
  }
  .wo-print-area table {
    width: 100%;
    border-collapse: collapse;
  }
  .wo-print-area .th,
  .wo-print-area .td {
    border-bottom: 1px solid #e5e7eb;
  }
}
</style>
