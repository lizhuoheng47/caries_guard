<template>
  <div class="page">
    <div class="page-hello" style="margin-bottom: 18px">
      <div>
        <div class="micro">Doctor QA</div>
        <h1 class="page-hello-title">智能解释与医生问答</h1>
      </div>
      <div class="lib-filter-group">
        <button class="lib-filter">患者模式</button>
        <button class="lib-filter on">医生模式</button>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: 280px 1fr 320px; gap: 16px; min-height: 680px">
      <section class="card" style="padding: 18px">
        <div class="micro" style="margin-bottom: 10px">Case Context</div>
        <div style="aspect-ratio: 16 / 10; border-radius: 12px; background: linear-gradient(180deg, var(--surface-sunk), var(--surface)); border: 1px solid var(--line); margin-bottom: 14px"></div>
        <div style="display: grid; gap: 10px; font-size: 13px">
          <div style="display: flex; justify-content: space-between"><span class="micro">患者</span><span class="mono">P-1002</span></div>
          <div style="display: flex; justify-content: space-between"><span class="micro">基本信息</span><span>男 / 45 岁</span></div>
          <div style="display: flex; justify-content: space-between"><span class="micro">检查日期</span><span class="mono">2026-04-18</span></div>
        </div>
        <div class="card" style="margin-top: 16px; padding: 14px; background: var(--warn-100); border-color: #f4d7ad">
          <div class="micro" style="margin-bottom: 6px; color: var(--warn-700)">AI 结论</div>
          <div style="display: flex; align-items: baseline; gap: 10px">
            <span class="chip" style="background: #fff; color: var(--warn-700)">G3</span>
            <span style="font-size: 13px; color: var(--ink-2)">检测到深龋，建议结合临床检查进一步确认。</span>
          </div>
        </div>
      </section>

      <section class="card" style="display: flex; flex-direction: column; overflow: hidden">
        <div class="card-head">
          <h3>问答对话</h3>
          <div class="card-head-actions">
            <span class="micro">Java BFF / RAG</span>
          </div>
        </div>

        <div style="flex: 1; overflow-y: auto; padding: 18px; display: flex; flex-direction: column; gap: 14px; background: var(--surface-2)">
          <template v-for="(msg, index) in messages" :key="index">
            <div
              :style="{
                display: 'flex',
                gap: '10px',
                maxWidth: '85%',
                alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row'
              }"
            >
              <div
                :style="{
                  width: '32px',
                  height: '32px',
                  borderRadius: '10px',
                  display: 'grid',
                  placeItems: 'center',
                  background: msg.role === 'user' ? 'var(--brand-100)' : 'var(--surface)',
                  border: '1px solid var(--line)',
                  flexShrink: 0
                }"
              >
                <AppIcon :name="msg.role === 'user' ? 'user' : 'sparkle'" :size="14" />
              </div>
              <div style="display: flex; flex-direction: column; gap: 8px">
                <div
                  :style="{
                    padding: '12px 14px',
                    borderRadius: '14px',
                    background: msg.role === 'user' ? 'var(--brand-700)' : 'var(--surface)',
                    color: msg.role === 'user' ? '#fff' : 'var(--ink-1)',
                    border: msg.role === 'user' ? '1px solid var(--brand-700)' : '1px solid var(--line)',
                    lineHeight: '1.65',
                    fontSize: '13px'
                  }"
                  v-html="msg.content"
                ></div>
                <div
                  v-if="msg.warning"
                  class="card"
                  style="padding: 10px 12px; background: var(--warn-100); border-color: #f4d7ad; font-size: 12px; color: var(--warn-700)"
                >
                  {{ msg.warning }}
                </div>
              </div>
            </div>
          </template>

          <div v-if="loading" style="display: flex; gap: 10px; max-width: 85%">
            <div style="width: 32px; height: 32px; border-radius: 10px; display: grid; place-items: center; background: var(--surface); border: 1px solid var(--line)">
              <AppIcon name="sparkle" :size="14" />
            </div>
            <div class="card" style="padding: 12px 14px; background: #fff">正在生成回答...</div>
          </div>
        </div>

        <div style="padding: 16px; border-top: 1px solid var(--line); background: #fff">
          <div style="display: flex; gap: 10px">
            <textarea
              v-model.trim="inputText"
              class="card"
              style="flex: 1; min-height: 76px; padding: 12px 14px; resize: none; outline: none; border-radius: 14px; background: var(--surface-2)"
              placeholder="询问更具体的诊断解释、分级原因或临床建议"
              @keydown.enter.exact.prevent="sendMessage"
            ></textarea>
            <button class="btn btn-primary" style="align-self: flex-end; height: 44px" @click="sendMessage">
              发送
              <AppIcon name="arrow_right" :size="14" />
            </button>
          </div>
        </div>
      </section>

      <section class="card" style="display: flex; flex-direction: column; overflow: hidden">
        <div class="card-head">
          <h3>引用来源</h3>
          <div class="card-head-actions">
            <span class="micro">{{ citations.length }} Items</span>
          </div>
        </div>
        <div style="padding: 16px; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; flex: 1">
          <div v-for="(citation, index) in citations" :key="index" class="card" style="padding: 14px">
            <div style="display: flex; justify-content: space-between; gap: 10px; align-items: start">
              <div style="display: flex; gap: 8px; align-items: center; min-width: 0">
                <span class="chip chip-neutral" style="padding: 0 8px">#{{ index + 1 }}</span>
                <span style="font-size: 13px; font-weight: 600; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                  {{ citation.docTitle || 'Knowledge citation' }}
                </span>
              </div>
              <span class="mono" style="font-size: 10px; color: var(--ink-3)">P{{ citation.pageNumber || '?' }}</span>
            </div>
            <p style="font-size: 12px; color: var(--ink-2); line-height: 1.7; margin: 10px 0 0">
              {{ citation.chunkText || '暂无摘要片段。' }}
            </p>
          </div>

          <div v-if="citations.length === 0" class="card" style="padding: 16px; color: var(--ink-3)">
            当前回答没有返回引用片段。
          </div>
        </div>
        <div style="padding: 16px; border-top: 1px solid var(--line); background: var(--surface-2); display: grid; gap: 8px">
          <div style="display: flex; justify-content: space-between"><span class="micro">知识库版本</span><span class="mono">{{ latestKbVersion || 'v1.0' }}</span></div>
          <div style="display: flex; justify-content: space-between"><span class="micro">置信度</span><span class="mono">{{ latestConfidence !== null ? `${(latestConfidence * 100).toFixed(0)}%` : '--' }}</span></div>
          <div style="display: flex; justify-content: space-between; gap: 10px"><span class="micro">Trace ID</span><span class="mono" style="font-size: 10px; color: var(--ink-3)">{{ latestTraceId || '--' }}</span></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { ragApi } from '@/api/rag'

interface Message {
  role: 'ai' | 'user'
  content: string
  warning?: string
}

const inputText = ref('')
const loading = ref(false)
const messages = ref<Message[]>([
  {
    role: 'ai',
    content:
      '系统已完成当前影像的初步推理。在右下磨牙邻面区域检测到 <strong>G3</strong> 级龋坏，建议结合探诊、冷热测验和既往病史进一步确认。',
  }
])
const citations = ref<any[]>([])
const latestKbVersion = ref('')
const latestConfidence = ref<number | null>(null)
const latestTraceId = ref('')

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true

  try {
    const res = await ragApi.ask(text)
    messages.value.push({
      role: 'ai',
      content: res.data.answerText || res.data.answer || '当前没有可返回的答案。',
      warning: res.data.safetyFlag === '1' ? '警告：当前回答仅供辅助参考，不能替代临床最终判断。' : undefined
    })
    citations.value = res.data.citations || []
    latestKbVersion.value = res.data.knowledgeVersion || ''
    latestConfidence.value = typeof res.data.confidence === 'number' ? res.data.confidence : null
    latestTraceId.value = res.data.traceId || ''
  } catch (error) {
    console.error('Failed to send rag message', error)
    messages.value.push({
      role: 'ai',
      content: '获取问答结果失败，请稍后重试。'
    })
  } finally {
    loading.value = false
  }
}
</script>
