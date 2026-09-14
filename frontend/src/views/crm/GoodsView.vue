<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Mahsulotlar" />
    <div class="mb-4 flex gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'goods' }" @click="tab = 'goods'">Mahsulotlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'groups' }" @click="tab = 'groups'">Guruhlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'units' }" @click="tab = 'units'">Birliklar</button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div v-show="tab === 'goods'" class="card">
      <div class="head">
        <h3 class="title">Mahsulotlar</h3>
        <div class="flex gap-2">
          <input v-model="search" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
          <button type="button" class="btn" @click="openCreate">+ Yangi mahsulot</button>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Nomi</th>
              <th class="th">Guruh</th>
              <th class="th">Birlik</th>
              <th class="th">Turi</th>
              <th class="th">Sotish</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">Mahsulot yo‘q</td></tr>
            <tr v-for="g in filtered" :key="g.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ g.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ g.name }}</td>
              <td class="td">{{ g.goodsGroupName || '—' }}</td>
              <td class="td">{{ g.unitTypeName || '—' }}</td>
              <td class="td">{{ g.typeLabel || g.type || '—' }}</td>
              <td class="td">{{ money(g.priceSelling) }}</td>
              <td class="td text-right"><RowActions @edit="openEdit(g)" @delete="onDelete(g)" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-show="tab === 'groups'" class="card">
      <div class="head">
        <h3 class="title">Guruhlar</h3>
        <button type="button" class="btn" @click="openGroupCreate">+ Yangi guruh</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">Nomi</th>
            <th class="th">Status</th>
            <th class="th">Yoqish / o‘chirish</th>
            <th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="groups.length === 0"><td colspan="5" class="empty">Guruh yo‘q</td></tr>
          <tr v-for="gr in groups" :key="gr.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ gr.id }}</td>
            <td class="td font-medium text-gray-800 dark:text-white/90">{{ gr.name }}</td>
            <td class="td">
              <span
                class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium"
                :class="isActiveStatus(gr.status) ? 'bg-success-50 text-success-600' : 'bg-gray-100 text-gray-500'"
              >
                {{ isActiveStatus(gr.status) ? 'Active' : 'No active' }}
              </span>
            </td>
            <td class="td">
              <button
                type="button"
                class="status-toggle"
                :class="isActiveStatus(gr.status) ? 'on' : 'off'"
                :aria-pressed="isActiveStatus(gr.status)"
                :title="isActiveStatus(gr.status) ? 'O‘chirish' : 'Yoqish'"
                @click="onGroupStatus(gr)"
              >
                <span class="status-knob" />
              </button>
            </td>
            <td class="td text-right"><RowActions @edit="openGroupEdit(gr)" @delete="onGroupDelete(gr)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'units'" class="card">
      <div class="head">
        <h3 class="title">Birliklar</h3>
        <button type="button" class="btn" @click="openUnitCreate">+ Yangi birlik</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">Nomi</th>
            <th class="th">Status</th>
            <th class="th">Yoqish / o‘chirish</th>
            <th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="units.length === 0"><td colspan="5" class="empty">Birlik yo‘q</td></tr>
          <tr v-for="u in units" :key="u.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ u.id }}</td>
            <td class="td font-medium text-gray-800 dark:text-white/90">{{ u.name }}</td>
            <td class="td">
              <span
                class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium"
                :class="isActiveStatus(u.status) ? 'bg-success-50 text-success-600' : 'bg-gray-100 text-gray-500'"
              >
                {{ isActiveStatus(u.status) ? 'Active' : 'No active' }}
              </span>
            </td>
            <td class="td">
              <button
                type="button"
                class="status-toggle"
                :class="isActiveStatus(u.status) ? 'on' : 'off'"
                :aria-pressed="isActiveStatus(u.status)"
                :title="isActiveStatus(u.status) ? 'O‘chirish' : 'Yoqish'"
                @click="onUnitStatus(u)"
              >
                <span class="status-knob" />
              </button>
            </td>
            <td class="td text-right"><RowActions @edit="openUnitEdit(u)" @delete="onUnitDelete(u)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="modalOpen" class="overlay" @click.self="modalOpen = false">
      <div class="modal">
        <h3 class="title mb-4">{{ editingId ? 'Mahsulotni tahrirlash' : 'Yangi mahsulot' }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">Nomi *</label>
            <input v-model="form.name" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">Guruh *</label>
              <select v-model.number="form.goodsGroupId" required class="field">
                <option :value="0" disabled>Tanlang</option>
                <option v-for="gr in groups" :key="gr.id" :value="gr.id">{{ gr.name }}</option>
              </select>
            </div>
            <div>
              <label class="lbl">Birlik *</label>
              <select v-model.number="form.unitTypeId" required class="field">
                <option :value="0" disabled>Tanlang</option>
                <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
              </select>
            </div>
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div>
              <label class="lbl">Turi *</label>
              <select v-model="form.type" required class="field">
                <option value="PRODUCT">PRODUCT</option>
                <option value="SERVICE">SERVICE</option>
                <option value="WINDOW">WINDOW</option>
              </select>
            </div>
            <div>
              <label class="lbl">Tannarx *</label>
              <input v-model.number="form.priceCost" type="number" min="0" step="0.01" required class="field" />
            </div>
            <div>
              <label class="lbl">Sotish *</label>
              <input v-model.number="form.priceSelling" type="number" min="0" step="0.01" required class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">Barcode</label>
            <input v-model="form.barcode" class="field" />
          </div>
          <div>
            <label class="lbl">Rasm</label>
            <input type="file" accept="image/*" class="field" @change="onFile" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="modalOpen = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Saqlash' }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="nameModal" class="overlay" @click.self="nameModal = false">
      <div class="modal max-w-md">
        <h3 class="title mb-4">{{ nameKind === 'group' ? 'Guruh' : 'Birlik' }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onNameSubmit">
          <div>
            <label class="lbl">Nomi *</label>
            <input v-model="nameValue" required class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="nameModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Saqlash' }}</button>
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
  changeGoodsGroupStatus,
  changeUnitTypeStatus,
  createGoods,
  createGoodsGroup,
  createUnitType,
  deleteGoods,
  deleteGoodsGroup,
  deleteUnitType,
  fetchGoods,
  fetchGoodsGroups,
  fetchUnitTypes,
  updateGoods,
  updateGoodsGroup,
  updateUnitType,
  type Goods,
  type GoodsGroup,
  type UnitType,
} from '@/api/goods'
import { formatApiError } from '@/api/http'
import { money } from '@/utils/format'

const tab = ref<'goods' | 'groups' | 'units'>('goods')
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
const nameModal = ref(false)
const nameKind = ref<'group' | 'unit'>('group')
const nameEditingId = ref<number | null>(null)
const nameValue = ref('')

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
  const list = !q
    ? items.value
    : items.value.filter((g) =>
        [g.name, g.goodsGroupName, g.barcode, g.type]
          .filter(Boolean)
          .some((v) => String(v).toLowerCase().includes(q)),
      )
  return [...list].sort((a, b) => Number(a.id) - Number(b.id))
})

function byIdAsc<T extends { id: number }>(arr: T[]) {
  return [...arr].sort((a, b) => Number(a.id) - Number(b.id))
}

function onFile(e: Event) {
  photoFile.value = (e.target as HTMLInputElement).files?.[0] || null
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
    items.value = byIdAsc(goodsRes.data || [])
    groups.value = byIdAsc(groupRes || [])
    units.value = byIdAsc(unitRes || [])
  } catch (e) {
    error.value = formatApiError(e, 'Yuklashda xatolik')
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
    formError.value = formatApiError(e, 'Saqlashda xatolik')
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
    error.value = formatApiError(e, 'O‘chirishda xatolik')
  }
}

function openGroupCreate() {
  nameKind.value = 'group'
  nameEditingId.value = null
  nameValue.value = ''
  formError.value = null
  nameModal.value = true
}

function openGroupEdit(gr: GoodsGroup) {
  nameKind.value = 'group'
  nameEditingId.value = gr.id
  nameValue.value = gr.name
  formError.value = null
  nameModal.value = true
}

function openUnitCreate() {
  nameKind.value = 'unit'
  nameEditingId.value = null
  nameValue.value = ''
  formError.value = null
  nameModal.value = true
}

function openUnitEdit(u: UnitType) {
  nameKind.value = 'unit'
  nameEditingId.value = u.id
  nameValue.value = u.name
  formError.value = null
  nameModal.value = true
}

async function onNameSubmit() {
  saving.value = true
  formError.value = null
  try {
    const name = nameValue.value.trim()
    if (nameKind.value === 'group') {
      if (nameEditingId.value) await updateGoodsGroup(nameEditingId.value, name)
      else await createGoodsGroup(name)
    } else {
      if (nameEditingId.value) await updateUnitType(nameEditingId.value, name)
      else await createUnitType(name)
    }
    nameModal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e, 'Saqlashda xatolik')
  } finally {
    saving.value = false
  }
}

async function onGroupDelete(gr: GoodsGroup) {
  if (!confirm(`“${gr.name}” o‘chirilsinmi?`)) return
  try {
    await deleteGoodsGroup(gr.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, 'O‘chirishda xatolik')
  }
}

async function onUnitDelete(u: UnitType) {
  if (!confirm(`“${u.name}” o‘chirilsinmi?`)) return
  try {
    await deleteUnitType(u.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, 'O‘chirishda xatolik')
  }
}

function isActiveStatus(status?: string) {
  return !status || status === 'ACTIVE'
}

async function onGroupStatus(gr: GoodsGroup) {
  try {
    await changeGoodsGroupStatus(gr.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, 'Statusni o‘zgartirishda xatolik')
  }
}

async function onUnitStatus(u: UnitType) {
  try {
    await changeUnitTypeStatus(u.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, 'Statusni o‘zgartirishda xatolik')
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { font-size: 0.875rem; color: #6b7280; }
.head { display: flex; flex-wrap: wrap; gap: 0.75rem; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.5rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; min-width: 10.5rem; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.875rem; color: #4b5563; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 1rem; font-size: 0.875rem; color: #4b5563; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 32rem; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.status-toggle {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 2.75rem;
  height: 1.5rem;
  border-radius: 9999px;
  padding: 0.125rem;
  transition: background 0.2s ease;
}
.status-toggle.on { background: #12b76a; }
.status-toggle.off { background: #d1d5db; }
.status-knob {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 9999px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,.15);
  transition: transform 0.2s ease;
}
.status-toggle.off .status-knob { transform: translateX(0); }
.status-toggle.on .status-knob { transform: translateX(1.25rem); }
:global(.dark) .card, :global(.dark) .modal { background: #111827; border-color: #1f2937; }
:global(.dark) .title { color: rgba(255,255,255,.9); }
:global(.dark) .status-toggle.off { background: #4b5563; }
</style>
