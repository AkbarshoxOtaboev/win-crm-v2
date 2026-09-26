<template>
  <img v-if="objectUrl" :src="objectUrl" :alt="alt" v-bind="$attrs" />
  <div v-else class="auth-image-placeholder" v-bind="$attrs">
    <span v-if="failed">!</span>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { getAccessToken } from '@/api/http'

defineOptions({ inheritAttrs: false })

const props = defineProps<{ src?: string | null; alt?: string }>()
const emit = defineEmits<{ loaded: [url: string] }>()

const objectUrl = ref('')
const failed = ref(false)
let requestId = 0

function revoke() {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
}

async function load(src?: string | null) {
  const current = ++requestId
  revoke()
  failed.value = false
  if (!src) return
  try {
    const token = getAccessToken()
    const res = await fetch(src, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
    if (!res.ok) throw new Error(String(res.status))
    const blob = await res.blob()
    if (current !== requestId) return
    objectUrl.value = URL.createObjectURL(blob)
    emit('loaded', objectUrl.value)
  } catch {
    if (current === requestId) failed.value = true
  }
}

watch(() => props.src, load, { immediate: true })
onBeforeUnmount(() => {
  requestId++
  revoke()
})
</script>

<style scoped>
.auth-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #dc2626;
  font-weight: 600;
}
.dark .auth-image-placeholder { background: #1f2937; }
</style>
