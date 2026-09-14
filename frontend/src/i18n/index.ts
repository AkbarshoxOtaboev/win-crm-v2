import { createI18n } from 'vue-i18n'
import uz from './locales/uz'
import ru from './locales/ru'
import en from './locales/en'

export type AppLocale = 'uz' | 'ru' | 'en'

const STORAGE_KEY = 'wincrm_locale'

export function getStoredLocale(): AppLocale {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw === 'uz' || raw === 'ru' || raw === 'en') return raw
  return 'uz'
}

export function storeLocale(locale: AppLocale) {
  localStorage.setItem(STORAGE_KEY, locale)
  document.documentElement.lang = locale
}

const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'uz',
  messages: { uz, ru, en },
})

storeLocale(getStoredLocale())

export default i18n
