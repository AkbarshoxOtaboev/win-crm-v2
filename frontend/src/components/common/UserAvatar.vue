<template>
  <span
    class="inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full"
    :class="[sizeClass, !src ? iconMeta.bg : 'bg-gray-100 dark:bg-gray-800']"
    :title="title"
  >
    <img v-if="src" :src="src" alt="" class="h-full w-full object-cover" />
    <component
      :is="iconComponent"
      v-else
      class="h-[45%] w-[45%]"
      :class="iconMeta.fg"
      :stroke-width="2"
    />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Briefcase,
  Coffee,
  GraduationCap,
  Headphones,
  Rocket,
  Smile,
  User,
} from 'lucide-vue-next'
import {
  AVATAR_ICONS,
  resolveAvatarSrc,
  type AvatarIconId,
  type AvatarPreference,
} from '@/utils/avatar'

const props = withDefaults(
  defineProps<{
    preference: AvatarPreference
    size?: 'sm' | 'md' | 'lg' | 'xl'
    title?: string
  }>(),
  { size: 'md', title: '' },
)

const iconMap = {
  user: User,
  smile: Smile,
  briefcase: Briefcase,
  headphones: Headphones,
  graduation: GraduationCap,
  coffee: Coffee,
  rocket: Rocket,
} as const

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'h-9 w-9'
    case 'lg':
      return 'h-16 w-16'
    case 'xl':
      return 'h-28 w-28'
    default:
      return 'h-11 w-11'
  }
})

const src = computed(() => resolveAvatarSrc(props.preference))

const iconMeta = computed(() => {
  const id = props.preference.iconId as AvatarIconId
  return AVATAR_ICONS.find((i) => i.id === id) || AVATAR_ICONS[0]
})

const iconComponent = computed(() => iconMap[props.preference.iconId] || User)
</script>
