import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function useFilialScope() {
  const auth = useAuthStore()
  const { t } = useI18n()
  const route = useRoute()

  const isSettingsRoute = computed(() => route.path.startsWith('/settings'))

  const writeBlocked = computed(() => {
    // Sozlamalar filial tanloviga bog‘lanmaydi
    if (isSettingsRoute.value) return false
    // Xodimlar: director o‘z filialiga bog‘langan
    if (route.path.startsWith('/employees')) {
      return auth.assignedFilialId == null
    }
    if (auth.superAdmin) return auth.selectedFilialId == null
    return auth.assignedFilialId == null
  })

  const writeBlockedMessage = computed(() => String(t('errors.filialRequired')))

  return { auth, writeBlocked, writeBlockedMessage, isSettingsRoute }
}
