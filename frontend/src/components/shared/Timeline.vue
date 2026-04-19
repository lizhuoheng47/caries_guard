<template>
  <div class="relative w-full py-4 corner-cut-tl corner-cut-br border border-line-subtle p-4 bg-deep-surface/30">
    <div class="flex items-center justify-between relative z-10">
      <template v-for="(node, index) in nodes" :key="node.code">
        <!-- Node -->
        <div class="flex flex-col items-center gap-2 relative z-20">
          <!-- Icon / Circle -->
          <div 
            class="w-5 h-5 rounded-full flex items-center justify-center bg-void-black transition-all"
            :class="{
              'border border-cyan glow-cyan bg-cyan/20 text-cyan': node.status === 'COMPLETED',
              'border border-amber glow-amber': node.status === 'CURRENT',
              'border border-ghost-dim/50': node.status === 'PENDING'
            }"
          >
            <!-- Checkmark for completed -->
            <svg v-if="node.status === 'COMPLETED'" class="w-3 h-3 text-cyan" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
            
            <!-- Pulse dot for current -->
            <div v-else-if="node.status === 'CURRENT'" class="w-2 h-2 rounded-full bg-amber shadow-[0_0_6px_var(--amber)] animate-pulse-opacity"></div>
            
            <!-- Dot for pending -->
            <div v-else class="w-1 h-1 rounded-full bg-ghost-dim/50"></div>
          </div>
          
          <!-- Text -->
          <div class="text-center">
            <div class="text-[10px] whitespace-nowrap mb-0.5" :class="{'text-ice-white': node.status !== 'PENDING', 'text-ghost-dim': node.status === 'PENDING'}">
              {{ node.name }}
            </div>
            <div class="text-[8px] font-mono whitespace-nowrap" :class="{'text-cyan': node.status === 'COMPLETED', 'text-amber val-amber animate-pulse-opacity': node.status === 'CURRENT', 'text-ghost-dim': node.status === 'PENDING'}">
              {{ node.time || (node.status === 'CURRENT' ? 'LIVE' : '—') }}
            </div>
          </div>
        </div>
        
        <!-- Connecting Line -->
        <div v-if="index < nodes.length - 1" class="flex-1 h-[1px] relative -translate-y-4 mx-2">
          <div 
            class="absolute inset-0 transition-all"
            :class="{
              'bg-cyan': node.status === 'COMPLETED' && nodes[index+1].status !== 'PENDING',
              'bg-gradient-to-r from-cyan to-transparent': node.status === 'COMPLETED' && nodes[index+1].status === 'PENDING',
              'bg-gradient-to-r from-amber to-transparent opacity-50': node.status === 'CURRENT',
              'bg-line-subtle': node.status === 'PENDING'
            }"
          ></div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TimelineNode } from '../../models/analysis';

defineProps<{
  nodes: TimelineNode[];
}>();
</script>
