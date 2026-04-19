<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex items-center gap-2">
        <h2 class="text-[18px] font-medium text-ice-white m-0">RAG Knowledge Console</h2>
        <div class="hud-chip bg-knowledge-violet/10 border-knowledge-violet/30 text-knowledge-violet px-1.5 py-0.5 ml-2 border-[0.5px] rounded-xs font-mono text-[8px] flex items-center gap-1">
          <div class="w-1 h-1 bg-knowledge-violet rounded-full animate-pulse-opacity"></div>
          ENGINE ACTIVE
        </div>
      </div>
    </div>
    
    <!-- Three Columns Layout -->
    <div class="flex-1 flex gap-4 min-h-0 mb-4 overflow-hidden">
      
      <!-- Left: Context (240px) -->
      <div class="w-[240px] shrink-0 flex flex-col gap-4">
        <Panel title="Patient Context" color="cyan">
          <div class="flex flex-col gap-3">
            <div class="flex justify-between items-baseline border-b border-line-subtle/50 pb-1">
              <span class="font-mono text-[9px] text-ghost-dim uppercase">ID</span>
              <span class="font-mono text-[11px] text-ice-white">P-1002345</span>
            </div>
            <div class="flex justify-between items-baseline border-b border-line-subtle/50 pb-1">
              <span class="font-mono text-[9px] text-ghost-dim uppercase">Demographics</span>
              <span class="font-mono text-[11px] text-ice-white">Male, 34 yrs</span>
            </div>
            <div class="flex justify-between items-baseline border-b border-line-subtle/50 pb-1">
              <span class="font-mono text-[9px] text-ghost-dim uppercase">Visit Date</span>
              <span class="font-mono text-[11px] text-ice-white">2026-04-19</span>
            </div>
          </div>
        </Panel>
        
        <Panel title="Case Summary" color="amber" class="flex-1">
          <div class="text-[10px] text-steel-blue leading-relaxed mb-4">
            Patient complains of sensitivity to cold in the lower right quadrant.
          </div>
          <div class="bg-void-black border border-line-subtle p-2 rounded relative">
            <div class="absolute inset-0 bg-gradient-to-br from-alert-amber/5 to-transparent"></div>
            <div class="flex justify-between items-center mb-1">
              <span class="font-mono text-[8px] text-ghost-dim uppercase">AI Grading</span>
              <span class="font-mono text-[12px] text-alert-amber val-amber">G3</span>
            </div>
            <div class="text-[9px] font-mono text-ice-white">
              Lesion approaching inner half of dentin (46 MO).
            </div>
          </div>
          <!-- Mock Thumbnail -->
          <div class="mt-4 w-full h-[80px] bg-[#0A1428] border border-line-subtle rounded flex items-center justify-center relative overflow-hidden">
             <div class="absolute inset-0 z-0 opacity-[0.05]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 8px 8px;"></div>
             <span class="font-mono text-[8px] text-ghost-dim">X-Ray Thumb</span>
          </div>
        </Panel>
      </div>
      
      <!-- Middle: Chat Area -->
      <div class="flex-1 min-w-0 flex flex-col glass-panel rounded-md border-knowledge-violet/20 overflow-hidden relative">
        <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-knowledge-violet via-neural-cyan to-transparent opacity-50"></div>
        
        <!-- Mode Switcher -->
        <div class="flex justify-center p-2 border-b border-line-subtle bg-deep-surface">
          <div class="flex gap-1 bg-void-black p-0.5 rounded border border-line-subtle">
            <button class="px-3 py-1 text-[10px] font-mono bg-knowledge-violet/20 text-knowledge-violet rounded-xs">Doctor Q&A</button>
            <button class="px-3 py-1 text-[10px] font-mono text-ghost-dim hover:text-ice-white rounded-xs">Patient Explanation</button>
          </div>
        </div>
        
        <!-- Messages -->
        <div class="flex-1 overflow-y-auto p-4 space-y-6 flex flex-col" ref="chatContainer">
          <div v-if="store.history.length === 0" class="flex-1 flex items-center justify-center text-ghost-dim flex-col gap-3">
            <svg class="w-10 h-10 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" /></svg>
            <span class="font-mono text-[10px] tracking-widest">AWAITING QUERY</span>
          </div>
          
          <template v-for="(msg, i) in store.history" :key="i">
            <!-- User Message -->
            <div v-if="msg.role === 'user'" class="flex justify-end">
              <div class="max-w-[80%] bg-neural-cyan/10 border-[0.5px] border-neural-cyan/30 rounded-lg rounded-tr-none p-3 text-[12px] text-ice-white leading-relaxed">
                {{ msg.content }}
              </div>
            </div>
            
            <!-- AI Message -->
            <div v-else class="flex justify-start">
              <div class="max-w-[85%] bg-knowledge-violet/5 border-l-2 border-knowledge-violet rounded-lg rounded-tl-none p-4 text-[12px] text-ice-white leading-relaxed relative">
                <!-- Warning flag if exists -->
                <div v-if="msg.response?.safetyWarning" class="flex items-start gap-2 bg-alert-amber/10 border border-alert-amber/30 p-2 rounded-sm mb-3">
                  <svg class="w-3.5 h-3.5 text-alert-amber shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                  <div class="flex flex-col gap-1">
                    <span class="text-[10px] text-alert-amber" v-for="w in msg.response.safetyMessages" :key="w">{{ w }}</span>
                  </div>
                </div>
                
                <!-- Content rendering with citations (simplified for MVP) -->
                <div>
                  {{ msg.content }}
                </div>
                
                <div class="flex justify-end gap-3 mt-2 pt-2 border-t border-line-subtle/50">
                  <span class="font-mono text-[8px] text-ghost-dim">LATENCY: 1.45s</span>
                  <span class="font-mono text-[8px] text-ghost-dim">CONF: {{ (msg.response?.confidence || 0).toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </template>
          
          <div v-if="store.loading" class="flex justify-start">
            <div class="bg-knowledge-violet/5 border-l-2 border-knowledge-violet p-3 text-knowledge-violet flex items-center gap-2">
              <div class="flex gap-1">
                <div class="w-1.5 h-1.5 bg-knowledge-violet rounded-full animate-bounce" style="animation-delay: 0ms"></div>
                <div class="w-1.5 h-1.5 bg-knowledge-violet rounded-full animate-bounce" style="animation-delay: 150ms"></div>
                <div class="w-1.5 h-1.5 bg-knowledge-violet rounded-full animate-bounce" style="animation-delay: 300ms"></div>
              </div>
              <span class="font-mono text-[9px] tracking-widest ml-2">RETRIEVING KNOWLEDGE...</span>
            </div>
          </div>
        </div>
        
        <!-- Input Area -->
        <div class="p-4 border-t border-line-subtle bg-deep-surface shrink-0">
          <form @submit.prevent="submitQuestion" class="flex gap-2">
            <input 
              v-model="question"
              type="text" 
              placeholder="Ask about guidelines, treatments, or interpretation..." 
              class="flex-1 bg-void-black border border-line-subtle rounded h-[40px] px-3 text-[12px] text-ice-white focus:outline-none focus:border-knowledge-violet focus:shadow-[0_0_8px_rgba(139,92,246,0.3)] transition-all font-sans"
              :disabled="store.loading"
            >
            <button 
              type="submit"
              class="w-[40px] h-[40px] flex items-center justify-center rounded bg-knowledge-violet/20 border border-knowledge-violet text-knowledge-violet hover:bg-knowledge-violet/30 hover:shadow-[0_0_12px_rgba(139,92,246,0.4)] transition-all disabled:opacity-50"
              :disabled="!question.trim() || store.loading"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
            </button>
          </form>
        </div>
      </div>
      
      <!-- Right: Tracing (260px) -->
      <div class="w-[260px] shrink-0">
        <Panel title="Retrieval Tracing" color="violet">
          <div class="flex flex-col h-full gap-4">
            <div v-if="lastResponse" class="flex-1 overflow-auto pr-1">
              <h4 class="font-mono text-[9px] text-ghost-dim uppercase tracking-[0.1em] mb-2 border-b border-line-subtle pb-1">Sources Cited</h4>
              
              <div class="flex flex-col gap-2">
                <div v-for="cit in lastResponse.citations" :key="cit.id" class="bg-void-black border border-line-subtle rounded-sm p-2 hover:border-knowledge-violet/50 transition-colors">
                  <div class="flex items-center gap-1.5 mb-1.5">
                    <div class="bg-knowledge-violet text-void-black font-mono text-[8px] px-1 py-0.5 rounded-xs">{{ cit.id }}</div>
                    <span class="text-[10px] text-ice-white font-medium truncate">{{ cit.title }}</span>
                  </div>
                  <div class="text-[9px] text-steel-blue leading-relaxed line-clamp-3">
                    {{ cit.text }}
                  </div>
                  <div class="text-right mt-1">
                    <span class="font-mono text-[8px] text-knowledge-violet">Pg. {{ cit.page }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="flex-1 flex items-center justify-center text-ghost-dim">
              <span class="text-[10px] text-center px-4">Sources will appear here once an answer is generated.</span>
            </div>
            
            <div class="shrink-0 bg-void-black border border-line-subtle p-2 rounded-sm flex flex-col gap-1.5 mt-auto">
              <div class="flex justify-between items-center">
                <span class="font-mono text-[8px] text-ghost-dim">KB VERSION</span>
                <span class="font-mono text-[9px] text-knowledge-violet">{{ lastResponse?.version || 'v2.1.0' }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="font-mono text-[8px] text-ghost-dim">INDEX TIME</span>
                <span class="font-mono text-[9px] text-steel-blue">2026-04-18</span>
              </div>
            </div>
          </div>
        </Panel>
      </div>
      
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRagStore } from '../stores/rag';
import Panel from '../components/shared/Panel.vue';
import CitationTag from '../components/shared/CitationTag.vue';

const store = useRagStore();
const question = ref('');

const submitQuestion = async () => {
  if (!question.value.trim() || store.loading) return;
  const q = question.value;
  question.value = '';
  await store.askQuestion(q);
};

const lastResponse = computed(() => {
  for (let i = store.history.length - 1; i >= 0; i--) {
    if (store.history[i].response) return store.history[i].response;
  }
  return null;
});
</script>
