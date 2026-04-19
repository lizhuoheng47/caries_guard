<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex flex-col">
        <span class="font-mono text-[9px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">INTEL / <span class="text-[var(--cyan)]">DASHBOARD</span></span>
        <div class="flex items-center gap-3">
          <h2 class="text-[20px] font-medium text-[var(--tp)] m-0">AI 评估看板</h2>
          <StatusChip status="RUNNING" label="LIVE · 24/7" />
        </div>
      </div>
      
      <div class="flex items-center gap-4">
        <!-- Time Range -->
        <div class="flex gap-1 border border-[var(--ln)] rounded-[3px] p-0.5" style="background: rgba(3,8,18,0.5);">
          <button class="px-3 py-1 font-mono text-[9px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">24H</button>
          <button class="px-3 py-1 font-mono text-[9px] bg-[var(--cyan)] text-[var(--void)] rounded-[2px] shadow-[0_0_8px_var(--cyan)] glow-cyan">7D</button>
          <button class="px-3 py-1 font-mono text-[9px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">30D</button>
          <button class="px-3 py-1 font-mono text-[9px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">ALL</button>
        </div>
        
        <NeuralButton variant="ghost">EXPORT</NeuralButton>
        <NeuralButton variant="ghost">
          <template #icon-left><div class="w-1.5 h-1.5 rotate-45 bg-[var(--cyan)]"></div></template>
          CONFIGURE
        </NeuralButton>
      </div>
    </div>
    
    <!-- KPI Row -->
    <div class="grid grid-cols-5 gap-3 mb-4 shrink-0">
      <KpiCard label="TOTAL SCANS" value="2,847" trend="18.2" color="cyan" sparkline="0,12 10,8 20,14 30,5 42,10" />
      <KpiCard label="AGREEMENT" value="94.2%" trend="0.8" color="emerald" sparkline="0,10 10,12 20,8 30,6 42,4" />
      <KpiCard label="REVIEW RATE" value="12.4%" trend="-1.2" color="amber" sparkline="0,5 10,6 20,4 30,8 42,5" />
      <KpiCard label="RAG CALLS" value="1,203" trend="24.6" color="violet" sparkline="0,2 10,5 20,8 30,12 42,15" />
      <KpiCard label="AVG LATENCY" value="2.34s" trend="-0.28" color="magenta" sparkline="0,8 10,6 20,7 30,4 42,4" />
    </div>
    
    <!-- 2x3 Grid Panels -->
    <div class="flex-1 grid grid-cols-3 grid-rows-2 gap-3 min-h-0 mb-4 overflow-hidden">
      
      <!-- Panel 1: Uncertainty Distribution -->
      <Panel title="Uncertainty Distribution" color="amber">
        <template #meta><span class="font-mono text-[8px] text-[var(--td)]">N=2,847</span></template>
        <div class="w-full h-full relative flex items-end pb-4">
          <!-- Mock SVG Area Chart -->
          <svg class="w-full h-[80%] overflow-visible" viewBox="0 0 100 100" preserveAspectRatio="none">
            <!-- Grid lines -->
            <line x1="0" y1="50" x2="100" y2="50" stroke="var(--ln)" stroke-width="0.5" stroke-dasharray="2" />
            <line x1="35" y1="0" x2="35" y2="100" stroke="var(--amber)" stroke-width="0.5" stroke-dasharray="2" opacity="0.5" />
            
            <path d="M0,80 Q20,70 35,20 T70,70 T100,90" fill="none" stroke="var(--amber)" stroke-width="2" style="filter: drop-shadow(0 0 8px var(--amber));" />
            <path d="M0,80 Q20,70 35,20 T70,70 T100,90 L100,100 L0,100 Z" fill="url(#amber-grad)" opacity="0.3" />
            
            <defs>
              <linearGradient id="amber-grad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="var(--amber)" />
                <stop offset="100%" stop-color="transparent" />
              </linearGradient>
            </defs>
            
            <!-- Peak Point -->
            <circle cx="35" cy="20" r="2" fill="var(--amber)" style="filter: drop-shadow(0 0 4px var(--amber));" />
            <text x="35" y="10" fill="var(--amber)" font-family="monospace" font-size="6" text-anchor="middle">PEAK</text>
            <text x="35" y="0" fill="var(--amber)" font-family="monospace" font-size="6" text-anchor="middle">Θ 0.35</text>
          </svg>
          <!-- X Axis -->
          <div class="absolute bottom-0 left-0 right-0 flex justify-between px-2 text-[var(--td)] font-mono text-[7px]">
            <span>0.0</span><span>0.25</span><span>0.5</span><span>0.75</span><span>1.0</span>
          </div>
        </div>
      </Panel>

      <!-- Panel 2: Confusion Matrix -->
      <Panel title="Confusion Matrix" color="emerald">
        <template #meta><span class="font-mono text-[8px] text-[var(--td)] uppercase">G0-G3</span></template>
        <div class="flex flex-col h-full items-center justify-center pt-4">
          <div class="font-mono text-[8px] text-[var(--td)] tracking-widest mb-2">PREDICTED</div>
          <table class="border-collapse">
            <thead>
              <tr>
                <th></th>
                <th class="w-10 pb-2 font-mono text-[9px] text-[var(--ts)]">G0</th>
                <th class="w-10 pb-2 font-mono text-[9px] text-[var(--ts)]">G1</th>
                <th class="w-10 pb-2 font-mono text-[9px] text-[var(--ts)]">G2</th>
                <th class="w-10 pb-2 font-mono text-[9px] text-[var(--ts)]">G3</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="pr-2 font-mono text-[9px] text-[var(--ts)]">G0</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[12px]">412</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[12px]">18</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[12px]">3</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-transparent text-[var(--td)] font-mono text-[12px]">0</td>
              </tr>
              <tr>
                <td class="pr-2 font-mono text-[9px] text-[var(--ts)]">G1</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[12px]">22</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[12px]">385</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[12px]">14</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[12px]">2</td>
              </tr>
              <tr>
                <td class="pr-2 font-mono text-[9px] text-[var(--ts)]">G2</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[12px]">4</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[12px]">17</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[12px]">368</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/20 text-[var(--tp)] font-mono text-[12px]">20</td>
              </tr>
              <tr>
                <td class="pr-2 font-mono text-[9px] text-[var(--ts)]">G3</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-transparent text-[var(--td)] font-mono text-[12px]">1</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/5 text-[var(--tp)] font-mono text-[12px]">5</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)]/10 text-[var(--tp)] font-mono text-[12px]">15</td>
                <td class="w-10 h-10 text-center border-[0.5px] border-[var(--void)] bg-[var(--emerald)] text-[var(--void)] font-mono text-[12px]">298</td>
              </tr>
            </tbody>
          </table>
          <div class="flex justify-between w-[160px] mt-4 ml-[20px]">
            <span class="font-mono text-[9px] text-[var(--td)]">ACCURACY: <span class="text-[var(--emerald)]">94.2%</span></span>
            <span class="font-mono text-[9px] text-[var(--td)]">F1: <span class="text-[var(--emerald)]">0.936</span></span>
          </div>
        </div>
      </Panel>

      <!-- Panel 3: Model Capability -->
      <Panel title="Model Capability" color="violet">
        <template #meta><span class="font-mono text-[8px] text-[var(--td)] uppercase">v1.0 vs v0.9</span></template>
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
            <text x="50" y="5" fill="var(--ts)" font-family="monospace" font-size="5" text-anchor="middle">DETECT</text>
            <text x="95" y="40" fill="var(--ts)" font-family="monospace" font-size="5" text-anchor="start">GRADE</text>
            <text x="80" y="90" fill="var(--ts)" font-family="monospace" font-size="5" text-anchor="middle">SPEED</text>
            <text x="20" y="90" fill="var(--ts)" font-family="monospace" font-size="5" text-anchor="middle">SEGM</text>
            <text x="5" y="40" fill="var(--ts)" font-family="monospace" font-size="5" text-anchor="end">UNC</text>
          </svg>
          
          <div class="absolute bottom-0 flex justify-center w-full gap-4">
            <div class="flex items-center gap-1.5"><div class="w-3 h-0.5 bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div><span class="font-mono text-[8px] text-[var(--tp)]">v1.0</span></div>
            <div class="flex items-center gap-1.5"><div class="w-3 h-0.5 border-t border-dashed border-[var(--violet)]"></div><span class="font-mono text-[8px] text-[var(--td)]">v0.9</span></div>
          </div>
        </div>
      </Panel>

      <!-- Panel 4: Grading Distribution -->
      <Panel title="Grading Distribution" color="cyan">
        <template #meta><span class="font-mono text-[8px] text-[var(--td)] uppercase">7D</span></template>
        <div class="flex flex-col justify-around h-full py-2">
          <!-- G0 -->
          <div class="flex items-center gap-3">
            <div class="w-[80px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--emerald)] shadow-[0_0_4px_var(--emerald)]"></div><span class="font-mono text-[10px] text-[var(--td)]">G0 <span class="text-[var(--emerald)] ml-1 opacity-80">NORMAL</span></span></div>
            </div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">433</div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">15.2%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]" style="width: 15.2%"></div>
            </div>
          </div>
          <!-- G1 -->
          <div class="flex items-center gap-3">
            <div class="w-[80px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div><span class="font-mono text-[10px] text-[var(--td)]">G1 <span class="text-[var(--cyan)] ml-1 opacity-80">MILD</span></span></div>
            </div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">423</div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">14.9%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]" style="width: 14.9%"></div>
            </div>
          </div>
          <!-- G2 -->
          <div class="flex items-center gap-3">
            <div class="w-[80px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]"></div><span class="font-mono text-[10px] text-[var(--td)]">G2 <span class="text-[var(--amber)] ml-1 opacity-80" style="font-size: 8px;">MODERATE</span></span></div>
            </div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">1,253</div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">44.0%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--amber)] shadow-[0_0_8px_var(--amber)]" style="width: 44%"></div>
            </div>
          </div>
          <!-- G3 -->
          <div class="flex items-center gap-3">
            <div class="w-[80px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--magenta)] shadow-[0_0_4px_var(--magenta)]"></div><span class="font-mono text-[10px] text-[var(--td)]">G3 <span class="text-[var(--magenta)] ml-1 opacity-80">DEEP</span></span></div>
            </div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">628</div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">22.1%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--magenta)] shadow-[0_0_8px_var(--magenta)]" style="width: 22.1%"></div>
            </div>
          </div>
          <!-- G4 -->
          <div class="flex items-center gap-3">
            <div class="w-[80px] flex items-center justify-between">
              <div class="flex items-center gap-1.5"><div class="w-1.5 h-1.5 rounded-full bg-[var(--violet)] shadow-[0_0_4px_var(--violet)]"></div><span class="font-mono text-[10px] text-[var(--td)]">G4 <span class="text-[var(--violet)] ml-1 opacity-80 text-[8px]">CRITICAL</span></span></div>
            </div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">110</div>
            <div class="font-mono text-[10px] text-[var(--ts)] w-8 text-right">3.8%</div>
            <div class="flex-1 h-[2px] bg-[var(--ln)] rounded-full overflow-hidden flex items-center">
              <div class="h-full bg-[var(--violet)] shadow-[0_0_8px_var(--violet)]" style="width: 3.8%"></div>
            </div>
          </div>
        </div>
      </Panel>

      <!-- Panel 5: Weekly Activity Heatmap -->
      <Panel title="Weekly Activity Heatmap" color="cyan">
        <template #meta><span class="font-mono text-[8px] text-[var(--td)] uppercase">7D x 12H</span></template>
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
            <span class="font-mono text-[8px] text-[var(--td)] leading-none">MON<br/>08:00</span>
            <div class="flex items-center gap-1">
              <span class="font-mono text-[8px] text-[var(--td)] mr-1">LESS</span>
              <div class="w-2.5 h-2.5 bg-[var(--cyan)]/10"></div>
              <div class="w-2.5 h-2.5 bg-[var(--cyan)]/40"></div>
              <div class="w-2.5 h-2.5 bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div>
              <span class="font-mono text-[8px] text-[var(--td)] ml-1">MORE</span>
            </div>
            <span class="font-mono text-[8px] text-[var(--td)] leading-none text-right">SUN<br/>20:00</span>
          </div>
        </div>
      </Panel>

      <!-- Panel 6: System Events -->
      <Panel title="System Events" color="amber">
        <template #meta>
          <div class="flex items-center gap-1.5">
            <div class="w-1 h-1 rounded-full bg-[var(--td)] animate-pulse-opacity"></div>
            <span class="font-mono text-[8px] text-[var(--td)] uppercase">LIVE</span>
          </div>
        </template>
        <div class="flex flex-col gap-2 h-full overflow-y-auto pr-2">
          <!-- Log 1 -->
          <div class="flex items-center gap-3">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:28:42</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--emerald)]/20 text-[var(--emerald)] border border-[var(--emerald)]/30 w-7 text-center">OK</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">2.3s</span>
          </div>
          <!-- Log 2 -->
          <div class="flex items-center gap-3">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:28:15</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--amber)]/20 text-[var(--amber)] border border-[var(--amber)]/30 w-7 text-center">WRN</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">0.72</span>
          </div>
          <!-- Log 3 -->
          <div class="flex items-center gap-3">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:27:58</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--cyan)]/20 text-[var(--cyan)] border border-[var(--cyan)]/30 w-7 text-center">INF</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">180ms</span>
          </div>
          <!-- Log 4 -->
          <div class="flex items-center gap-3">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:27:31</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--emerald)]/20 text-[var(--emerald)] border border-[var(--emerald)]/30 w-7 text-center">OK</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">2.1s</span>
          </div>
          <!-- Log 5 -->
          <div class="flex items-center gap-3 opacity-80">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:27:04</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--cyan)]/20 text-[var(--cyan)] border border-[var(--cyan)]/30 w-7 text-center">INF</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">—</span>
          </div>
          <!-- Log 6 -->
          <div class="flex items-center gap-3 opacity-60">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:26:47</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--magenta)]/20 text-[var(--magenta)] border border-[var(--magenta)]/30 w-7 text-center glow-magenta">ERR</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">#3</span>
          </div>
          <!-- Log 7 -->
          <div class="flex items-center gap-3 opacity-40">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:26:22</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--emerald)]/20 text-[var(--emerald)] border border-[var(--emerald)]/30 w-7 text-center">OK</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">—</span>
          </div>
          <!-- Log 8 -->
          <div class="flex items-center gap-3 opacity-20">
            <span class="font-mono text-[9px] text-[var(--cyan-soft)] w-14">14:26:01</span>
            <div class="font-mono text-[8px] px-1.5 py-0.5 rounded-xs bg-[var(--cyan)]/20 text-[var(--cyan)] border border-[var(--cyan)]/30 w-7 text-center">INF</div>
            <div class="flex-1 border-b border-[var(--ln)]/30 border-dashed h-px relative top-1 mx-2"></div>
            <span class="font-mono text-[9px] text-[var(--ts)]">v1.0</span>
          </div>
        </div>
      </Panel>
      
    </div>
  </div>
</template>

<script setup lang="ts">
import Panel from '../components/shared/Panel.vue';
import KpiCard from '../components/shared/KpiCard.vue';
import NeuralButton from '../components/shared/NeuralButton.vue';
import StatusChip from '../components/shared/StatusChip.vue';
</script>
