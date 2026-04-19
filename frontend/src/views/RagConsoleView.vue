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
              
              <!-- AI Msg 1 -->
              <div class="flex gap-3 max-w-[85%]">
                <div class="w-[28px] h-[28px] rounded-[4px] bg-[var(--violet)]/20 border border-[var(--violet)] flex items-center justify-center shrink-0">
                  <div class="w-3 h-3 bg-gradient-to-br from-[var(--cyan)] to-[var(--violet)] shadow-[0_0_8px_var(--violet)] rounded-xs rotate-45"></div>
                </div>
                <div class="bg-[var(--violet)]/5 border-l-2 border-l-[var(--violet)] border border-[var(--violet)]/20 rounded-[4px] p-3 text-[12px] text-[var(--tp)] leading-relaxed">
                  系统已完成全景X光片的初步推理。在右下颌第一磨牙远中邻面检测到 <strong>G3 级</strong> 龋坏。该病变表现为典型的透射影像，深度已突破釉质牙本质界（DEJ），位于牙本质中层<CitationTag id="1" />。<br/><br/>
                  由于该区域边缘存在伪影（Uncertainty 0.72），系统已自动将此病例标记为需医生复核。
                </div>
              </div>
              
              <!-- User Msg -->
              <div class="flex gap-3 max-w-[85%] self-end flex-row-reverse">
                <div class="w-[28px] h-[28px] rounded-[4px] bg-[rgba(3,8,18,0.8)] border border-[var(--ln)] flex items-center justify-center shrink-0">
                  <svg class="w-4 h-4 text-[var(--ts)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                </div>
                <div class="bg-[var(--cyan)]/10 border border-[var(--cyan)]/30 rounded-[4px] p-3 text-[12px] text-[var(--tp)] leading-relaxed">
                  根据临床指南，这种深度的病变现在应该采取什么保守治疗措施？
                </div>
              </div>
              
              <!-- AI Msg 2 -->
              <div class="flex gap-3 max-w-[85%]">
                <div class="w-[28px] h-[28px] rounded-[4px] bg-[var(--violet)]/20 border border-[var(--violet)] flex items-center justify-center shrink-0">
                  <div class="w-3 h-3 bg-gradient-to-br from-[var(--cyan)] to-[var(--violet)] shadow-[0_0_8px_var(--violet)] rounded-xs rotate-45"></div>
                </div>
                <div class="flex flex-col gap-2">
                  <div class="bg-[var(--violet)]/5 border-l-2 border-l-[var(--violet)] border border-[var(--violet)]/20 rounded-[4px] p-3 text-[12px] text-[var(--tp)] leading-relaxed">
                    对于 G3 级（累及牙本质中层）但未波及牙髓的病变，最新版保守牙医学指南建议：<br/><br/>
                    1. <strong>选择性去龋（Selective Caries Removal）</strong>：保留靠近牙髓的软化但可再矿化的感染牙本质，以避免露髓风险<CitationTag id="2" />。<br/>
                    2. 使用具有良好密封性的生物活性材料（如树脂改性玻璃离子或含钙硅酸盐材料）进行垫底封闭<CitationTag id="3" />。<br/>
                    3. 上方使用复合树脂进行最终充填重建接触关系。
                  </div>
                  <!-- Safety Flag -->
                  <div class="bg-[var(--amber)]/10 border border-[var(--amber)]/30 rounded-[4px] p-2 flex items-center gap-2">
                    <svg class="w-4 h-4 text-[var(--amber)]" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                    <span class="text-[10px] text-[var(--amber)] font-mono">WARNING: AI 建议仅供参考，具体方案请结合患者临床症状和活力测试。</span>
                  </div>
                </div>
              </div>
              
            </div>
            
            <!-- Input Area -->
            <div class="shrink-0 pt-3 border-t border-[var(--ln)]">
              <div class="relative">
                <textarea 
                  class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[4px] py-3 pl-3 pr-12 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-colors resize-none h-[48px]"
                  placeholder="Ask for deeper diagnostic details..."
                ></textarea>
                <button class="absolute right-2 top-1/2 -translate-y-1/2 w-[32px] h-[32px] flex items-center justify-center bg-[var(--cyan)]/20 border border-[var(--cyan)] text-[var(--cyan)] rounded-[3px] hover:shadow-[0_0_8px_rgba(0,229,255,0.4)] transition-all">
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
              <div v-for="i in 3" :key="i" class="p-3 bg-[rgba(3,8,18,0.5)] border border-[var(--ln)] hover:border-[var(--violet)]/50 rounded-[4px] cursor-pointer transition-colors relative">
                <div class="absolute -left-[1px] top-2 w-[2px] h-[16px] bg-[var(--violet)] shadow-[0_0_8px_var(--violet)]"></div>
                <div class="flex justify-between items-start mb-1.5 pl-1.5">
                  <div class="flex items-center gap-1.5">
                    <div class="w-4 h-4 bg-[var(--violet)]/20 border border-[var(--violet)]/40 rounded-xs flex items-center justify-center font-mono text-[8px] text-[var(--violet)]">{{ i }}</div>
                    <span class="font-mono text-[9px] text-[var(--tp)] truncate w-[140px]">{{ i === 1 ? '龋病诊断专家共识' : '现代牙体牙髓病学' }}</span>
                  </div>
                  <span class="font-mono text-[8px] text-[var(--td)]">P.{{ 40+i*12 }}</span>
                </div>
                <p class="text-[10px] text-[var(--ts)] leading-relaxed line-clamp-3 pl-1.5">
                  {{ i === 1 ? '当影像学显示透射区位于牙本质外1/2或中层，且临床表现无不可逆性牙髓炎症状时，应避免彻底去除靠近髓腔的感染牙本质...' : '树脂改性玻璃离子具有良好的边缘封闭性和一定程度的氟释放能力，是垫底的理想材料...' }}
                </p>
                <div class="flex justify-end mt-2">
                  <span class="font-mono text-[8px] text-[var(--emerald)] val-emerald">MATCH 9{{ 8-i }}%</span>
                </div>
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
import Panel from '../components/shared/Panel.vue';
import CitationTag from '../components/shared/CitationTag.vue';
</script>
