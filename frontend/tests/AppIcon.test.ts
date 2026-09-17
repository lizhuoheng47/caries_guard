import { createSSRApp, h } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { describe, expect, it } from 'vitest'

import AppIcon from '../src/components/AppIcon.vue'

describe('AppIcon', () => {
  it('renders a known icon at the requested size', async () => {
    const html = await renderToString(
      createSSRApp({ render: () => h(AppIcon, { name: 'home', size: 24 }) }),
    )

    expect(html).toContain('width="24"')
    expect(html).toContain('height="24"')
    expect(html).toContain('d="M3 10.5')
  })

  it('keeps unknown icon names safe and decorative', async () => {
    const html = await renderToString(
      createSSRApp({ render: () => h(AppIcon, { name: 'not-a-real-icon' }) }),
    )

    expect(html).toContain('aria-hidden="true"')
    expect(html).toContain('d=""')
  })
})
