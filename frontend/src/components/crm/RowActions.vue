<template>
  <div class="inline-flex items-center justify-end gap-1.5">
    <button
      v-if="edit"
      type="button"
      class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-warning-500 transition hover:bg-warning-50 dark:text-warning-400 dark:hover:bg-warning-500/10"
      title="Tahrirlash"
      @click="$emit('edit')"
    >
      <PencilAltIcon :size="18" />
    </button>
    <slot />
    <button
      v-if="remove"
      type="button"
      class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-error-500 transition hover:bg-error-50 dark:text-error-400 dark:hover:bg-error-500/10"
      title="O‘chirish"
      @click="$emit('delete')"
    >
      <TrashOutlineIcon :size="18" />
    </button>
  </div>
</template>

<script setup lang="ts">
import PencilAltIcon from '@/icons/PencilAltIcon.vue'
import TrashOutlineIcon from '@/icons/TrashOutlineIcon.vue'

withDefaults(
  defineProps<{
    edit?: boolean
    remove?: boolean
  }>(),
  { edit: true, remove: true },
)

defineEmits<{
  edit: []
  delete: []
}>()
</script>
