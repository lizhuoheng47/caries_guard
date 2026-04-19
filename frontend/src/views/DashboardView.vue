<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex flex-col">
        <span class="font-mono text-[11px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">智能情报 / <span class="text-[var(--cyan)]">评估看板</span></span>
        <div class="flex items-center gap-3">
          <h2 class="text-[22px] font-medium text-[var(--tp)] m-0">AI 评估看板</h2>
          <StatusChip status="RUNNING" label="实时运行 · 24/7" />
        </div>
      </div>
      
      <div class="flex items-center gap-4">
        <!-- Time Range -->
        <div class="flex gap-1 border border-[var(--ln)] rounded-[3px] p-0.5" style="background: rgba(3,8,18,0.5);">
          <button class="px-3 py-1 font-mono text-[11px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">24小时</button>
          <button class="px-3 py-1 font-mono text-[11px] bg-[var(--cyan)] text-[var(--void)] rounded-[2px] shadow-[0_0_8px_var(--cyan)] glow-cyan">7天</button>
          <button class="px-3 py-1 font-mono text-[11px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">30天</button>
          <button class="px-3 py-1 font-mono text-[11px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">全部</button>
        </div>
        
        <NeuralButton variant="ghost">导出</NeuralButton>
        <NeuralButton variant="ghost">
          <template #icon-left><div class="w-1.5 h-1.5 rotate-45 bg-[var(--cyan)]"></div></template>
          配置
        </NeuralButton>
      </div>
    </div>
    
    <!-- KPI Row -->
    <div class="grid grid-cols-5 gap-3 mb-4 shrink-0" v-if="dashboardData">
      <KpiCard label="总扫描数" :value="dashboardData.kpi.totalScans.toLocaleString()" :trend="18.2" color="cyan" sparkline="0,12 10,8 20,14 30,5 42,10" />
      <KpiCard label="一致性" :value="(dashboardData.kpi.agreementRate * 100).toFixed(1) + '%'" :trend="0.8" color="emerald" sparkline="0,10 10,12 20,8 30,6 42,4" />
      <KpiCard label="复核率" :value="(dashboardData.kpi.reviewRate * 100).toFixed(1) + '%'" :trend="-1.2" color="amber" sparkline="0,5 10,6 20,4 30,8 42,5" />
      <KpiCard label="RAG 调用" :value="dashboardData.kpi.ragCalls.toLocaleString()" :trend="24.6" color="violet" sparkline="0,2 10,5 20,8 30,12 42,15" />
      <KpiCard label="平均延迟" :value="(dashboardData.kpi.avgLatencyMs / 1000).toFixed(2) + '秒'" :trend="-0.28" color="magenta" sparkline="0,8 10,6 20,7 30,4 42,4" />
    </div>
    <div class="grid grid-cols-5 gap-3 mb-4 shrink-0 opacity-50" v-else>
      <KpiCard label="总扫描数" value="—" color="cyan" />
      <KpiCard label="一致性" value="—" color="emerald" />
      <KpiCard label="复核率" value="—" color="amber" />
      <KpiCard label="RAG 调用" value="—" color="violet" />
      <KpiCard label="平均延迟" value="—" color="magenta" />
    </div>

    
    <!-- 2x3 Grid Panels -->
    <div class="flex-1 grid grid-cols-3 grid-rows-2 gap-3 min-h-0 mb-4 overflow-hidden">
      
      <!-- Panel 1: Uncertainty Distribution -->
      <Panel title="不确定度分布" color="amber">
        <template #meta><span class="font-mono text-[10px] text-[var(--td)]">N=2,847</span></template>
        <div class="w-full h-full relative flex items-end pt-2 pb-4">
          <!-- Mock SVG Area Chart -->
          <svg class="w-full h-[88%] overflow-visible" viewBox="0 0 100 60" preserveAspectRatio="xMidYMid meet">
            <!-- Background horizontal line -->
            <line x1="0" y1="50" x2="100" y2="50" stroke="var(--ln)" stroke-width="0.5" stroke-dasharray="4" opacity="0.3" />
            
            <!-- Area under the curve -->
            <path d="M15,50 C25,50 30,20 35,20 C40,20 45,50 60,50 L60,60 L15,60 Z" fill="url(#amber-glow-grad)" opacity="0.4" />
            
            <!-- Peak Indicator Line -->
            <line x1="35" y1="20" x2="35" y2="50" stroke="var(--amber)" stroke-width="0.8" stroke-dasharray="2" opacity="0.6" />
            
            <!-- Main Curve -->
            <path d="M15,50 C25,50 30,20 35,20 C40,20 45,50 60,50" fill="none" stroke="var(--amber)" stroke-width="1" stroke-linecap="round" style="filter: drop-shadow(0 0 8px var(--amber));" />
            
            <defs>
              <linearGradient id="amber-glow-grad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="var(--amber)" />
                <stop offset="70%" stop-color="var(--amber)" stop-opacity="0.1" />
                <stop offset="100%" stop-color="transparent" />
              </linearGradient>
            </defs>
            
            <!-- Peak Point Marker -->
            <circle cx="35" cy="20" r="1" fill="var(--amber)" style="filter: drop-shadow(0 0 4px var(--amber));" />
            
            <!-- Labels -->
            <text x="35" y="14" fill="var(--amber)" font-family="monospace" font-size="4" text-anchor="middle" font-weight="bold">峰值</text>
            <text x="35" y="8" fill="var(--amber)" font-family="monospace" font-size="5" text-anchor="middle" font-weight="bold">Θ 0.35</text>
          </svg>



          <!-- X Axis -->
          <div class="absolute bottom-0 left-0 right-0 flex justify-between px-2 text-[var(--td)] font-mono text-[10px]">
            <span>0.0</span><span>0.25</span><span>0.5</span><span>0.75</span><span>1.0</span>
          </div>

        </div>
      </Panel>

      <!-- Panel 2: Confusion Matrix -->
      <Panel title="混淆矩阵" color="emerald">
        <template #meta><span class="font-mono text-[10px] text-[var(--td)] uppercase">G0-G3</span></template>
        <div class="flex flex-col h-full items-center justify-center pt-4">
          <div class="font-mono text-[10px] text-[var(--td)] tracking-widest mb-2">预测值</div>
          <table class="border-collapse">
            <thead>
              <tr>
                <th></th>
                <th class="w-10 pb-2 font-mono text-[11px] text-[var(--ts)]">G0</th>
                <th class="w-10 pb-2 font-mono text-[11px] text-[var(--ts)]">G1</th>
                <th class="w-10 pb-2 font-mono text-[11px] text-[var(--ts)]">G2</th>
                <th class="w-10 pb-2 font-mono text-[11px] text-[var(--ts)]">G3</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="pr-2 font-mono text-[11px] text-[var(--ts)]">G0</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[13px]">412</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[13px]">18</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[13px]">3</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-transparent text-[var(--td)] font-mono text-[13px]">0</td>
              </tr>
              <tr>
                <td class="pr-2 font-mono text-[11px] text-[var(--ts)]">G1</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[13px]">22</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[13px]">385</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[13px]">14</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[13px]">2</td>
              </tr>
              <tr>
                <td class="pr-2 font-mono text-[11px] text-[var(--ts)]">G2</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[13px]">4</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[13px]">17</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[13px]">368</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/20 text-[var(--tp)] font-mono text-[13px]">20</td>
              </tr>
              <tr>
                <td class="pr-2 font-mono text-[11px] text-[var(--ts)]">G3</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-transparent text-[var(--td)] font-mono text-[13px]">1</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[13px]">5</td>
                <td class="w-10 h-10 text-center border-[0.5px] border(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[13px]">15</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[13px]">298</td>
              </tr>
            </tbody>
          </table>
          <div class="flex justify-between w-[180px] mt-4 ml-[20px]">
            <span class="font-mono text-[11px] text-[var(--td)]">准确率: <span class="text-[var(--emerald)]">94.2%</span></span>
            <span class="font-mono text-[11px] text-[var(--td)]">F1 值: <span class="text-[var(--emerald)]">0.936</span></span>
          </div>
        </div>
      </Panel>

      <!-- Panel 3: Model Capability -->
      <Panel title="模型能力评估" color="violet">
        <template #meta><span class="font-mono text-[10px] text-[var(--td)] uppercase">v1.0 对比 v0.9</span></template>
        <div class="relative w-full h-full flex flex-col items-center justify-center">
          <svg viewBox="0 0 100 100" class="w-[80%] h-[80%] overflow-visible">
            <!-- Radar background grid (pentagon) -->
            <polygon points="50,10 88,38 73,82 27,82 12,38" fill="none" stroke="var(--ln)" stroke-width="0.5" />
            <polygon points="50,30 69,44 62,66 38,66 31,44" fill="none" stroke="var(--ln)" stroke-width="0.5" />
            <!-- Axis lines -->
            <line x1="50" y1="50" x2="50" y2="10" stroke="var(--ln)" stroke-width="0.5" />
            <line x1="50" y1="50" x2="88" y2="38" stroke="var(--ln)" stroke-width="0.5" />
            <line x1="50" y1="50" x2="73" y2="82" stroke="var(--ln)" stroke-width="0.5" />
            <line x1="50" y1="50" x2="27" y2="82" stroke="var(--ln)" stroke-width="0.5" />
            <line x1="50" y1="50" x2="12" y2="38" stroke="var(--ln)" stroke-width="0.5" />
            <!-- v0.9 data (violet dashed) -->
            <polygon points="50,20 75,40 60,70 35,65 20,40" fill="none" stroke="var(--violet)" stroke-width="1" stroke-dasharray="2" opacity="0.6" />
            <polygon points="50,20 75,40 60,70 35,65 20,40" fill="var(--violet)" opacity="0.1" />
            <!-- v1.0 data (cyan solid) -->
            <polygon points="50,12 85,35 70,75 25,78 15,35" fill="none" stroke="var(--cyan)" stroke-width="1.5" style="filter: drop-shadow(0 0 4px var(--cyan));" />
            <polygon points="50,12 85,35 70,75 25,78 15,35" fill="var(--cyan)" opacity="0.2" />
            
            <!-- Axis Labels -->
            <text x="50" y="5" fill="var(--ts)" font-family="monospace" font-size="9" text-anchor="middle">检测</text>
            <text x="95" y="40" fill="var(--ts)" font-family="monospace" font-size="9" text-anchor="start">分级</text>
            <text x="80" y="90" fill="var(--ts)" font-family="monospace" font-size="9" text-anchor="middle">速度</text>
            <text x="20" y="90" fill="var(--ts)" font-family="monospace" font-size="9" text-anchor="middle">分割</text>
            <text x="5" y="40" fill="var(--ts)" font-family="monospace" font-size="9" text-anchor="end">置信度</text>
          </svg>

          
          <div class="absolute bottom-0 flex justify-center w-full gap-4">
            <div class="flex items-center gap-1.5"><div class="w-3 h-0.5 bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div><span class="font-mono text-[10px] text-[var(--tp)]">v1.0</span></div>
            <div class="flex items-center gap-1.5"><div class="w-3 h-0.5 border-t border-dashed border-[var(--violet)]"></div><span class="font-mono text-[10px] text-[var(--td)]">v0.9</span></div>
          </div>
        </div>
      </Panel>


      <!-- Panel 4: Grading Distribution -->
      <Panel title="病变等级分布" color="cyan">
        <template #meta><span class="font-mono text-[10px] text-[var(--td)] uppercase">最近7天</span></template>
        <div class="flex flex-col justify-around h-full py-2">
          <!-- G0 -->
          <div class="flex items-center gap-3">
            <div class="w-[100px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--emerald)] shadow-[0_0_4px_var(--emerald)]"></div><span class="font-mono text-[11px] text-[var(--td)]">G0 <span class="text-[var(--emerald)] ml-1 opacity-80">正常</span></span></div>
            </div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradeCount('G0') }}</div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradePct('G0') }}%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]" :style="{ width: getGradePct('G0') + '%' }"></div>
            </div>
          </div>
          <!-- G1 -->
          <div class="flex items-center gap-3">
            <div class="w-[100px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div><span class="font-mono text-[11px] text-[var(--td)]">G1 <span class="text-[var(--cyan)] ml-1 opacity-80">轻微</span></span></div>
            </div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradeCount('G1') }}</div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradePct('G1') }}%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]" :style="{ width: getGradePct('G1') + '%' }"></div>
            </div>
          </div>
          <!-- G2 -->
          <div class="flex items-center gap-3">
            <div class="w-[100px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]"></div><span class="font-mono text-[11px] text-[var(--td)]">G2 <span class="text-[var(--amber)] ml-1 opacity-80">中度</span></span></div>
            </div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradeCount('G2') }}</div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradePct('G2') }}%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--amber)] shadow-[0_0_8px_var(--amber)]" :style="{ width: getGradePct('G2') + '%' }"></div>
            </div>
          </div>
          <!-- G3 -->
          <div class="flex items-center gap-3">
            <div class="w-[100px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--magenta)] shadow-[0_0_4px_var(--magenta)]"></div><span class="font-mono text-[11px] text-[var(--td)]">G3 <span class="text-[var(--magenta)] ml-1 opacity-80">深度</span></span></div>
            </div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradeCount('G3') }}</div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradePct('G3') }}%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--magenta)] shadow-[0_0_8px_var(--magenta)]" :style="{ width: getGradePct('G3') + '%' }"></div>
            </div>
          </div>
          <!-- G4 -->
          <div class="flex items-center gap-3">
            <div class="w-[100px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--violet)] shadow-[0_0_4px_var(--violet)]"></div><span class="font-mono text-[11px] text-[var(--td)]">G4 <span class="text-[var(--violet)] ml-1 opacity-80">危重</span></span></div>
            </div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradeCount('G4') }}</div>
            <div class="font-mono text-[11px] text-[var(--ts)] w-8 text-right">{{ getGradePct('G4') }}%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--violet)] shadow-[0_0_8px_var(--violet)]" :style="{ width: getGradePct('G4') + '%' }"></div>
            </div>
          </div>
        </div>
      </Panel>

      <!-- Panel 5: Weekly Activity Heatmap -->
      <Panel title="周活跃度热力图" color="cyan">
        <template #meta><span class="font-mono text-[10px] text-[var(--td)] uppercase">7天 x 24小时</span></template>
        <div class="flex flex-col h-full justify-between pb-2 pt-2">
          <div class="flex justify-between flex-1 mb-2">
            <!-- 7 columns (days), 12 rows (hours) simulated -->
            <div v-for="d in 12" :key="d" class="flex flex-col justify-between w-[8%]">
              <div v-for="h in 7" :key="h" 
                   class="w-full aspect-square rounded-xs border border-[rgba(0,229,255,0.05)]"
                   :style="{ 
                     backgroundColor: `rgba(0,229,255, ${Math.max(0.05, Math.random() * (Math.sin(d/3)*Math.cos(h/2) > 0 ? 1 : 0.2))})`,
                     boxShadow: Math.sin(d/3)*Math.cos(h/2) > 0.8 ? '0 0 8px rgba(0,229,255,0.5)' : 'none'
                   }"
              ></div>
            </div>
          </div>
          <div class="flex justify-between items-center px-1">
            <span class="font-mono text-[9px] text-[var(--td)] leading-none">周一<br/>08:00</span>
            <div class="flex items-center gap-1">
              <span class="font-mono text-[9px] text-[var(--td)] mr-1">低</span>
              <div class="w-2.5 h-2.5 bg-[var(--cyan)]/10"></div>
              <div class="w-2.5 h-2.5 bg-[var(--cyan)]/40"></div>
              <div class="w-2.5 h-2.5 bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div>
              <span class="font-mono text-[9px] text-[var(--td)] ml-1">高</span>
            </div>
            <span class="font-mono text-[9px] text-[var(--td)] leading-none text-right">周日<br/>20:00</span>
          </div>
        </div>
      </Panel>

      <!-- Panel 6: System Events -->
      <Panel title="系统事件日志" color="amber">
        <template #meta>
          <div class="flex items-center gap-1.5">
            <div class="w-1 h-1 rounded-full bg-[var(--td)] animate-pulse-opacity"></div>
            <span class="font-mono text-[10px] text-[var(--td)] uppercase">实时</span>
          </div>
        </template>
        <div class="flex flex-col gap-2 h-full overflow-y-auto pr-2">
          <template v-if="dashboardData">
            <div v-for="event in dashboardData.events" :key="event.eventId" class="flex items-center gap-3">
              <span class="font-mono text-[11px] text-[var(--cyan-soft)] w-14">{{ new Date(event.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) }}</span>
              <div class="font-mono text-[10px] px-1.5 py-0.5 rounded-xs border w-10 text-center glow-magenta" 
                   :class="event.type === 'ERROR' ? 'bg-[var(--magenta)]/20 text-[var(--magenta)] border-[var(--magenta)]/30' : event.type === 'WARNING' ? 'bg-[var(--amber)]/20 text-[var(--amber)] border-[var(--amber)]/30' : event.type === 'INFO' ? 'bg-[var(--cyan)]/20 text-[var(--cyan)] border-[var(--cyan)]/30' : 'bg-[var(--emerald)]/20 text-[var(--emerald)] border-[var(--emerald)]/30'">
                {{ event.type === 'SUCCESS' ? '完成' : event.type === 'WARNING' ? '警告' : event.type === 'ERROR' ? '错误' : '信息' }}
              </div>
              <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
              <span class="font-mono text-[11px] text-[var(--ts)]">{{ event.message || '—' }}</span>
            </div>
          </template>
        </div>
      </Panel>

      
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import Panel from '../components/shared/Panel.vue';
import KpiCard from '../components/shared/KpiCard.vue';
import NeuralButton from '../components/shared/NeuralButton.vue';
import StatusChip from '../components/shared/StatusChip.vue';
import { dashboardApi } from '../api/dashboard';

const dashboardData = ref<any>(null);

const fetchDashboard = async () => {
  try {
    const res = await dashboardApi.getNeuralDashboard();
    dashboardData.value = res.data;
  } catch (error) {
    console.error('Failed to fetch dashboard', error);
  }
};

const getGradeCount = (grade: string) => {
  if (!dashboardData.value || !dashboardData.value.gradingDistribution) return 0;
  const item = dashboardData.value.gradingDistribution.find((x: any) => x.grade === grade);
  return item ? item.count : 0;
};

const getGradePct = (grade: string) => {
  if (!dashboardData.value || !dashboardData.value.gradingDistribution) return 0;
  const total = dashboardData.value.gradingDistribution.reduce((sum: number, x: any) => sum + x.count, 0);
  if (total === 0) return 0;
  const item = dashboardData.value.gradingDistribution.find((x: any) => x.grade === grade);
  return item ? ((item.count / total) * 100).toFixed(1) : 0;
};

onMounted(() => {
  fetchDashboard();
});
</script>
