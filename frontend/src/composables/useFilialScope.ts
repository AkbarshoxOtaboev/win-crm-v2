import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function useFilialScope() {
  const auth = useAuthStore()
  const { t } = useI18n()
  const route = useRoute()

  const isSettingsRoute = computed(() => route.path.startsWith('/settings'))

  /** Joriy sahifadan qat'i nazar: filialga bog'liq ma'lumot qo'shish uchun filial tanlanmagan. */
  const filialMissing = computed(() =>
    auth.superAdmin ? auth.selectedFilialId == null : auth.assignedFilialId == null,
  )

  const writeBlocked = computed(() => {
    // Sozlamalar filial tanloviga bog‘lanmaydi
    if (isSettingsRoute.value) return false
    // Xodimlar: director o‘z filialiga bog‘langan
    if (route.path.startsWith('/employees')) {
      return auth.assignedFilialId == null
    }
    return filialMissing.value
  })

  const writeBlockedMessage = computed(() => String(t('errors.filialRequired')))

  return { auth, writeBlocked, writeBlockedMessage, filialMissing, isSettingsRoute }
}
