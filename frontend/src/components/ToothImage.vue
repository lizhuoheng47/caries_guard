<script setup lang="ts">
defineProps<{ scanning?: boolean }>()
const teeth = [0, 1, 2, 3, 4, 5, 6, 7]
const labels = ['17', '16', '15', '14', '13', '12', '11', '21']
</script>

<template>
  <svg viewBox="0 0 800 520" class="ti-svg" preserveAspectRatio="xMidYMid slice">
    <defs>
      <radialGradient id="tg-bg" cx=".5" cy=".5" r=".8">
        <stop offset="0%" stop-color="#2a3a42" />
        <stop offset="60%" stop-color="#121a1f" />
        <stop offset="100%" stop-color="#070a0c" />
      </radialGradient>
      <linearGradient id="tg-enamel" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#e8eef2" />
        <stop offset="100%" stop-color="#9aacb6" />
      </linearGradient>
      <linearGradient id="tg-dentin" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#cfd8dd" />
        <stop offset="100%" stop-color="#6b7a83" />
      </linearGradient>
      <filter id="tg-soft"><feGaussianBlur stdDeviation=".6" /></filter>
    </defs>
    <rect width="800" height="520" fill="url(#tg-bg)" />
    <g filter="url(#tg-soft)">
      <template v-for="i in teeth" :key="i">
        <g>
          <path
            :d="`M ${80 + i * 82} 180 Q ${80 + i * 82 - 28} 120 ${80 + i * 82 - 6 + Math.sin(i) * 4} 90 Q ${80 + i * 82 + 18} 78 ${80 + i * 82 + 34} 100 Q ${80 + i * 82 + 56} 140 ${80 + i * 82 + 40} 200 Q ${80 + i * 82 + 20} 230 ${80 + i * 82} 230 Q ${80 + i * 82 - 14} 220 ${80 + i * 82 - 6} 200 Z`"
            fill="url(#tg-enamel)"
            stroke="#c2cfd6"
            stroke-width=".5"
            opacity=".92"
          />
          <path
            :d="`M ${80 + i * 82 + 2} 185 Q ${80 + i * 82 - 18} 140 ${80 + i * 82 + 2} 115 Q ${80 + i * 82 + 22} 108 ${80 + i * 82 + 34} 130 Q ${80 + i * 82 + 42} 170 ${80 + i * 82 + 30} 205 Q ${80 + i * 82 + 14} 215 ${80 + i * 82 + 2} 210 Z`"
            fill="url(#tg-dentin)"
            opacity=".6"
          />
          <path
            :d="`M ${80 + i * 82 - 4} 225 Q ${80 + i * 82 - 12} 320 ${80 + i * 82 + 2} 390 Q ${80 + i * 82 + 18} 420 ${80 + i * 82 + 30} 390 Q ${80 + i * 82 + 44} 330 ${80 + i * 82 + 38} 225 Z`"
            fill="url(#tg-dentin)"
            opacity=".55"
          />
          <text
            :x="80 + i * 82 + 16"
            y="460"
            fill="rgba(255,255,255,.32)"
            font-size="11"
            font-family="JetBrains Mono"
            text-anchor="middle"
          >{{ labels[i] }}</text>
        </g>
      </template>
    </g>
    <path d="M 40 230 Q 400 218 760 234 L 760 250 Q 400 238 40 252 Z" fill="rgba(170,100,90,.12)" />
    <rect v-if="scanning" x="0" y="0" width="800" height="520" fill="rgba(18,165,148,.04)" />
  </svg>
</template>
