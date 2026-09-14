<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Mahsulotlar" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Mahsulotlar</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">Tovarlar CRUD</p>
        </div>
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
          <input
            v-model="search"
            type="search"
            placeholder="Qidiruv..."
            class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white/90 sm:w-56"
          />
          <button
            type="button"
            class="inline-flex h-10 items-center justify-center rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600"
            @click="openCreate"
          >
            + Yangi mahsulot
          </button>
        </div>
      </div>
      <div
        v-if="error"
        class="mx-5 mt-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400"
      >
        {{ error }}
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">#</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Nomi</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Guruh</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Birlik</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Turi</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Sotish</th>
              <th class="px-5 py-3 text-right text-xs font-medium text-gray-500">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="px-5 py-10 text-center text-sm text-gray-500">Yuklanmoqda...</td>
            </tr>
            <tr v-else-if="filtered.length === 0">
              <td colspan="7" class="px-5 py-10 text-center text-sm text-gray-500">Mahsulot yo‘q</td>
            </tr>
            <tr
              v-for="g in filtered"
              :key="g.id"
              class="border-b border-gray-100 dark:border-gray-800"
            >
              <td class="px-5 py-3 text-sm text-gray-500">{{ g.id }}</td>
              <td class="px-5 py-3 text-sm font-medium text-gray-800 dark:text-white/90">{{ g.name }}</td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">{{ g.goodsGroupName || '—' }}</td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">{{ g.unitTypeName || '—' }}</td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">{{ g.typeLabel || g.type || '—' }}</td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">{{ formatMoney(g.priceSelling) }}</td>
              <td class="px-5 py-3 text-right text-sm">
                <RowActions @edit="openEdit(g)" @delete="onDelete(g)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      v-if="modalOpen"
      class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4"
      @click.self="modalOpen = false"
    >
      <div class="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 shadow-xl dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? 'Mahsulotni tahrirlash' : 'Yangi mahsulot' }}
        </h3>
        <div
          v-if="formError"
          class="mb-3 rounded-lg border border-error-200 bg-error-50 px-3 py-2 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400"
        >
          {{ formError }}
        </div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Nomi *</label>
            <input v-model="form.name" required class="field" />
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Guruh *</label>
              <select v-model.number="form.goodsGroupId" required class="field">
                <option :value="0" disabled>Tanlang</option>
                <option v-for="gr in groups" :key="gr.id" :value="gr.id">{{ gr.name }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Birlik *</label>
              <select v-model.number="form.unitTypeId" required class="field">
                <option :value="0" disabled>Tanlang</option>
                <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
              </select>
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Turi *</label>
              <select v-model="form.type" required class="field">
                <option value="PRODUCT">PRODUCT</option>
                <option value="SERVICE">SERVICE</option>
                <option value="WINDOW">WINDOW</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Tannarx *</label>
              <input v-model.number="form.priceCost" type="number" min="0" step="0.01" required class="field" />
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Sotish *</label>
              <input v-model.number="form.priceSelling" type="number" min="0" step="0.01" required class="field" />
            </div>
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Barcode</label>
            <input v-model="form.barcode" class="field" />
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Rasm (ixtiyoriy)</label>
            <input type="file" accept="image/*" class="field" @change="onFile" />
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm dark:border-gray-700" @click="modalOpen = false">Bekor</button>
            <button type="submit" :disabled="saving" class="h-10 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60">
              {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
import { ApiError } from '@/api/http'

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
const photoFile = ref<File | null>(null)

const form = reactive({
  name: '',
  goodsGroupId: 0,
  unitTypeId: 0,
  type: 'PRODUCT',
  priceCost: 0,
  priceSelling: 0,
  barcode: '',
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((g) =>
    [g.name, g.goodsGroupName, g.barcode, g.type]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q)),
  )
})

function formatMoney(v?: number) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(Number(v))
}

function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  photoFile.value = input.files?.[0] || null
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
    items.value = goodsRes.data || []
    groups.value = groupRes || []
    units.value = unitRes || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    goodsGroupId: groups.value[0]?.id || 0,
    unitTypeId: units.value[0]?.id || 0,
    type: 'PRODUCT',
    priceCost: 0,
    priceSelling: 0,
    barcode: '',
  })
  photoFile.value = null
  formError.value = null
  modalOpen.value = true
}

function openEdit(g: Goods) {
  editingId.value = g.id
  Object.assign(form, {
    name: g.name || '',
    goodsGroupId: g.goodsGroupId || 0,
    unitTypeId: g.unitTypeId || 0,
    type: g.type || 'PRODUCT',
    priceCost: Number(g.priceCost || 0),
    priceSelling: Number(g.priceSelling || 0),
    barcode: g.barcode || '',
  })
  photoFile.value = null
  formError.value = null
  modalOpen.value = true
}

function buildFormData() {
  const fd = new FormData()
  fd.append('name', form.name.trim())
  fd.append('goodsGroupId', String(form.goodsGroupId))
  fd.append('unitTypeId', String(form.unitTypeId))
  fd.append('type', form.type)
  fd.append('priceCost', String(form.priceCost))
  fd.append('priceSelling', String(form.priceSelling))
  if (form.barcode.trim()) fd.append('barcode', form.barcode.trim())
  if (photoFile.value) fd.append('photo', photoFile.value)
  return fd
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  try {
    const fd = buildFormData()
    if (editingId.value) await updateGoods(editingId.value, fd)
    else await createGoods(fd)
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError || e instanceof Error ? e.message : 'Saqlashda xatolik'
  } finally {
    saving.value = false
  }
}

async function onDelete(g: Goods) {
  if (!confirm(`“${g.name}” o‘chirilsinmi?`)) return
  try {
    await deleteGoods(g.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'O‘chirishda xatolik'
  }
}

onMounted(load)
</script>

<style scoped>
.field {
  height: 2.5rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
:global(.dark) .field {
  border-color: #374151;
  color: rgba(255, 255, 255, 0.9);
}
</style>
