<template>
  <router-link to="/" class="xl:hidden">
    <img class="dark:hidden" src="/images/logo/logo.svg" alt="WinCRM" />
    <img class="hidden dark:block" src="/images/logo/logo-dark.svg" alt="WinCRM" />
  </router-link>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
