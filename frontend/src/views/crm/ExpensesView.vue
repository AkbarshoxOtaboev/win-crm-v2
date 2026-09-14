<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Xarajatlar" />

    <div class="mb-4 flex gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'expenses' }" @click="tab = 'expenses'">Xarajatlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'categories' }" @click="tab = 'categories'">Kategoriyalar</button>
    </div>

    <!-- Expenses -->
    <div v-show="tab === 'expenses'" class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Xarajatlar</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">Expense CRUD</p>
        </div>
        <div class="flex gap-2">
          <input v-model="expenseSearch" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
          <button type="button" class="btn" @click="openExpenseCreate">+ Yangi xarajat</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Kategoriya</th>
              <th class="th">Summa</th>
              <th class="th">Sana</th>
              <th class="th">Izoh</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filteredExpenses.length === 0"><td colspan="6" class="empty">Xarajat yo‘q</td></tr>
            <tr v-for="e in filteredExpenses" :key="e.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ e.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ e.categoryName || '—' }}</td>
              <td class="td">{{ money(e.amount) }}</td>
              <td class="td">{{ e.expenseDate }}</td>
              <td class="td">{{ e.description || '—' }}</td>
              <td class="td text-right">
                <RowActions @edit="openExpenseEdit(e)" @delete="onExpenseDelete(e)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Categories -->
    <div v-show="tab === 'categories'" class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Kategoriyalar</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">Expense category CRUD</p>
        </div>
        <button type="button" class="btn" @click="openCategoryCreate">+ Yangi kategoriya</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Nomi</th>
              <th class="th">Izoh</th>
              <th class="th">Status</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="categories.length === 0"><td colspan="5" class="empty">Kategoriya yo‘q</td></tr>
            <tr v-for="c in categories" :key="c.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ c.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ c.name }}</td>
              <td class="td">{{ c.description || '—' }}</td>
              <td class="td">{{ c.status || 'ACTIVE' }}</td>
              <td class="td text-right">
                <RowActions @edit="openCategoryEdit(c)" @delete="onCategoryDelete(c)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Expense modal -->
    <div v-if="expenseModal" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="expenseModal = false">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ expenseEditingId ? 'Xarajatni tahrirlash' : 'Yangi xarajat' }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onExpenseSubmit">
          <div>
            <label class="lbl">Kategoriya *</label>
            <select v-model.number="expenseForm.categoryId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">Summa *</label>
              <input v-model.number="expenseForm.amount" type="number" min="0.01" step="0.01" required class="field" />
            </div>
            <div>
              <label class="lbl">Sana *</label>
              <input v-model="expenseForm.expenseDate" type="date" required class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">Izoh</label>
            <input v-model="expenseForm.description" class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="expenseModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Saqlash' }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Category modal -->
    <div v-if="categoryModal" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="categoryModal = false">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ categoryEditingId ? 'Kategoriyani tahrirlash' : 'Yangi kategoriya' }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onCategorySubmit">
          <div>
            <label class="lbl">Nomi *</label>
            <input v-model="categoryForm.name" required class="field" />
          </div>
          <div>
            <label class="lbl">Izoh</label>
            <input v-model="categoryForm.description" class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="categoryModal = false">Bekor</button>
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
  createExpense,
  createExpenseCategory,
  deleteExpense,
  deleteExpenseCategory,
  fetchExpenseCategories,
  fetchExpenses,
  updateExpense,
  updateExpenseCategory,
  type Expense,
  type ExpenseCategory,
} from '@/api/expenses'
import { ApiError } from '@/api/http'

const tab = ref<'expenses' | 'categories'>('expenses')
const expenses = ref<Expense[]>([])
const categories = ref<ExpenseCategory[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const expenseSearch = ref('')

const expenseModal = ref(false)
const expenseEditingId = ref<number | null>(null)
const expenseForm = reactive({ categoryId: 0, amount: 0, expenseDate: '', description: '' })

const categoryModal = ref(false)
const categoryEditingId = ref<number | null>(null)
const categoryForm = reactive({ name: '', description: '' })

const filteredExpenses = computed(() => {
  const q = expenseSearch.value.trim().toLowerCase()
  if (!q) return expenses.value
  return expenses.value.filter((e) =>
    [e.categoryName, e.description, String(e.amount)].filter(Boolean).some((v) => String(v).toLowerCase().includes(q)),
  )
})

function money(v?: number) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(Number(v))
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [expRes, catRes] = await Promise.all([fetchExpenses(), fetchExpenseCategories()])
    expenses.value = expRes.data?.content || []
    categories.value = catRes.data?.content || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

function openExpenseCreate() {
  expenseEditingId.value = null
  expenseForm.categoryId = categories.value[0]?.id || 0
  expenseForm.amount = 0
  expenseForm.expenseDate = today()
  expenseForm.description = ''
  formError.value = null
  expenseModal.value = true
}

function openExpenseEdit(e: Expense) {
  expenseEditingId.value = e.id
  expenseForm.categoryId = e.categoryId
  expenseForm.amount = Number(e.amount)
  expenseForm.expenseDate = e.expenseDate
  expenseForm.description = e.description || ''
  formError.value = null
  expenseModal.value = true
}

async function onExpenseSubmit() {
  saving.value = true
  formError.value = null
  try {
    const payload = {
      categoryId: expenseForm.categoryId,
      amount: expenseForm.amount,
      expenseDate: expenseForm.expenseDate,
      description: expenseForm.description.trim() || undefined,
    }
    if (expenseEditingId.value) await updateExpense(expenseEditingId.value, payload)
    else await createExpense(payload)
    expenseModal.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Saqlashda xatolik'
  } finally {
    saving.value = false
  }
}

async function onExpenseDelete(e: Expense) {
  if (!confirm(`Xarajat #${e.id} o‘chirilsinmi?`)) return
  try {
    await deleteExpense(e.id)
    await load()
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : 'O‘chirishda xatolik'
  }
}

function openCategoryCreate() {
  categoryEditingId.value = null
  categoryForm.name = ''
  categoryForm.description = ''
  formError.value = null
  categoryModal.value = true
}

function openCategoryEdit(c: ExpenseCategory) {
  categoryEditingId.value = c.id
  categoryForm.name = c.name
  categoryForm.description = c.description || ''
  formError.value = null
  categoryModal.value = true
}

async function onCategorySubmit() {
  saving.value = true
  formError.value = null
  try {
    const payload = {
      name: categoryForm.name.trim(),
      description: categoryForm.description.trim() || undefined,
    }
    if (categoryEditingId.value) await updateExpenseCategory(categoryEditingId.value, payload)
    else await createExpenseCategory(payload)
    categoryModal.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Saqlashda xatolik'
  } finally {
    saving.value = false
  }
}

async function onCategoryDelete(c: ExpenseCategory) {
  if (!confirm(`“${c.name}” o‘chirilsinmi?`)) return
  try {
    await deleteExpenseCategory(c.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'O‘chirishda xatolik'
  }
}

onMounted(load)
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.875rem; color: #4b5563; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 1rem; font-size: 0.875rem; color: #4b5563; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
</style>
