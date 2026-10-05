<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.services')" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.services') }}</h3>
          <p class="mt-0.5 text-sm text-gray-500 dark:text-gray-400">{{ t('services.subtitle') }}</p>
        </div>
        <div class="flex gap-2">
          <input v-model="search" type="search" :placeholder="t('common.search')" class="field sm:w-56" />
          <button type="button" class="btn whitespace-nowrap" :disabled="writeBlocked" @click="openCreate">{{ t('services.newService') }}</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.name') }}</th>
              <th class="th">{{ t('services.unit') }}</th>
              <th class="th">{{ t('services.price') }}</th>
              <th class="th">{{ t('services.maxDiscount') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="6" class="empty">{{ t('services.empty') }}</td></tr>
            <tr v-for="(s, i) in filtered" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ i + 1 }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ s.name }}</td>
              <td class="td">{{ s.unitTypeName || '—' }}</td>
              <td class="td whitespace-nowrap font-semibold text-gray-800 dark:text-white/90">{{ priceLabel(s) }}</td>
              <td class="td">
                <span v-if="s.maxDiscountPercent != null" class="inline-flex rounded-full bg-warning-50 px-2 py-0.5 text-xs font-medium text-warning-700 dark:bg-warning-500/10 dark:text-warning-400">
                  ≤ {{ Number(s.maxDiscountPercent) }}%
                </span>
                <span v-else class="text-gray-400">—</span>
              </td>
              <td class="td text-right"><RowActions @edit="openEdit(s)" @delete="onDelete(s)" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? t('services.editTitle') : t('services.createTitle') }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">{{ t('common.name') }} *</label>
            <input v-model="form.name" required class="field" />
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div class="col-span-2">
              <label class="lbl">{{ t('services.price') }} *</label>
              <input v-model.number="form.price" type="number" min="0" step="any" required class="field" />
            </div>
            <div>
              <label class="lbl">{{ t('services.currency') }}</label>
              <select v-model="form.currency" class="field">
                <option v-for="c in CURRENCIES" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('services.unit') }}</label>
              <select v-model.number="form.unitTypeId" class="field">
                <option :value="0">{{ t('services.unitAuto') }}</option>
                <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
              </select>
            </div>
            <div>
              <label class="lbl">{{ t('services.maxDiscount') }}</label>
              <input v-model="form.maxDiscount" type="number" min="0" max="100" step="any" class="field" />
            </div>
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm dark:border-gray-700" @click="modalOpen = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('common.save') }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Swal from 'sweetalert2'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  createGoods,
  deleteGoods,
  fetchGoods,
  fetchUnitTypes,
  updateGoods,
  type Goods,
  type UnitType,
} from '@/api/goods'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { money } from '@/utils/format'
import { BASE_CURRENCY, CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()

const items = ref<Goods[]>([])
const units = ref<UnitType[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const editingGroupId = ref<number | null>(null)

const form = reactive({
  name: '',
  price: 0,
  currency: BASE_CURRENCY as CurrencyCode,
  unitTypeId: 0,
  maxDiscount: '' as string | number,
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return items.value
    .filter((s) => !q || s.name.toLowerCase().includes(q))
    .sort((a, b) => Number(a.id) - Number(b.id))
})

function priceLabel(s: Goods) {
  return s.priceCurrency && s.priceCurrency !== BASE_CURRENCY
    ? moneyIn(s.priceSelling, s.priceCurrency, t('common.currency'))
    : money(s.priceSelling)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [goodsRes, unitRes] = await Promise.all([fetchGoods(), fetchUnitTypes()])
    items.value = (goodsRes.data || []).filter((g) => (g.type || '').toUpperCase() === 'SERVICE')
    units.value = (unitRes || []).filter((u) => !u.status || u.status === 'ACTIVE')
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (writeBlocked.value) return
  editingId.value = null
  editingGroupId.value = null
  Object.assign(form, { name: '', price: 0, currency: BASE_CURRENCY, unitTypeId: 0, maxDiscount: '' })
  formError.value = null
  modalOpen.value = true
}

function openEdit(s: Goods) {
  editingId.value = s.id
  editingGroupId.value = s.goodsGroupId || null
  Object.assign(form, {
    name: s.name || '',
    price: Number(s.priceSelling || 0),
    currency: s.priceCurrency || BASE_CURRENCY,
    unitTypeId: s.unitTypeId || 0,
    maxDiscount: s.maxDiscountPercent != null ? Number(s.maxDiscountPercent) : '',
  })
  formError.value = null
  modalOpen.value = true
}

function buildFormData() {
  const fd = new FormData()
  fd.append('name', form.name.trim())
  fd.append('type', 'SERVICE')
  fd.append('priceSelling', String(Number(form.price)))
  fd.append('priceCurrency', form.currency)
  if (form.unitTypeId) fd.append('unitTypeId', String(form.unitTypeId))
  if (editingGroupId.value) fd.append('goodsGroupId', String(editingGroupId.value))
  if (form.maxDiscount !== '' && form.maxDiscount != null) fd.append('maxDiscountPercent', String(Number(form.maxDiscount)))
  return fd
}

async function onSubmit() {
  formError.value = null
  if (!form.name.trim()) {
    formError.value = t('services.nameRequired')
    return
  }
  if (!(Number(form.price) > 0)) {
    formError.value = t('services.priceRequired')
    return
  }
  saving.value = true
  try {
    const fd = buildFormData()
    if (editingId.value) await updateGoods(editingId.value, fd)
    else await createGoods(fd)
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e, t('common.saveError'))
  } finally {
    saving.value = false
  }
}

async function onDelete(s: Goods) {
  const result = await Swal.fire({
    title: t('services.deleteTitle'),
    text: t('services.deleteText', { name: s.name }),
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: t('services.deleteYes'),
    cancelButtonText: t('common.cancel'),
    confirmButtonColor: '#d92d20',
    cancelButtonColor: '#98a2b3',
    reverseButtons: true,
  })
  if (!result.isConfirmed) return
  try {
    await deleteGoods(s.id)
    await load()
    await Swal.fire({ title: t('services.deleted'), icon: 'success', timer: 1500, showConfirmButton: false })
  } catch (e) {
    error.value = formatApiError(e, t('common.deleteError'))
  }
}

onMounted(load)
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.lbl { margin-bottom: 0.25rem; display: block; font-size: 0.875rem; color: #4b5563; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { opacity: 0.6; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
:global(html.dark .lbl) { color: #9ca3af; }
:global(html.dark select.field) { background: #111827; color: rgba(255, 255, 255, 0.92); border-color: #374151; }
</style>
