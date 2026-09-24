<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Rollar" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="ok" class="ok mb-4">{{ ok }}</div>

    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Rollar</h3>
        <button type="button" class="btn" @click="openCreate">+ Rol</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Nomi</th>
              <th class="th">Status</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in roles" :key="r.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ r.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ r.name }}</td>
              <td class="td">{{ r.status || 'ACTIVE' }}</td>
              <td class="td text-right">
                <RowActions @edit="openEdit(r)" @delete="onDelete(r)">
                  <button
                    type="button"
                    class="inline-flex h-8 items-center gap-1 rounded-lg px-2 text-xs font-medium text-brand-500 transition hover:bg-brand-50 dark:hover:bg-brand-500/10"
                    title="Ruxsatlar"
                    @click="openPerms(r)"
                  >
                    <Shield class="h-4 w-4" />
                    Ruxsatlar
                  </button>
                </RowActions>
              </td>
            </tr>
            <tr v-if="roles.length === 0">
              <td colspan="4" class="empty">Rol yo‘q</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create / rename role -->
    <div v-if="nameModal" class="overlay">
      <div class="modal">
        <h3 class="mb-4 text-lg font-semibold text-gray-800">
          {{ editingId ? 'Rolni tahrirlash' : 'Yangi rol' }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSaveName">
          <input v-model="roleName" required class="field" placeholder="Masalan: MANAGER" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="nameModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">
              {{ saving ? '...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Assign permissions (separate step) -->
    <div v-if="permModal && permRole" class="overlay">
      <div class="modal modal-lg">
        <div class="mb-4 flex items-start justify-between gap-3">
          <div>
            <h3 class="text-lg font-semibold text-gray-800">Ruxsatlar</h3>
            <p class="mt-1 text-sm text-gray-500">
              <span class="font-medium text-brand-500">{{ permRole.name }}</span> roli uchun
              ruxsatlarni belgilang, so‘ng Saqlash ni bosing.
            </p>
          </div>
          <button type="button" class="ghost !h-8 !px-2 text-sm" @click="closePerms">✕</button>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <button type="button" class="ghost !h-8 text-xs" @click="selectAll">Hammasini belgilash</button>
          <button type="button" class="ghost !h-8 text-xs" @click="clearAll">Tozalash</button>
          <span class="ms-auto text-xs text-gray-500">{{ selectedIds.size }} / {{ allPerms.length }}</span>
        </div>
        <div class="max-h-[50vh] overflow-y-auto rounded-xl border border-gray-100 p-3">
          <div class="grid gap-2 sm:grid-cols-2">
            <label
              v-for="p in allPerms"
              :key="p.id"
              class="flex cursor-pointer items-start gap-2 rounded-lg px-2 py-1.5 text-sm text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-white/[0.03]"
            >
              <input
                type="checkbox"
                class="mt-0.5"
                :checked="selectedIds.has(p.id)"
                @change="toggleDraft(p.id, ($event.target as HTMLInputElement).checked)"
              />
              <span>
                <span class="font-medium">{{ permissionLabel(p.name, t) }}</span>
                <span class="mt-0.5 block text-xs text-gray-400">{{ p.name }}</span>
              </span>
            </label>
          </div>
          <p v-if="allPerms.length === 0" class="py-8 text-center text-sm text-gray-500">Ruxsatlar yo‘q</p>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <button type="button" class="ghost" @click="closePerms">Bekor</button>
          <button type="button" class="btn" :disabled="saving" @click="onSavePerms">
            {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
          </button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import { Shield } from 'lucide-vue-next'
import {
  assignPermission,
  createRole,
  deleteRole,
  fetchAllPermissions,
  fetchRolePermissions,
  fetchRoles,
  removePermission,
  updateRole,
  type PermissionItem,
  type RoleItem,
} from '@/api/roles'
import { formatApiError } from '@/api/http'
import { permissionLabel } from '@/utils/permissions'

const { t } = useI18n()
const roles = ref<RoleItem[]>([])
const allPerms = ref<PermissionItem[]>([])
const permRole = ref<RoleItem | null>(null)
const selectedIds = ref<Set<number>>(new Set())
const initialIds = ref<Set<number>>(new Set())
const error = ref<string | null>(null)
const ok = ref<string | null>(null)
const formError = ref<string | null>(null)
const nameModal = ref(false)
const permModal = ref(false)
const editingId = ref<number | null>(null)
const roleName = ref('')
const saving = ref(false)

async function load() {
  try {
    const [r, p] = await Promise.all([fetchRoles(), fetchAllPermissions()])
    roles.value = r.data || []
    allPerms.value = p.data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function openCreate() {
  editingId.value = null
  roleName.value = ''
  formError.value = null
  nameModal.value = true
}

function openEdit(r: RoleItem) {
  editingId.value = r.id
  roleName.value = r.name
  formError.value = null
  nameModal.value = true
}

async function onSaveName() {
  formError.value = null
  saving.value = true
  try {
    const name = roleName.value.trim()
    if (editingId.value) {
      await updateRole(editingId.value, name)
      nameModal.value = false
      ok.value = 'Rol yangilandi'
      await load()
    } else {
      const res = await createRole(name)
      nameModal.value = false
      ok.value = 'Rol yaratildi — endi ruxsatlarni belgilang'
      await load()
      const created = res.data || roles.value.find((x) => x.name === name)
      if (created) await openPerms(created)
    }
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(r: RoleItem) {
  if (!confirm(`“${r.name}” o‘chirilsinmi?`)) return
  try {
    await deleteRole(r.id)
    if (permRole.value?.id === r.id) closePerms()
    ok.value = 'Rol o‘chirildi'
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function openPerms(r: RoleItem) {
  permRole.value = r
  formError.value = null
  permModal.value = true
  try {
    const perms = (await fetchRolePermissions(r.id)).data || []
    const ids = new Set(perms.map((p) => p.id))
    selectedIds.value = new Set(ids)
    initialIds.value = new Set(ids)
  } catch (e) {
    formError.value = formatApiError(e)
  }
}

function closePerms() {
  permModal.value = false
  permRole.value = null
  selectedIds.value = new Set()
  initialIds.value = new Set()
}

function toggleDraft(id: number, on: boolean) {
  const next = new Set(selectedIds.value)
  if (on) next.add(id)
  else next.delete(id)
  selectedIds.value = next
}

function selectAll() {
  selectedIds.value = new Set(allPerms.value.map((p) => p.id))
}

function clearAll() {
  selectedIds.value = new Set()
}

async function onSavePerms() {
  if (!permRole.value) return
  formError.value = null
  saving.value = true
  const roleId = permRole.value.id
  const toAdd = [...selectedIds.value].filter((id) => !initialIds.value.has(id))
  const toRemove = [...initialIds.value].filter((id) => !selectedIds.value.has(id))
  try {
    for (const id of toAdd) await assignPermission(roleId, id)
    for (const id of toRemove) await removePermission(roleId, id)
    ok.value = `“${permRole.value.name}” ruxsatlari saqlandi`
    closePerms()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
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
.empty {
  padding: 2rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
.field {
  height: 2.5rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 0.75rem;
}
.btn {
  height: 2.5rem;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1rem;
  color: #fff;
}
.ghost {
  height: 2.5rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1rem;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
}
.ok {
  border-radius: 0.5rem;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  padding: 0.75rem;
  color: #166534;
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
  border-radius: 1rem;
  background: #fff;
  padding: 1.25rem;
}
.modal-lg {
  max-width: 40rem;
}
</style>
