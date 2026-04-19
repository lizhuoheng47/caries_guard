<template>
  <div class="h-screen w-screen flex flex-col page-bg overflow-hidden text-steel-blue">
    <ChromeBar />
    
    <div class="flex-1 flex overflow-hidden">
      <AppSidebar />
      
      <main class="flex-1 flex flex-col overflow-hidden relative z-10">
        <!-- Route views will handle their own internal header / scroll areas -->
        <router-view v-slot="{ Component }">
          <transition name="fade-up" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
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
