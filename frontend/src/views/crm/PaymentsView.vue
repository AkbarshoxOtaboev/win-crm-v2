<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="To‘lovlar" />

    <div class="mb-4 flex flex-wrap gap-2">
      <button
        type="button"
        class="tab"
        :class="{ active: tab === 'payments' }"
        @click="tab = 'payments'"
      >
        <Wallet class="h-4 w-4" />
        To‘lovlar
        <span class="tab-count">{{ items.length }}</span>
      </button>
      <button
        type="button"
        class="tab"
        :class="{ active: tab === 'types' }"
        @click="tab = 'types'"
      >
        <CreditCard class="h-4 w-4" />
        To‘lov turlari
        <span class="tab-count">{{ paymentTypes.length }}</span>
      </button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <!-- Payments -->
    <div
      v-show="tab === 'payments'"
      class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <div
        class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800 lg:flex-row lg:items-center lg:justify-between"
      >
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">To‘lovlar</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Mijoz to‘lovlarini qo‘shish, filtrlash va kuzatish
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <button
            type="button"
            class="icon-btn"
            title="Filtrlar"
            @click="showFilters = !showFilters"
          >
            <ListFilter class="h-4 w-4" />
          </button>
          <button
            type="button"
            class="icon-btn"
            title="Yangilash"
            :disabled="loading"
            @click="load"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
          <button type="button" class="btn btn-with-icon" @click="openCreate">
            <Plus class="h-4 w-4" />
            Yangi to‘lov
          </button>
        </div>
      </div>

      <div v-if="showFilters" class="filters border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="lbl-block">
            Mijoz
            <SearchableSelect
              :model-value="filterClientId"
              :options="filterClientOptions"
              placeholder="Barcha mijozlar"
              search-placeholder="FIO yoki telefon..."
              @update:model-value="onFilterClientChange"
            />
          </label>
          <label class="lbl-block">
            Qidiruv
            <div class="relative">
              <Search class="field-icon" />
              <input
                v-model="search"
                type="search"
                placeholder="Tur, buyurtma yoki izoh..."
                class="field"
              />
            </div>
          </label>
        </div>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <button type="button" class="ghost" @click="clearFilters">Filterni tozalash</button>
          <span class="text-sm text-gray-500 dark:text-gray-400">
            Ko‘rsatilmoqda: {{ filtered.length }} ta
          </span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Mijoz</th>
              <th class="th">Tur</th>
              <th class="th">Summa</th>
              <th class="th">Sana</th>
              <th class="th">Buyurtma</th>
              <th class="th th-actions">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="empty">Yuklanmoqda...</td>
            </tr>
            <tr v-else-if="filtered.length === 0">
              <td colspan="7" class="empty">
                <div class="flex flex-col items-center gap-2 py-2">
                  <Wallet class="h-8 w-8 text-gray-300 dark:text-gray-600" />
                  <span>To‘lov topilmadi</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="p in filtered"
              :key="p.id"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50/80 dark:border-gray-800 dark:hover:bg-white/[0.02]"
            >
              <td class="td text-gray-400">{{ p.id }}</td>
              <td class="td">
                <div class="font-medium text-gray-800 dark:text-white/90">
                  {{ p.clientFullName || '—' }}
                </div>
                <div v-if="p.comment" class="mt-0.5 max-w-48 truncate text-xs text-gray-400">
                  {{ p.comment }}
                </div>
              </td>
              <td class="td">
                <span class="type-badge">
                  <CreditCard class="h-3.5 w-3.5 shrink-0 opacity-70" />
                  {{ p.paymentTypeName || '—' }}
                </span>
              </td>
              <td class="td">
                <span class="font-semibold text-success-600 dark:text-success-400">
                  {{ money(p.paymentAmount) }}
                </span>
              </td>
              <td class="td whitespace-nowrap">{{ formatDate(p.paymentDate) }}</td>
              <td class="td">
                <span
                  v-if="p.saleOrderId"
                  class="inline-flex items-center gap-1 rounded-md bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/10 dark:text-brand-400"
                >
                  #{{ p.saleOrderId }}
                </span>
                <span v-else class="text-xs text-gray-400">Avans</span>
              </td>
              <td class="td td-actions">
                <RowActions @edit="openEdit(p)" @delete="onDelete(p)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Payment types -->
    <div
      v-show="tab === 'types'"
      class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <div
        class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800"
      >
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">To‘lov turlari</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Naqd, plastik, o‘tkazma va boshqa to‘lov usullari
          </p>
        </div>
        <button type="button" class="btn btn-with-icon" @click="openTypeCreate">
          <Plus class="h-4 w-4" />
          Yangi tur
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Nomi</th>
              <th class="th th-actions">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="3" class="empty">Yuklanmoqda...</td>
            </tr>
            <tr v-else-if="paymentTypes.length === 0">
              <td colspan="3" class="empty">
                <div class="flex flex-col items-center gap-2 py-2">
                  <CreditCard class="h-8 w-8 text-gray-300 dark:text-gray-600" />
                  <span>To‘lov turi yo‘q</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="t in paymentTypes"
              :key="t.id"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50/80 dark:border-gray-800 dark:hover:bg-white/[0.02]"
            >
              <td class="td text-gray-400">{{ t.id }}</td>
              <td class="td">
                <div class="flex items-center gap-3">
                  <span
                    class="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/10 dark:text-brand-400"
                  >
                    <CreditCard class="h-4 w-4" />
                  </span>
                  <span class="font-medium text-gray-800 dark:text-white/90">{{ t.name }}</span>
                </div>
              </td>
              <td class="td td-actions">
                <RowActions @edit="openTypeEdit(t)" @delete="onTypeDelete(t)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Payment modal -->
    <div v-if="modalOpen" class="overlay" @click.self="closePaymentModal">
      <div
        class="modal modal-lg"
        role="dialog"
        aria-modal="true"
        aria-labelledby="payment-modal-title"
      >
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-500 dark:bg-brand-500/10 dark:text-brand-400"
            >
              <Banknote class="h-5 w-5" />
            </span>
            <div>
              <h3
                id="payment-modal-title"
                class="text-lg font-semibold text-gray-800 dark:text-white/90"
              >
                {{ editingId ? 'To‘lovni tahrirlash' : 'Yangi to‘lov' }}
              </h3>
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Mijoz, summa va to‘lov turini kiriting
              </p>
            </div>
          </div>
          <button
            type="button"
            class="close-btn"
            aria-label="Yopish"
            @click="closePaymentModal"
          >
            <X class="h-5 w-5" />
          </button>
        </div>

        <div v-if="formError" class="err mb-4">{{ formError }}</div>

        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">Mijoz <span class="req">*</span></label>
            <SearchableSelect
              :model-value="form.clientId"
              :options="clientOptions"
              placeholder="Mijozni tanlang..."
              search-placeholder="FIO yoki telefon..."
              @update:model-value="onFormClientChange"
            />
          </div>

          <div v-if="form.clientId && (clientSummaryLoading || clientBalance)" class="debt-card">
            <div v-if="clientSummaryLoading" class="debt-loading">Qarz ma’lumoti yuklanmoqda...</div>
            <template v-else-if="clientBalance">
              <div class="debt-head">
                <span class="debt-title">Mijoz balansi</span>
                <span class="debt-orders">{{ clientSaleOrders.length }} ta buyurtma</span>
              </div>
              <div class="debt-grid">
                <div class="debt-item">
                  <span class="debt-lbl">Jami savdo</span>
                  <span class="debt-val">{{ money(clientBalance.totalPurchase) }}</span>
                </div>
                <div class="debt-item">
                  <span class="debt-lbl">Jami to‘lovlar</span>
                  <span class="debt-val text-success-600 dark:text-success-400">
                    {{ money(clientBalance.totalPaid) }}
                    <span v-if="clientPaymentsCount" class="debt-sub">({{ clientPaymentsCount }} ta)</span>
                  </span>
                </div>
                <div class="debt-item debt-item-accent">
                  <span class="debt-lbl">Umumiy qarz</span>
                  <span
                    class="debt-val"
                    :class="Number(clientBalance.totalDebt) > 0 ? 'text-error-600 dark:text-error-400' : 'text-success-600'"
                  >
                    {{ money(clientBalance.totalDebt) }}
                  </span>
                </div>
              </div>
              <div v-if="clientOrdersWithDebt.length" class="debt-orders-list">
                <div
                  v-for="o in clientOrdersWithDebt"
                  :key="o.id"
                  class="debt-order-row"
                  :class="{ active: form.saleOrderId === o.id }"
                  role="button"
                  tabindex="0"
                  @click="form.saleOrderId = o.id"
                  @keydown.enter.prevent="form.saleOrderId = o.id"
                >
                  <span>#{{ o.id }}</span>
                  <span class="text-gray-400">{{ money(o.totalSum) }}</span>
                  <span class="font-medium text-error-600 dark:text-error-400">
                    qarz: {{ money(o.debtSum) }}
                  </span>
                </div>
              </div>
              <p v-else-if="clientSaleOrders.length" class="debt-hint">
                Barcha buyurtmalar bo‘yicha qarz yo‘q
              </p>
            </template>
          </div>

          <div>
            <label for="pay-user" class="lbl">Qabul qiluvchi <span class="req">*</span></label>
            <div class="relative">
              <UserCog class="field-icon" />
              <select id="pay-user" v-model.number="form.userId" required class="field field-select">
                <option v-for="u in users" :key="u.id" :value="u.id">
                  {{ u.fullName || u.username }}
                </option>
              </select>
              <ChevronDown class="select-chevron" />
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="pay-type" class="lbl">To‘lov turi <span class="req">*</span></label>
              <div class="relative">
                <CreditCard class="field-icon" />
                <select
                  id="pay-type"
                  v-model.number="form.paymentTypeId"
                  required
                  class="field field-select"
                >
                  <option v-for="t in paymentTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
                </select>
                <ChevronDown class="select-chevron" />
              </div>
            </div>

            <div>
              <label for="pay-amount" class="lbl">Summa <span class="req">*</span></label>
              <div class="relative">
                <Banknote class="field-icon" />
                <input
                  id="pay-amount"
                  v-model.number="form.paymentAmount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  required
                  placeholder="0.00"
                  class="field"
                />
              </div>
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="pay-date" class="lbl">Sana <span class="req">*</span></label>
              <div class="relative">
                <CalendarDays class="field-icon" />
                <input
                  id="pay-date"
                  v-model="form.paymentDate"
                  type="datetime-local"
                  required
                  class="field"
                />
              </div>
            </div>

            <div>
              <label for="pay-order" class="lbl">Savdo buyurtmasi</label>
              <div class="relative">
                <ShoppingCart class="field-icon" />
                <select
                  id="pay-order"
                  v-model.number="form.saleOrderId"
                  class="field field-select"
                  :disabled="!form.clientId"
                >
                  <option :value="0">— Avans (buyurtmasiz) —</option>
                  <option v-for="o in clientSaleOrders" :key="o.id" :value="o.id">
                    {{ formatOrderOption(o) }}
                  </option>
                </select>
                <ChevronDown class="select-chevron" />
              </div>
              <p v-if="!form.clientId" class="mt-1 text-xs text-gray-400">Avval mijozni tanlang</p>
            </div>
          </div>

          <div>
            <label for="pay-comment" class="lbl">Izoh</label>
            <div class="relative">
              <MessageSquare class="field-icon field-icon-top" />
              <textarea
                id="pay-comment"
                v-model="form.comment"
                rows="2"
                placeholder="Ixtiyoriy izoh..."
                class="field field-textarea"
              />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="closePaymentModal">Bekor</button>
            <button type="submit" class="btn btn-with-icon" :disabled="saving">
              <Check class="h-4 w-4" />
              {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Payment type modal -->
    <div v-if="typeModal" class="overlay" @click.self="closeTypeModal">
      <div
        class="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="type-modal-title"
      >
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-500 dark:bg-brand-500/10 dark:text-brand-400"
            >
              <CreditCard class="h-5 w-5" />
            </span>
            <div>
              <h3
                id="type-modal-title"
                class="text-lg font-semibold text-gray-800 dark:text-white/90"
              >
                {{ typeEditingId ? 'Turni tahrirlash' : 'Yangi to‘lov turi' }}
              </h3>
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Masalan: Naqd, Plastik, O‘tkazma
              </p>
            </div>
          </div>
          <button type="button" class="close-btn" aria-label="Yopish" @click="closeTypeModal">
            <X class="h-5 w-5" />
          </button>
        </div>

        <div v-if="typeFormError" class="err mb-4">{{ typeFormError }}</div>

        <form class="space-y-4" @submit.prevent="onTypeSubmit">
          <div>
            <label for="type-name" class="lbl">Tur nomi <span class="req">*</span></label>
            <div class="relative">
              <Tag class="field-icon" />
              <input
                id="type-name"
                v-model="typeName"
                required
                placeholder="Masalan: Naqd pul"
                class="field"
              />
            </div>
          </div>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="closeTypeModal">Bekor</button>
            <button type="submit" class="btn btn-with-icon" :disabled="typeSaving">
              <Check class="h-4 w-4" />
              {{ typeSaving ? 'Saqlanmoqda...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Banknote,
  CalendarDays,
  Check,
  ChevronDown,
  CreditCard,
  ListFilter,
  MessageSquare,
  Plus,
  RefreshCw,
  Search,
  ShoppingCart,
  Tag,
  UserCog,
  Wallet,
  X,
} from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import {
  createPayment,
  createPaymentType,
  deletePayment,
  deletePaymentType,
  fetchPayments,
  fetchPaymentsByClient,
  fetchPaymentTypes,
  updatePayment,
  updatePaymentType,
  type Payment,
  type PaymentType,
} from '@/api/payments'
import { fetchClients, type Client } from '@/api/clients'
import { fetchClientBalance, type ClientBalance } from '@/api/clientBalances'
import { fetchUsers, type UserItem } from '@/api/users'
import { fetchSaleOrdersByClient, type SaleOrder } from '@/api/sales'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'
import { formatDate, money, nowLocal, toApiDate, today } from '@/utils/format'
import { formatUzPhone } from '@/utils/phone'

const tab = ref<'payments' | 'types'>('payments')
const items = ref<Payment[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const paymentTypes = ref<PaymentType[]>([])
const clientSaleOrders = ref<SaleOrder[]>([])
const clientBalance = ref<ClientBalance | null>(null)
const clientPaymentsCount = ref(0)
const clientSummaryLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const typeSaving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const typeFormError = ref<string | null>(null)
const search = ref('')
const showFilters = ref(true)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const filterClientId = ref(0)
const typeModal = ref(false)
const typeEditingId = ref<number | null>(null)
const typeName = ref('')
const auth = useAuthStore()
const form = reactive({
  clientId: 0,
  userId: 0,
  paymentTypeId: 0,
  paymentAmount: 0,
  paymentDate: '',
  saleOrderId: 0,
  comment: '',
})

function clientLabel(c: Client) {
  const phone = c.phone ? formatUzPhone(c.phone) : ''
  return phone ? `${c.fullName} · ${phone}` : c.fullName
}

function formatOrderOption(o: SaleOrder) {
  const debt = Number(o.debtSum || 0)
  const base = `#${o.id} · ${money(o.totalSum)}`
  return debt > 0 ? `${base} (qarz: ${money(debt)})` : base
}

const clientOptions = computed(() =>
  clients.value.map((c) => ({
    value: c.id,
    label: clientLabel(c),
    searchText: `${c.fullName} ${c.phone || ''}`,
  })),
)

const filterClientOptions = computed(() => [
  { value: 0, label: 'Barcha mijozlar', searchText: '' },
  ...clientOptions.value,
])

const clientOrdersWithDebt = computed(() =>
  clientSaleOrders.value.filter((o) => Number(o.debtSum || 0) > 0),
)

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((p) =>
    [p.clientFullName, p.paymentTypeName, String(p.saleOrderId || ''), p.comment].some((v) =>
      String(v || '')
        .toLowerCase()
        .includes(q),
    ),
  )
})

function onFilterClientChange(id: number) {
  filterClientId.value = id
  load()
}

function clearFilters() {
  filterClientId.value = 0
  search.value = ''
  load()
}

function clearClientSummary() {
  clientBalance.value = null
  clientSaleOrders.value = []
  clientPaymentsCount.value = 0
  clientSummaryLoading.value = false
}

async function loadClientSummary(clientId: number) {
  if (!clientId) {
    clearClientSummary()
    return
  }
  clientSummaryLoading.value = true
  try {
    const [balRes, ordersRes, paysRes] = await Promise.all([
      fetchClientBalance(clientId, { fromDate: '2000-01-01', toDate: today() }),
      fetchSaleOrdersByClient(clientId, 0, 200),
      fetchPaymentsByClient(clientId, 0, 200),
    ])
    clientBalance.value = balRes.data || null
    clientSaleOrders.value = ordersRes.data?.content || []
    const pays = paysRes.data?.content || []
    clientPaymentsCount.value = paysRes.data?.totalElements ?? pays.length
    if (
      form.saleOrderId &&
      !clientSaleOrders.value.some((o) => o.id === form.saleOrderId)
    ) {
      form.saleOrderId = 0
    }
  } catch (e) {
    formError.value = formatApiError(e)
    clearClientSummary()
  } finally {
    clientSummaryLoading.value = false
  }
}

function onFormClientChange(id: number) {
  form.clientId = id
  form.saleOrderId = 0
  formError.value = null
  loadClientSummary(id)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [clientsRes, usersRes, typesRes] = await Promise.all([
      fetchClients(),
      fetchUsers(),
      fetchPaymentTypes(),
    ])
    clients.value = clientsRes.data || []
    users.value = usersRes.data || []
    paymentTypes.value = typesRes.data?.content || []
    const payRes = filterClientId.value
      ? await fetchPaymentsByClient(filterClientId.value)
      : await fetchPayments()
    items.value = payRes.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

function fillForm(p?: Payment) {
  const me = users.value.find((u) => u.username === auth.username)
  form.clientId = p?.clientId || 0
  form.userId = p?.userId || me?.id || users.value[0]?.id || 0
  form.paymentTypeId = p?.paymentTypeId || paymentTypes.value[0]?.id || 0
  form.paymentAmount = Number(p?.paymentAmount || 0)
  form.paymentDate = p?.paymentDate ? p.paymentDate.slice(0, 16) : nowLocal()
  form.saleOrderId = p?.saleOrderId || 0
  form.comment = p?.comment || ''
}

function closePaymentModal() {
  modalOpen.value = false
  clearClientSummary()
}

function openCreate() {
  editingId.value = null
  fillForm()
  clearClientSummary()
  formError.value = null
  modalOpen.value = true
}

function openEdit(p: Payment) {
  editingId.value = p.id
  fillForm(p)
  formError.value = null
  modalOpen.value = true
  if (form.clientId) loadClientSummary(form.clientId)
  else clearClientSummary()
}

async function onSubmit() {
  if (!form.clientId) {
    formError.value = 'Mijozni tanlang'
    return
  }
  saving.value = true
  formError.value = null
  try {
    const payload = {
      clientId: form.clientId,
      userId: form.userId,
      paymentTypeId: form.paymentTypeId,
      paymentAmount: form.paymentAmount,
      paymentDate: toApiDate(form.paymentDate),
      saleOrderId: form.saleOrderId || null,
      comment: form.comment || undefined,
    }
    if (editingId.value) await updatePayment(editingId.value, payload)
    else await createPayment(payload)
    modalOpen.value = false
    clearClientSummary()
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(p: Payment) {
  if (!confirm(`To‘lov #${p.id} o‘chirilsinmi?`)) return
  try {
    await deletePayment(p.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function closeTypeModal() {
  typeModal.value = false
}

function openTypeCreate() {
  typeEditingId.value = null
  typeName.value = ''
  typeFormError.value = null
  typeModal.value = true
}

function openTypeEdit(t: PaymentType) {
  typeEditingId.value = t.id
  typeName.value = t.name
  typeFormError.value = null
  typeModal.value = true
}

async function onTypeSubmit() {
  typeSaving.value = true
  typeFormError.value = null
  try {
    if (typeEditingId.value) await updatePaymentType(typeEditingId.value, typeName.value.trim())
    else await createPaymentType(typeName.value.trim())
    typeModal.value = false
    await load()
  } catch (e) {
    typeFormError.value = formatApiError(e)
  } finally {
    typeSaving.value = false
  }
}

async function onTypeDelete(t: PaymentType) {
  if (!confirm(`“${t.name}” o‘chirilsinmi?`)) return
  try {
    await deletePaymentType(t.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.th {
  padding: 0.75rem 1.25rem;
  text-align: start;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
}
.th-actions {
  width: 6.5rem;
  text-align: end;
  white-space: nowrap;
}
.td {
  padding: 0.875rem 1.25rem;
  font-size: 0.875rem;
  color: #4b5563;
}
.td-actions {
  width: 6.5rem;
  text-align: end;
  white-space: nowrap;
}
.empty {
  padding: 2.5rem 1rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
.lbl {
  display: block;
  margin-bottom: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}
.lbl-block {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}
.req {
  color: #ef4444;
}
.field-icon {
  position: absolute;
  top: 50%;
  inset-inline-start: 0.75rem;
  z-index: 10;
  height: 1.1rem;
  width: 1.1rem;
  transform: translateY(-50%);
  color: #98a2b3;
  pointer-events: none;
}
.field-icon-top {
  top: 0.85rem;
  transform: none;
}
.select-chevron {
  pointer-events: none;
  position: absolute;
  top: 50%;
  inset-inline-end: 0.75rem;
  height: 1rem;
  width: 1rem;
  transform: translateY(-50%);
  color: #9ca3af;
}
.field {
  height: 2.75rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem 0 2.5rem;
  font-size: 0.875rem;
  color: #1f2937;
  outline: none;
}
.field:focus {
  border-color: #9cb0ff;
  box-shadow: 0 0 0 4px rgb(70 95 255 / 10%);
}
.field-select {
  appearance: none;
  padding-inline-end: 2.5rem;
}
.field-textarea {
  height: auto;
  min-height: 4.5rem;
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
  resize: vertical;
}
.btn {
  display: inline-flex;
  height: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1.25rem;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
}
.btn:disabled {
  opacity: 0.6;
}
.btn-with-icon {
  gap: 0.4rem;
}
.ghost {
  display: inline-flex;
  height: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1.25rem;
  font-size: 0.875rem;
  color: #374151;
}
.icon-btn {
  display: inline-flex;
  height: 2.75rem;
  width: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  color: #4b5563;
  background: transparent;
}
.icon-btn:hover {
  background: #f9fafb;
}
.icon-btn:disabled {
  opacity: 0.5;
}
.close-btn {
  display: flex;
  height: 2.25rem;
  width: 2.25rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  color: #9ca3af;
}
.close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
  font-size: 0.875rem;
}
.tab {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.5rem;
  border-radius: 0.625rem;
  border: 1px solid #d1d5db;
  padding: 0 0.875rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
  background: #fff;
}
.tab.active {
  background: #465fff;
  color: #fff;
  border-color: #465fff;
}
.tab-count {
  display: inline-flex;
  min-width: 1.25rem;
  height: 1.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 0.35rem;
  font-size: 0.7rem;
  font-weight: 600;
  background: rgb(0 0 0 / 8%);
}
.tab.active .tab-count {
  background: rgb(255 255 255 / 20%);
}
.type-badge {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 0.35rem;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #374151;
}
.overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  padding: 1rem;
}
.modal {
  width: 100%;
  max-width: 28rem;
  max-height: calc(100vh - 2rem);
  overflow-y: auto;
  border-radius: 1rem;
  background: #fff;
  padding: 1.5rem;
  box-shadow: 0 20px 40px rgb(0 0 0 / 12%);
}
.modal-lg {
  max-width: 36rem;
}
.debt-card {
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 0.875rem 1rem;
}
.debt-loading {
  font-size: 0.8125rem;
  color: #6b7280;
}
.debt-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}
.debt-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #374151;
}
.debt-orders {
  font-size: 0.75rem;
  color: #6b7280;
}
.debt-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
}
.debt-item {
  border-radius: 0.5rem;
  background: #fff;
  border: 1px solid #eef0f3;
  padding: 0.5rem 0.65rem;
}
.debt-item-accent {
  border-color: #fecaca;
  background: #fff7f7;
}
.debt-lbl {
  display: block;
  font-size: 0.7rem;
  color: #6b7280;
  margin-bottom: 0.15rem;
}
.debt-val {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #1f2937;
  word-break: break-word;
}
.debt-sub {
  font-weight: 400;
  font-size: 0.7rem;
  color: #9ca3af;
}
.debt-orders-list {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-height: 8rem;
  overflow-y: auto;
}
.debt-order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 0.45rem 0.65rem;
  font-size: 0.75rem;
  color: #374151;
  cursor: pointer;
}
.debt-order-row:hover,
.debt-order-row.active {
  border-color: #9cb0ff;
  background: #eff4ff;
}
.debt-hint {
  margin-top: 0.65rem;
  font-size: 0.75rem;
  color: #6b7280;
}
:global(.dark) .debt-card {
  border-color: #344054;
  background: rgb(255 255 255 / 4%);
}
:global(.dark) .debt-title {
  color: rgba(255, 255, 255, 0.9);
}
:global(.dark) .debt-item {
  background: #101828;
  border-color: #344054;
}
:global(.dark) .debt-item-accent {
  background: rgb(240 68 56 / 8%);
  border-color: rgb(240 68 56 / 30%);
}
:global(.dark) .debt-val {
  color: rgba(255, 255, 255, 0.9);
}
:global(.dark) .debt-order-row {
  background: #101828;
  border-color: #344054;
  color: #d1d5db;
}
:global(.dark) .debt-order-row:hover,
:global(.dark) .debt-order-row.active {
  border-color: #465fff;
  background: rgb(70 95 255 / 12%);
}
:global(.dark) .lbl,
:global(.dark) .lbl-block {
  color: #9ca3af;
}
:global(.dark) .field {
  border-color: #344054;
  color: rgba(255, 255, 255, 0.9);
}
:global(.dark) .ghost,
:global(.dark) .icon-btn,
:global(.dark) .tab {
  border-color: #344054;
  color: #d1d5db;
  background: transparent;
}
:global(.dark) .icon-btn:hover,
:global(.dark) .close-btn:hover {
  background: rgb(255 255 255 / 5%);
  color: rgba(255, 255, 255, 0.8);
}
:global(.dark) .modal {
  background: #101828;
  border: 1px solid #344054;
}
:global(.dark) .type-badge {
  border-color: #344054;
  background: rgb(255 255 255 / 4%);
  color: #d1d5db;
}
:global(.dark) .td {
  color: #9ca3af;
}
</style>
