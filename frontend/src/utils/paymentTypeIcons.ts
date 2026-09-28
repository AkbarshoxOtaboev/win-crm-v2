import type { Component } from 'vue'
import { ArrowLeftRight, Banknote, CreditCard, Landmark, Smartphone, Wallet } from 'lucide-vue-next'

export type PaymentTypeIconKey = 'CASH' | 'CARD' | 'ONLINE' | 'BANK' | 'TRANSFER' | 'WALLET'

export const PAYMENT_TYPE_ICONS: { key: PaymentTypeIconKey; icon: Component }[] = [
  { key: 'CASH', icon: Banknote },
  { key: 'CARD', icon: CreditCard },
  { key: 'ONLINE', icon: Smartphone },
  { key: 'BANK', icon: Landmark },
  { key: 'TRANSFER', icon: ArrowLeftRight },
  { key: 'WALLET', icon: Wallet },
]

export const DEFAULT_PAYMENT_TYPE_ICON: PaymentTypeIconKey = 'CARD'

export function paymentTypeIcon(key?: string | null): Component {
  return PAYMENT_TYPE_ICONS.find((i) => i.key === key)?.icon ?? CreditCard
}
