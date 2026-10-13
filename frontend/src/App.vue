<template>
  <ThemeProvider>
    <SidebarProvider>
      <RouterView v-slot="{ Component, route }">
        <component :is="Component" :key="route.matched[route.matched.length - 1]?.path" />
      </RouterView>
    </SidebarProvider>
  </ThemeProvider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import ThemeProvider from './components/layout/ThemeProvider.vue'
import SidebarProvider from './components/layout/SidebarProvider.vue'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
onMounted(() => {
  if (auth.isAuthenticated) {
    void auth.hydrateProfile()
  }
})
</script>
