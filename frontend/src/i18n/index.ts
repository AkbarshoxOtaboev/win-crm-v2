import { createI18n } from 'vue-i18n'
import uz from './locales/uz'
import ru from './locales/ru'
import en from './locales/en'

export type AppLocale = 'uz' | 'ru' | 'en'

type Messages = { [key: string]: string | string[] | Messages }
type PageMessages = Record<AppLocale, Messages>

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

/**
 * Each file in ./pages exports `{ uz, ru, en }` and is mounted under a namespace equal to its file name,
 * e.g. ./pages/suppliers.ts -> t('suppliers.title').
 */
const pageModules = import.meta.glob<{ default: PageMessages }>('./pages/*.ts', { eager: true })

function buildMessages(): Record<AppLocale, Messages> {
  const messages = { uz: { ...uz }, ru: { ...ru }, en: { ...en } } as Record<AppLocale, Messages>
  for (const [path, mod] of Object.entries(pageModules)) {
    const ns = path.replace(/^.*\//, '').replace(/\.ts$/, '')
    for (const locale of ['uz', 'ru', 'en'] as AppLocale[]) {
      messages[locale][ns] = mod.default[locale] ?? mod.default.uz
    }
  }
  return messages
}

const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'uz',
  messages: buildMessages(),
})

storeLocale(getStoredLocale())

export default i18n
