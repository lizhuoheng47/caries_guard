<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex flex-col">
        <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">AI CORE / <span class="text-[var(--cyan)]">INTELLIGENCE RAG</span></span>
        <h2 class="text-[20px] font-medium text-[var(--tp)] m-0">智能解释</h2>
      </div>
      <div class="flex gap-2">
        <div class="flex border border-[var(--ln)] rounded-[3px] p-0.5 bg-[rgba(3,8,18,0.5)]">
          <button class="px-3 py-1 font-mono text-[9px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">PATIENT MODE</button>
          <button class="px-3 py-1 font-mono text-[9px] bg-[var(--cyan)] text-[var(--void)] rounded-[2px] shadow-[0_0_8px_var(--cyan)] glow-cyan">DOCTOR MODE</button>
        </div>
      </div>
    </div>
    
    <div class="flex-1 flex gap-3 min-h-0 mb-4 overflow-hidden">
      
      <!-- Left: Case Context -->
      <div class="w-[240px] shrink-0 min-h-0 flex flex-col gap-3">
        <Panel title="Patient Context" color="cyan">
          <div class="flex flex-col gap-3">
            <div class="w-full aspect-[16/9] bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] overflow-hidden relative">
              <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
              <div class="absolute bottom-2 left-2 flex items-center gap-1.5">
                <div class="w-1.5 h-1.5 rounded-full bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div>
                <span class="font-mono text-[8px] text-[var(--cyan-soft)]">SCAN 2026-04-18</span>
              </div>
            </div>
            
            <div class="flex flex-col gap-2">
              <div class="flex justify-between items-baseline border-b border-[var(--ln)] pb-1">
                <span class="font-mono text-[9px] text-[var(--td)] uppercase">PATIENT ID</span>
                <span class="font-mono text-[11px] text-[var(--tp)]">P-1002</span>
              </div>
              <div class="flex justify-between items-baseline border-b border-[var(--ln)] pb-1">
                <span class="font-mono text-[9px] text-[var(--td)] uppercase">DEMOGRAPHICS</span>
                <span class="font-mono text-[11px] text-[var(--tp)]">M / 45 YRS</span>
              </div>
              <div class="flex justify-between items-baseline pb-1">
                <span class="font-mono text-[9px] text-[var(--td)] uppercase">DATE</span>
                <span class="font-mono text-[11px] text-[var(--tp)]">2026-04-18</span>
              </div>
            </div>
            
            <div class="mt-2 p-3 bg-[var(--amber)]/5 border-[0.5px] border-[var(--amber)]/30 rounded-[3px]">
              <div class="flex items-center gap-2 mb-2">
                <span class="font-mono text-[9px] text-[var(--amber)] uppercase">AI DIAGNOSIS</span>
              </div>
              <div class="flex items-center gap-2 mb-2">
                <span class="font-mono text-[16px] text-[var(--amber)] val-amber">G3</span>
                <span class="text-[10px] text-[var(--ts)]">Deep Caries Detected</span>
              </div>
              <span class="font-mono text-[9px] text-[var(--td)]">UNCERTAINTY: 0.72</span>
            </div>
          </div>
        </Panel>
      </div>
      
      <!-- Middle: Chat Area -->
      <div class="flex-1 min-w-0 flex flex-col">
        <Panel title="Diagnostic Conversation" color="cyan">
          <div class="flex flex-col h-full">
            <div class="flex-1 overflow-y-auto pr-2 flex flex-col gap-4 pb-4">
              <template v-for="(msg, index) in messages" :key="index">
                <!-- AI Msg -->
                <div v-if="msg.role === 'ai'" class="flex gap-3 max-w-[85%]">
                  <div class="w-[28px] h-[28px] rounded-[4px] bg-[var(--violet)]/20 border border-[var(--violet)] flex items-center justify-center shrink-0">
                    <div class="w-3 h-3 bg-gradient-to-br from-[var(--cyan)] to-[var(--violet)] shadow-[0_0_8px_var(--violet)] rounded-xs rotate-45"></div>
                  </div>
                  <div class="flex flex-col gap-2">
                    <div class="bg-[var(--violet)]/5 border-l-2 border-l-[var(--violet)] border border-[var(--violet)]/20 rounded-[4px] p-3 text-[12px] text-[var(--tp)] leading-relaxed" v-html="msg.content">
                    </div>
                    <!-- Safety Flag -->
                    <div v-if="msg.warning" class="bg-[var(--amber)]/10 border border-[var(--amber)]/30 rounded-[4px] p-2 flex items-center gap-2">
                      <svg class="w-4 h-4 text-[var(--amber)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                      <span class="text-[10px] text-[var(--amber)] font-mono">{{ msg.warning }}</span>
                    </div>
                  </div>
                </div>
                
                <!-- User Msg -->
                <div v-else class="flex gap-3 max-w-[85%] self-end flex-row-reverse">
                  <div class="w-[28px] h-[28px] rounded-[4px] bg-[rgba(3,8,18,0.8)] border border-[var(--ln)] flex items-center justify-center shrink-0">
                    <svg class="w-4 h-4 text-[var(--ts)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                  </div>
                  <div class="bg-[var(--cyan)]/10 border border-[var(--cyan)]/30 rounded-[4px] p-3 text-[12px] text-[var(--tp)] leading-relaxed">
                    {{ msg.content }}
                  </div>
                </div>
              </template>
              <div v-if="loading" class="flex gap-3 max-w-[85%]">
                <div class="w-[28px] h-[28px] rounded-[4px] bg-[var(--violet)]/20 border border-[var(--violet)] flex items-center justify-center shrink-0">
                  <div class="w-3 h-3 bg-[var(--cyan)] rounded-xs rotate-45 animate-ping"></div>
                </div>
                <div class="bg-[var(--violet)]/5 border border-[var(--violet)]/20 rounded-[4px] p-3 text-[12px] text-[var(--td)]">
                  Thinking...
                </div>
              </div>
              
            </div>
            
            <!-- Input Area -->
            <div class="shrink-0 pt-3 border-t border-[var(--ln)]">
              <div class="relative">
                <textarea 
                  v-model="inputText"
                  @keydown.enter.prevent="sendMessage"
                  class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[4px] py-3 pl-3 pr-12 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-colors resize-none h-[48px]"
                  placeholder="Ask for deeper diagnostic details..."
                ></textarea>
                <button @click="sendMessage" class="absolute right-2 top-1/2 -translate-y-1/2 w-[32px] h-[32px] flex items-center justify-center bg-[var(--cyan)]/20 border border-[var(--cyan)] text-[var(--cyan)] rounded-[3px] hover:shadow-[0_0_8px_rgba(0,229,255,0.4)] transition-all">
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                </button>
              </div>
            </div>
          </div>
        </Panel>
      </div>
      
      <!-- Right: Citation Tracing -->
      <div class="w-[260px] shrink-0 min-h-0 flex flex-col gap-3">
        <Panel title="Citation Trace" color="violet">
          <div class="flex flex-col h-full">
            <span class="font-mono text-[9px] text-[var(--td)] tracking-widest uppercase mb-3">Retrieved Chunks</span>
            
            <div class="flex-1 overflow-y-auto flex flex-col gap-3 pr-2">
              <div v-for="(citation, index) in citations" :key="index" class="p-3 bg-[rgba(3,8,18,0.5)] border border-[var(--ln)] hover:border-[var(--violet)]/50 rounded-[4px] cursor-pointer transition-colors relative">
                <div class="absolute -left-[1px] top-2 w-[2px] h-[16px] bg-[var(--violet)] shadow-[0_0_8px_var(--violet)]"></div>
                <div class="flex justify-between items-start mb-1.5 pl-1.5">
                  <div class="flex items-center gap-1.5">
                    <div class="w-4 h-4 bg-[var(--violet)]/20 border border-[var(--violet)]/40 rounded-xs flex items-center justify-center font-mono text-[8px] text-[var(--violet)]">{{ index + 1 }}</div>
                    <span class="font-mono text-[9px] text-[var(--tp)] truncate w-[140px]">{{ citation.docTitle }}</span>
                  </div>
                  <span class="font-mono text-[8px] text-[var(--td)]">P.{{ citation.pageNumber || '?' }}</span>
                </div>
                <p class="text-[10px] text-[var(--ts)] leading-relaxed line-clamp-3 pl-1.5">
                  {{ citation.chunkText }}
                </p>
              </div>
            </div>
            
            <div class="shrink-0 pt-3 border-t border-[var(--ln)] mt-2">
              <div class="flex justify-between items-baseline mb-1">
                <span class="font-mono text-[8px] text-[var(--td)]">Knowledge Base DB</span>
                <span class="font-mono text-[8px] text-[var(--cyan)]">v1.4.2</span>
              </div>
              <div class="flex justify-between items-baseline">
                <span class="font-mono text-[8px] text-[var(--td)]">Indexed Chunks</span>
                <span class="font-mono text-[8px] text-[var(--tp)]">48,291</span>
              </div>
            </div>
          </div>
        </Panel>
      </div>
      
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import Panel from '../components/shared/Panel.vue';
import CitationTag from '../components/shared/CitationTag.vue';
import { ragApi } from '../api/rag';

interface Message {
  role: 'ai' | 'user';
  content: string;
  warning?: string;
}

const inputText = ref('');
const loading = ref(false);
const messages = ref<Message[]>([
  {
    role: 'ai',
    content: '系统已完成全景X光片的初步推理。在右下颌第一磨牙远中邻面检测到 <strong>G3 级</strong> 龋坏。该病变表现为典型的透射影像，深度已突破釉质牙本质界（DEJ），位于牙本质中层。<br/><br/>请问有什么可以帮助您？'
  }
]);
const citations = ref<any[]>([]);

const sendMessage = async () => {
  const text = inputText.value.trim();
  if (!text || loading.value) return;

  messages.value.push({ role: 'user', content: text });
  inputText.value = '';
  loading.value = true;

  try {
    const res = await ragApi.ask(text);
    messages.value.push({
      role: 'ai',
      content: res.data.answerText || res.data.answer,
      warning: res.data.safetyFlag === '1' ? 'WARNING: AI 建议仅供参考' : undefined
    });
    citations.value = res.data.citations || [];
  } catch (e) {
    messages.value.push({
      role: 'ai',
      content: 'I encountered an error retrieving the information.'
    });
  } finally {
    loading.value = false;
  }
};
</script>
