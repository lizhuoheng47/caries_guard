<script setup lang="ts">
interface Props {
  name: string
  size?: number
}
const props = withDefaults(defineProps<Props>(), { size: 18 })

// Inline paths — matches React Icons mapping (subset used by Vue app)
const paths: Record<string, string> = {
  logo: 'M7 3.5c-2 0-3.5 1.7-3.5 3.8 0 1.6.6 2.7 1 4 .5 1.5.5 3 .5 4.5 0 3 1 6.2 2.8 6.2 1.4 0 1.5-2 2.2-4 .5-1.4 1-2 2-2s1.5.6 2 2c.7 2 .8 4 2.2 4 1.8 0 2.8-3.2 2.8-6.2 0-1.5 0-3 .5-4.5.4-1.3 1-2.4 1-4 0-2.1-1.5-3.8-3.5-3.8-1.4 0-2.5.7-3.4 1.3-.7.4-1.1.6-1.6.6s-.9-.2-1.6-.6c-.9-.6-2-1.3-3.4-1.3Z',
  home: 'M3 10.5 12 3l9 7.5M5 9.5V20h14V9.5M10 20v-6h4v6',
  scan: 'M4 8V6a2 2 0 0 1 2-2h2M20 8V6a2 2 0 0 0-2-2h-2M4 16v2a2 2 0 0 0 2 2h2M20 16v2a2 2 0 0 1-2 2h-2M4 12h16',
  report: 'M7 3h7l5 5v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2ZM14 3v5h5M9 13h6M9 17h4',
  library: 'M3 5h4v14H3zM9 5h4v14H9zM15 6l4 14',
  settings: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3h0a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9v0a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z',
  upload: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12',
  search: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14ZM20 20l-3.5-3.5',
  plus: 'M12 5v14M5 12h14',
  check: 'm5 12 5 5 9-11',
  x: 'M6 6l12 12M18 6 6 18',
  chevron_right: 'm9 6 6 6-6 6',
  chevron_down: 'm6 9 6 6 6-6',
  bell: 'M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a2 2 0 0 0 3.4 0',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z M4 21a8 8 0 0 1 16 0',
  globe: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z M3 12h18 M12 3a14 14 0 0 1 0 18 M12 3a14 14 0 0 0 0 18',
  download: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3',
  share: 'M18 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM6 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM18 22a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm-9.4-11.5 6.8-4M8.6 13.5l6.8 4',
  arrow_right: 'M5 12h14M13 5l7 7-7 7',
  sparkle: 'M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8',
  menu: 'M4 7h16M4 12h16M4 17h16',
  filter: 'M3 5h18l-7 9v6l-4-2v-4L3 5Z',
  alert: 'M12 9v4M12 17h.01M10.3 3.9 2.5 17.6A2 2 0 0 0 4.2 20.5h15.6a2 2 0 0 0 1.7-2.9L13.7 3.9a2 2 0 0 0-3.4 0Z',
  compare: 'M12 3v18M7 7 3 11l4 4M17 7l4 4-4 4',
  zoom_in: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14ZM11 8v6M8 11h6M20 20l-3.5-3.5',
  zoom_out: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14ZM8 11h6M20 20l-3.5-3.5',
  contrast: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z M12 3v18',
  pen: 'M12 20h9M16.5 3.5a2.1 2.1 0 1 1 3 3L7 19l-4 1 1-4Z',
  rect: 'M4 5h16v14H4z',
  move: 'M5 9 2 12l3 3M9 5l3-3 3 3M15 19l-3 3-3-3M19 9l3 3-3 3M2 12h20M12 2v20',
  clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM12 7v5l3 2'
}
</script>

<template>
  <svg :width="props.size" :height="props.size" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      :d="paths[name] || ''"
      stroke="currentColor"
      stroke-width="1.6"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
  </svg>
</template>
