<template>
  <div class="h-screen w-screen page-bg flex flex-col overflow-hidden">
    <!-- Chrome Top Bar -->
    <ChromeBar />

    <!-- Main Content Area -->
    <div class="flex-1 flex overflow-hidden">
      <!-- Sidebar -->
      <AppSidebar />
      
      <!-- Page Content -->
      <div class="flex-1 min-w-0 relative h-full">
        <router-view v-slot="{ Component }">
          <transition name="fade-up" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ChromeBar from './ChromeBar.vue';
import AppSidebar from './AppSidebar.vue';
</script>

<style>
.fade-up-enter-active,
.fade-up-leave-active {
  transition: all 0.2s ease-out;
}
.fade-up-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
