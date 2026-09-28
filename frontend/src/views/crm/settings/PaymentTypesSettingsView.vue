<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.paymentTypes')" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.paymentTypes') }}</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openCreate">{{ t('common.new') }}</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">{{ t('common.name') }}</th>
            <th class="th text-right">{{ t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="pt in items" :key="pt.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ pt.id }}</td>
            <td class="td font-medium">
              <span class="inline-flex items-center gap-2">
                <component :is="paymentTypeIcon(pt.icon)" class="h-4 w-4 text-brand-500" />
                {{ pt.name }}
              </span>
            </td>
            <td class="td text-right"><RowActions @edit="openEdit(pt)" @delete="onDelete(pt)" /></td>
          </tr>
          <tr v-if="items.length === 0"><td colspan="3" class="empty">{{ t('settings.paymentTypes.empty') }}</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="modal" class="overlay">
      <div class="modal">
        <form class="space-y-3" @submit.prevent="onSave">
          <input v-model="name" required class="field" :placeholder="t('shared.nameRequired')" />
          <div>
            <span class="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">{{ t('payments.typeIcon') }}</span>
            <div class="icon-grid" role="radiogroup" :aria-label="t('payments.typeIcon')">
              <button
                v-for="opt in PAYMENT_TYPE_ICONS"
                :key="opt.key"
                type="button"
                role="radio"
                class="icon-opt"
                :class="{ active: icon === opt.key }"
                :aria-checked="icon === opt.key"
                @click="icon = opt.key"
              >
                <component :is="opt.icon" class="h-5 w-5" />
                <span>{{ t(`payments.icons.${opt.key}`) }}</span>
              </button>
            </div>
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="modal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn">{{ t('common.save') }}</button>
          </div>
        </form>
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
import {
  createPaymentType,
  deletePaymentType,
  fetchPaymentTypes,
  updatePaymentType,
  type PaymentType,
} from '@/api/payments'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import {
  DEFAULT_PAYMENT_TYPE_ICON,
  PAYMENT_TYPE_ICONS,
  paymentTypeIcon,
  type PaymentTypeIconKey,
} from '@/utils/paymentTypeIcons'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()
const items = ref<PaymentType[]>([])
const error = ref<string | null>(null)
const modal = ref(false)
const editingId = ref<number | null>(null)
const name = ref('')
const icon = ref<PaymentTypeIconKey>(DEFAULT_PAYMENT_TYPE_ICON)

async function load() {
  try {
    const res = await fetchPaymentTypes()
    items.value = res.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function openCreate() {
  editingId.value = null
  name.value = ''
  icon.value = DEFAULT_PAYMENT_TYPE_ICON
  modal.value = true
}

function openEdit(pt: PaymentType) {
  editingId.value = pt.id
  name.value = pt.name
  icon.value = (pt.icon as PaymentTypeIconKey) || DEFAULT_PAYMENT_TYPE_ICON
  modal.value = true
}

async function onSave() {
  try {
    if (editingId.value) await updatePaymentType(editingId.value, name.value.trim(), icon.value)
    else await createPaymentType(name.value.trim(), icon.value)
    modal.value = false
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onDelete(pt: PaymentType) {
  if (!confirm(t('common.deleteConfirmNamed', { name: pt.name }))) return
  try {
    await deletePaymentType(pt.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; }
.btn { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; color: #fff; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 24rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
.icon-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0.5rem; }
.icon-opt { display: flex; flex-direction: column; align-items: center; gap: 0.35rem; border-radius: 0.625rem; border: 1px solid #e5e7eb; padding: 0.65rem 0.5rem; font-size: 0.75rem; font-weight: 500; color: #4b5563; }
.icon-opt:hover { border-color: #9cb0ff; }
.icon-opt.active { border-color: #465fff; background: #eff4ff; color: #465fff; box-shadow: 0 0 0 3px rgb(70 95 255 / 12%); }
.dark .icon-opt { border-color: #344054; color: #d1d5db; }
.dark .icon-opt.active { border-color: #465fff; background: rgb(70 95 255 / 15%); color: #9cb0ff; }
</style>
