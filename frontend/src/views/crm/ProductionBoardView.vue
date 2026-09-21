<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Sex board" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Sex board</h3>
        <select v-model.number="workshopId" class="field sm:w-64" @change="loadBoard">
          <option :value="0" disabled>Sex tanlang</option>
          <option v-for="w in workshops" :key="w.id" :value="w.id">{{ w.name }}</option>
        </select>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">PO #</th>
              <th class="th">Savdo</th>
              <th class="th">Mijoz</th>
              <th class="th">Assignment</th>
              <th class="th">Holat</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="!workshopId"><td colspan="6" class="empty">Sex tanlang</td></tr>
            <tr v-else-if="items.length === 0"><td colspan="6" class="empty">Navbat bo‘sh</td></tr>
            <tr v-for="o in items" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ o.id }}</td>
              <td class="td">#{{ o.saleOrderId }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ o.clientFullName || '—' }}</td>
              <td class="td">{{ o.currentAssignmentStatus || '—' }}</td>
              <td class="td">{{ o.productionStatus || '—' }}</td>
              <td class="td text-right">
                <div class="inline-flex flex-wrap justify-end gap-1.5">
                  <button
                    v-if="o.currentAssignmentStatus === 'PENDING'"
                    type="button"
                    class="btn-sm"
                    @click="onStart(o)"
                  >
                    Boshlash
                  </button>
                  <button type="button" class="btn-sm alt" @click="openRedirect(o)">Yo‘naltirish</button>
                  <button type="button" class="btn-sm ok" @click="onComplete(o)">Yakunlash</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="redirectOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="redirectOpen = false">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">Keyingi sexga yo‘naltirish</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onRedirect">
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Keyingi sex *</label>
            <select v-model.number="nextWorkshopId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option
                v-for="w in redirectWorkshops"
                :key="w.id"
                :value="w.id"
              >
                {{ w.name }}
              </option>
            </select>
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Izoh</label>
            <input v-model="note" class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="redirectOpen = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Yo‘naltirish' }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  completeProduction,
  fetchProductionBoard,
  redirectProduction,
  startProduction,
  type ProductionOrder,
} from '@/api/production'
import { fetchActiveWorkshops, type Workshop } from '@/api/workshops'
import { ApiError } from '@/api/http'

const workshops = ref<Workshop[]>([])
const workshopId = ref(0)
const items = ref<ProductionOrder[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const redirectOpen = ref(false)
const selected = ref<ProductionOrder | null>(null)
const nextWorkshopId = ref(0)
const note = ref('')
const formError = ref<string | null>(null)
const saving = ref(false)

const redirectWorkshops = computed(() =>
  workshops.value.filter((w) => w.id !== selected.value?.currentWorkshopId),
)

async function loadWorkshops() {
  const res = await fetchActiveWorkshops()
  workshops.value = res.data || []
  if (!workshopId.value && workshops.value[0]) {
    workshopId.value = workshops.value[0].id
  }
}

async function loadBoard() {
  if (!workshopId.value) {
    items.value = []
    return
  }
  loading.value = true
  error.value = null
  try {
    const res = await fetchProductionBoard(workshopId.value)
    items.value = res.data || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

async function onStart(o: ProductionOrder) {
  try {
    await startProduction(o.id)
    await loadBoard()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Boshlashda xatolik'
  }
}

function openRedirect(o: ProductionOrder) {
  selected.value = o
  nextWorkshopId.value = 0
  note.value = ''
  formError.value = null
  redirectOpen.value = true
}

async function onRedirect() {
  if (!selected.value || !nextWorkshopId.value) return
  saving.value = true
  formError.value = null
  try {
    await redirectProduction(selected.value.id, nextWorkshopId.value, note.value.trim() || undefined)
    redirectOpen.value = false
    await loadBoard()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Yo‘naltirishda xatolik'
  } finally {
    saving.value = false
  }
}

async function onComplete(o: ProductionOrder) {
  if (!confirm(`#${o.id} ishlab chiqarish yakunlansinmi?`)) return
  try {
    await completeProduction(o.id)
    await loadBoard()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yakunlashda xatolik'
  }
}

onMounted(async () => {
  try {
    await loadWorkshops()
    await loadBoard()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  }
})
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn-sm { display: inline-flex; height: 2rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 0.75rem; font-size: 0.75rem; font-weight: 500; color: #fff; }
.btn-sm.alt { background: #f79009; }
.btn-sm.ok { background: #12b76a; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
</style>
