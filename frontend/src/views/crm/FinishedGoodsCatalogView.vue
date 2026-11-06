<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.finishedGoodsCatalog')" />
    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">{{ t('finishedGoods.catalogTitle') }}</h3>
          <p class="sub">{{ t('finishedGoods.catalogSubtitle') }}</p>
        </div>
        <div class="flex gap-2">
          <input v-model="search" type="search" :placeholder="t('common.search')" class="field sm:w-56" />
          <button type="button" class="btn" :disabled="writeBlocked" @click="openCreate">{{ t('finishedGoods.newProduct') }}</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.name') }}</th>
              <th class="th">{{ t('goods.group') }}</th>
              <th class="th">{{ t('goods.unit') }}</th>
              <th class="th">{{ t('goods.selling') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="6" class="empty">{{ t('finishedGoods.emptyCatalog') }}</td></tr>
            <tr v-for="g in filtered" :key="g.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ g.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ g.name }}</td>
              <td class="td">{{ g.goodsGroupName || '—' }}</td>
              <td class="td">{{ g.unitTypeName || '—' }}</td>
              <td class="td whitespace-nowrap">{{ sellingLabel(g) }}</td>
              <td class="td text-right"><RowActions @edit="openEdit(g)" @delete="onDelete(g)" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? t('finishedGoods.editTitle') : t('finishedGoods.createTitle') }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">{{ t('common.name') }} *</label>
            <input v-model="form.name" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('goods.group') }} *</label>
              <select v-model.number="form.goodsGroupId" required class="field">
                <option :value="0" disabled>{{ t('common.select') }}</option>
                <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
              </select>
            </div>
            <div>
              <label class="lbl">{{ t('goods.unit') }} *</label>
              <select v-model.number="form.unitTypeId" required class="field">
                <option :value="0" disabled>{{ t('common.select') }}</option>
                <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
              </select>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('goods.selling') }}</label>
              <input v-model.number="form.priceSelling" type="number" min="0" step="any" class="field" />
            </div>
            <div>
              <label class="lbl">{{ t('goods.priceCurrency') }}</label>
              <select v-model="form.priceCurrency" class="field">
                <option v-for="c in CURRENCIES" :key="c" :value="c">{{ c }}</option>
              </select>
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
  fetchGoodsGroups,
  fetchUnitTypes,
  updateGoods,
  type Goods,
  type GoodsGroup,
  type UnitType,
} from '@/api/goods'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { money } from '@/utils/format'
import { BASE_CURRENCY, CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()

const items = ref<Goods[]>([])
const groups = ref<GoodsGroup[]>([])
const units = ref<UnitType[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  goodsGroupId: 0,
  unitTypeId: 0,
  priceSelling: 0,
  priceCurrency: BASE_CURRENCY as CurrencyCode,
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return items.value
    .filter((g) => !q || g.name.toLowerCase().includes(q) || (g.goodsGroupName || '').toLowerCase().includes(q))
    .sort((a, b) => Number(a.id) - Number(b.id))
})

function sellingLabel(g: Goods) {
  return g.priceCurrency && g.priceCurrency !== BASE_CURRENCY
    ? moneyIn(g.priceSelling, g.priceCurrency, t('common.currency'))
    : money(g.priceSelling)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [goodsRes, groupRes, unitRes] = await Promise.all([
      fetchGoods(),
      fetchGoodsGroups(),
      fetchUnitTypes(),
    ])
    items.value = (goodsRes.data || []).filter((g) => (g.type || '').toUpperCase() === 'FINISHED_PRODUCT')
    groups.value = (groupRes || []).filter((g) => !g.status || g.status === 'ACTIVE')
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
  Object.assign(form, {
    name: '',
    goodsGroupId: groups.value[0]?.id || 0,
    unitTypeId: units.value[0]?.id || 0,
    priceSelling: 0,
    priceCurrency: BASE_CURRENCY,
  })
  formError.value = null
  modalOpen.value = true
}

function openEdit(g: Goods) {
  editingId.value = g.id
  Object.assign(form, {
    name: g.name || '',
    goodsGroupId: g.goodsGroupId || 0,
    unitTypeId: g.unitTypeId || 0,
    priceSelling: Number(g.priceSelling || 0),
    priceCurrency: g.priceCurrency || BASE_CURRENCY,
  })
  formError.value = null
  modalOpen.value = true
}

function buildFormData() {
  const fd = new FormData()
  fd.append('name', form.name.trim())
  fd.append('type', 'FINISHED_PRODUCT')
  fd.append('goodsGroupId', String(form.goodsGroupId))
  fd.append('unitTypeId', String(form.unitTypeId))
  fd.append('priceSelling', String(Number(form.priceSelling) || 0))
  fd.append('priceCurrency', form.priceCurrency)
  return fd
}

async function onSubmit() {
  formError.value = null
  if (!form.name.trim()) {
    formError.value = t('finishedGoods.nameRequired')
    return
  }
  if (!form.goodsGroupId) {
    formError.value = t('finishedGoods.groupRequired')
    return
  }
  if (!form.unitTypeId) {
    formError.value = t('finishedGoods.unitRequired')
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

async function onDelete(g: Goods) {
  const result = await Swal.fire({
    title: t('finishedGoods.deleteTitle'),
    text: t('finishedGoods.deleteText', { name: g.name }),
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: t('finishedGoods.deleteYes'),
    cancelButtonText: t('common.cancel'),
    confirmButtonColor: '#d92d20',
    reverseButtons: true,
  })
  if (!result.isConfirmed) return
  try {
    await deleteGoods(g.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, t('common.deleteError'))
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.dark .card { border-color: #1f2937; background: rgb(255 255 255 / 0.03); }
.head { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 0.75rem; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.15rem; font-size: 0.8125rem; color: #6b7280; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.lbl { margin-bottom: 0.25rem; display: block; font-size: 0.875rem; color: #4b5563; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { opacity: 0.6; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
</style>
