export type AvatarMode = 'icon' | 'upload'

export type AvatarIconId =
  | 'user'
  | 'smile'
  | 'briefcase'
  | 'headphones'
  | 'graduation'
  | 'coffee'
  | 'rocket'

export interface AvatarPreference {
  mode: AvatarMode
  iconId: AvatarIconId
  /** Local preview / offline fallback (data URL) */
  dataUrl?: string | null
  /** Server path from User.photoLink when available */
  photoLink?: string | null
}

export const AVATAR_ICONS: {
  id: AvatarIconId
  label: string
  bg: string
  fg: string
}[] = [
  { id: 'user', label: 'Foydalanuvchi', bg: 'bg-brand-50 dark:bg-brand-500/15', fg: 'text-brand-500' },
  { id: 'smile', label: 'Tabassum', bg: 'bg-success-50 dark:bg-success-500/15', fg: 'text-success-600' },
  { id: 'briefcase', label: 'Ish', bg: 'bg-blue-light-50 dark:bg-blue-light-500/15', fg: 'text-blue-light-500' },
  { id: 'headphones', label: 'Support', bg: 'bg-gray-100 dark:bg-white/10', fg: 'text-gray-600 dark:text-gray-300' },
  { id: 'graduation', label: 'Mutaxassis', bg: 'bg-orange-50 dark:bg-orange-500/15', fg: 'text-orange-500' },
  { id: 'coffee', label: 'Kofe', bg: 'bg-warning-50 dark:bg-warning-500/15', fg: 'text-warning-600' },
  { id: 'rocket', label: 'Raketa', bg: 'bg-error-50 dark:bg-error-500/15', fg: 'text-error-500' },
]

const DEFAULT_PREF: AvatarPreference = {
  mode: 'icon',
  iconId: 'user',
  dataUrl: null,
  photoLink: null,
}

function storageKey(username: string) {
  return `wincrm_avatar_${username}`
}

export function getAvatarPreference(username: string | null | undefined): AvatarPreference {
  if (!username) return { ...DEFAULT_PREF }
  try {
    const raw = localStorage.getItem(storageKey(username))
    if (!raw) return { ...DEFAULT_PREF }
    const parsed = JSON.parse(raw) as Partial<AvatarPreference>
    const iconId = AVATAR_ICONS.some((i) => i.id === parsed.iconId)
      ? (parsed.iconId as AvatarIconId)
      : 'user'
    return {
      mode: parsed.mode === 'upload' ? 'upload' : 'icon',
      iconId,
      dataUrl: parsed.dataUrl || null,
      photoLink: parsed.photoLink || null,
    }
  } catch {
    return { ...DEFAULT_PREF }
  }
}

export function saveAvatarPreference(username: string, pref: AvatarPreference) {
  localStorage.setItem(storageKey(username), JSON.stringify(pref))
  window.dispatchEvent(new CustomEvent('wincrm-avatar-changed', { detail: { username } }))
}

export function resolveAvatarSrc(pref: AvatarPreference): string | null {
  if (pref.mode === 'upload') {
    return pref.dataUrl || pref.photoLink || null
  }
  return null
}

/** Resize & compress image to a small square data URL for localStorage. */
export function fileToAvatarDataUrl(file: File, size = 256, quality = 0.85): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('Fayl o‘qilmadi'))
    reader.onload = () => {
      const img = new Image()
      img.onerror = () => reject(new Error('Rasm yuklanmadi'))
      img.onload = () => {
        const canvas = document.createElement('canvas')
        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas mavjud emas'))
          return
        }
        const min = Math.min(img.width, img.height)
        const sx = (img.width - min) / 2
        const sy = (img.height - min) / 2
        ctx.drawImage(img, sx, sy, min, min, 0, 0, size, size)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.src = String(reader.result)
    }
    reader.readAsDataURL(file)
  })
}
